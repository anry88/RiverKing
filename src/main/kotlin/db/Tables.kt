package db

import app.Env
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import util.CoinCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.jetbrains.exposed.sql.SortOrder

object DB {
    fun init(env: Env) {
        // SQLite single-file DB. Path from env.DATABASE_URL, e.g. jdbc:sqlite:/data/riverking.db
        Database.connect(url = env.dbUrl, driver = "org.sqlite.JDBC")
        transaction {
            ensureRodCodeColumn()
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Locations,
                Fish,
                Lures,
                Rods,
                InventoryLures,
                InventoryRods,
                Catches,
                PendingCatches,
                LocationFishWeights,
                Payments,
                PaySupportRequests,
                Tournaments,
                UserPrizes,
                ReferralLinks,
                ReferralRewards,
                ShopDiscounts,
            )
            seedIfEmpty()
            migrateCoins()
        }
    }

    private fun ensureRodCodeColumn() {
        val tx = TransactionManager.current()
        val tableName = Rods.tableName
        val rodsExists = tx.exec(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'"
        ) { rs -> rs.next() } ?: false
        if (!rodsExists) return
        val hasCode = tx.exec("PRAGMA table_info($tableName)") { rs ->
            var found = false
            while (rs.next()) {
                if (rs.getString("name").equals("code", ignoreCase = true)) {
                    found = true
                    break
                }
            }
            found
        } ?: false
        if (hasCode) return

        // Add the column without NOT NULL constraint to avoid SQLite migration failures.
        tx.exec("ALTER TABLE $tableName ADD COLUMN code VARCHAR(50)")

        val predefined = mapOf(
            "Искра" to "spark",
            "Роса" to "dew",
            "Поток" to "stream",
            "Глубь" to "abyss",
            "Шторм" to "storm",
        )

        predefined.forEach { (name, code) ->
            Rods.update({ Rods.name eq name }) { it[Rods.code] = code }
        }

        Rods.slice(Rods.id).select { Rods.code.isNull() }.forEach { row ->
            val id = row[Rods.id].value
            Rods.update({ Rods.id eq id }) { it[Rods.code] = "rod_$id" }
        }

        tx.exec("CREATE UNIQUE INDEX IF NOT EXISTS ${tableName}_code_unique ON $tableName(code)")
    }

    private fun seedIfEmpty() {
        fun upsertLocation(name: String, unlock: Double, mult: Double): Long {
            val row = Locations.select { Locations.name eq name }.singleOrNull()
            return if (row == null) {
                Locations.insertAndGetId {
                    it[Locations.name] = name
                    it[unlockKg] = unlock
                    it[sizeMultiplier] = mult
                }.value
            } else {
                val id = row[Locations.id].value
                Locations.update({ Locations.id eq id }) {
                    it[unlockKg] = unlock
                    it[sizeMultiplier] = mult
                }
                id
            }
        }

        fun upsertFish(n: String, r: String, mean: Double, vari: Double, pred: Boolean, water: String): Long {
            val row = Fish.select { Fish.name eq n }.singleOrNull()
            return if (row == null) {
                Fish.insertAndGetId {
                    it[name] = n
                    it[rarity] = r
                    it[meanKg] = mean
                    it[varKg] = vari
                    it[Fish.predator] = pred
                    it[Fish.water] = water
                }.value
            } else {
                val id = row[Fish.id].value
                Fish.update({ Fish.id eq id }) {
                    it[rarity] = r
                    it[meanKg] = mean
                    it[varKg] = vari
                    it[Fish.predator] = pred
                    it[Fish.water] = water
                }
                id
            }
        }

        fun setLFWeight(loc: Long, fish: Long, w: Double) {
            val existing = LocationFishWeights
                .select { (LocationFishWeights.locationId eq loc) and (LocationFishWeights.fishId eq fish) }
                .singleOrNull()
            if (existing == null) {
                LocationFishWeights.insert {
                    it[locationId] = loc; it[fishId] = fish; it[weight] = w
                }
            } else {
                LocationFishWeights.update({
                    (LocationFishWeights.locationId eq loc) and (LocationFishWeights.fishId eq fish)
                }) { it[weight] = w }
            }
        }

        fun upsertLure(name: String, predator: Boolean, water: String, rarityBonus: Double = 0.0): Long {
            val row = Lures.select { Lures.name eq name }.singleOrNull()
            return if (row == null) {
                Lures.insertAndGetId {
                    it[Lures.name] = name
                    it[priceStars] = null
                    it[modsJson] = "{}"
                    it[Lures.predator] = predator
                    it[Lures.water] = water
                    it[Lures.rarityBonus] = rarityBonus
                }.value
            } else {
                val id = row[Lures.id].value
                Lures.update({ Lures.id eq id }) {
                    it[Lures.predator] = predator
                    it[Lures.water] = water
                    it[Lures.rarityBonus] = rarityBonus
                }
                id
            }
        }

        fun upsertRod(
            code: String,
            name: String,
            unlock: Double,
            bonusWater: String? = null,
            bonusPredator: Boolean? = null,
        ): Long {
            val row = Rods.select { Rods.code eq code }.singleOrNull()
                ?: Rods.select { Rods.name eq name }.singleOrNull()
            return if (row == null) {
                Rods.insertAndGetId {
                    it[Rods.code] = code
                    it[Rods.name] = name
                    it[priceStars] = null
                    it[modsJson] = "{}"
                    it[unlockKg] = unlock
                    it[Rods.bonusWater] = bonusWater
                    it[Rods.bonusPredator] = bonusPredator
                }.value
            } else {
                val id = row[Rods.id].value
                Rods.update({ Rods.id eq id }) {
                    it[Rods.code] = code
                    it[Rods.name] = name
                    it[unlockKg] = unlock
                    it[Rods.bonusWater] = bonusWater
                    it[Rods.bonusPredator] = bonusPredator
                }
                id
            }
        }

        // --- Locations (rebalance unlockKg & sizeMultiplier) ---
        val pond        = upsertLocation("Пруд",            0.0,   1.0)
        val swamp       = upsertLocation("Болото",          5.0,   1.15)
        val river       = upsertLocation("Река",            20.0,  1.4)
        val lake        = upsertLocation("Озеро",           80.0,  1.8)
        val reservoir   = upsertLocation("Водохранилище",   180.0, 2.1)
        val mtnRiver    = upsertLocation("Горная река",     300.0, 2.3)
        val delta       = upsertLocation("Дельта реки",     450.0, 2.5)
        val coast       = upsertLocation("Прибрежье моря",  650.0, 2.8)
        val amazon      = upsertLocation("Русло Амазонки",          900.0,  3.0)
        val igapo       = upsertLocation("Игапо, затопленный лес",  1100.0, 3.05)
        val mangrove    = upsertLocation("Мангровые заросли",       1500.0, 3.1)
        val coralFlats  = upsertLocation("Коралловые отмели",       2000.0, 3.2)
        val fjord       = upsertLocation("Фьорд",           2600.0, 3.4)
        val openOcean   = upsertLocation("Открытый океан",  5000.0, 4.2)

        // --- Fish: common пресные мирные---
        val fCommonPeaceFreshPlotva  = upsertFish("Плотва", "common", 0.2, 0.05, false, "fresh")
        val fCommonPeaceFreshKaras  = upsertFish("Карась", "common", 0.3, 0.1, false, "fresh")
        val fCommonPeaceFreshUkleyka  = upsertFish("Уклейка", "common", 0.05, 0.02, false, "fresh")
        val fCommonPeaceFreshPeskar  = upsertFish("Пескарь", "common", 0.07, 0.03, false, "fresh")
        val fCommonPeaceFreshGustera = upsertFish("Густера", "common", 0.35, 0.12, false, "fresh")
        val fCommonPeaceFreshKrasnoperka = upsertFish("Краснопёрка", "common", 0.15, 0.05, false, "fresh")
        val fCommonPeaceFreshElets = upsertFish("Елец", "common", 0.12, 0.05, false, "fresh")
        val fCommonPeaceFreshVerhoplavka  = upsertFish("Верхоплавка", "common", 0.01, 0.005, false, "fresh")
        val fCommonPeaceFreshGolyan = upsertFish("Гольян", "common", 0.02, 0.01, false, "fresh")
        val fCommonPeaceFreshProhilodus = upsertFish("Прохилодус", "common", 1.5, 0.7, false, "fresh")
        val fCommonPeaceFreshOtocinklyus = upsertFish("Отоцинклюс", "common", 0.01, 0.005, false, "fresh")
        val fCommonPeaceFreshNeonTetra = upsertFish("Неоновая тетра", "common", 0.003, 0.0015, false, "fresh")
        val fCommonPeaceFreshCardinalTetra   = upsertFish("Кардинальная тетра", "common", 0.003, 0.0015, false, "fresh")
        val fCommonPeaceFreshNannostomus  = upsertFish("Нанностомус", "common", 0.004, 0.002, false, "fresh")

        // --- Fish: common пресные хищные---
        val fCommonPredatorFreshOkun  = upsertFish("Окунь", "common", 0.25, 0.07, true, "fresh")
        val fCommonPredatorFreshRotan  = upsertFish("Ротан", "common", 0.15, 0.05, true, "fresh")
        val fCommonPredatorFreshErsch  = upsertFish("Ёрш", "common", 0.08, 0.03, true, "fresh")

        // --- Fish: uncommon пресные мирные---
        val fUncommonPeaceFreshLin  = upsertFish("Линь", "uncommon", 0.7, 0.3, false, "fresh")
        val fUncommonPeaceFreshChehon  = upsertFish("Чехонь", "uncommon", 0.4, 0.15, false, "fresh")
        val fUncommonPeaceFreshSig  = upsertFish("Сиг", "uncommon", 1.2, 0.5, false, "fresh")
        val fUncommonPeaceFreshLesch  = upsertFish("Лещ", "uncommon", 0.8, 0.2, false, "fresh")
        val fUncommonPeaceFreshPacuBlack = upsertFish("Паку чёрный", "uncommon", 6.0, 3.0, false, "fresh")
        val fUncommonPeaceFreshAncistrus = upsertFish("Анциструс", "uncommon", 0.10, 0.05, false, "fresh")
        val fUncommonPeaceFreshTernecia = upsertFish("Тернеция", "uncommon", 0.02, 0.01, false, "fresh")
        val fUncommonPeaceFreshScalaria = upsertFish("Скалярия", "uncommon", 0.15, 0.08, false, "fresh")
        val fUncommonPeaceFreshAgassisa  = upsertFish("Апистограмма Агассиза", "uncommon", 0.05, 0.02, false, "fresh")
        val fUncommonPeaceFreshCoridorusPanda  = upsertFish("Коридорас панда", "uncommon", 0.05, 0.02, false, "fresh")

        // --- Fish: uncommon пресные хищные---
        val fUncommonPredatorFreshNalim  = upsertFish("Налим", "uncommon", 1.5, 0.8, true, "fresh")
        val fUncommonPredatorFreshGolavl  = upsertFish("Голавль", "uncommon", 0.8, 0.4, true, "fresh")
        val fUncommonPredatorFreshYaz = upsertFish("Язь", "uncommon", 1.20, 0.50, true, "fresh")
        val fUncommonPredatorFreshPiranhaRed = upsertFish("Пиранья краснобрюхая", "uncommon", 0.8, 0.4, true, "fresh")

        // --- Fish: rare пресные мирные---
        val fRarePeaceFreshTolstolobik  = upsertFish("Толстолобик", "rare", 4.0, 2.0, false, "fresh")
        val fRarePeaceFreshWhiteAmur  = upsertFish("Белый амур", "rare", 3.5, 1.5, false, "fresh")
        val fRarePeaceFreshKarp = upsertFish("Карп", "rare", 2.5, 1.0, false, "fresh")
        val fRarePeaceFreshRamiresi    = upsertFish("Рамирези", "rare", 0.05, 0.02, false, "fresh")

        // --- Fish: rare пресные хищные---
        val fRarePredatorFreshSudak  = upsertFish("Судак", "rare", 2.0, 1.0, true, "fresh")
        val fRarePredatorFreshHarius  = upsertFish("Хариус", "rare", 0.6, 0.25, true, "fresh")
        val fRarePredatorFreshForelStream  = upsertFish("Форель ручьевая", "rare", 1.2, 0.6, true, "fresh")
        val fRarePredatorFreshZhereh  = upsertFish("Жерех", "rare", 2.0, 1.0, true, "fresh")
        val fRarePredatorFreshSchuka = upsertFish("Щука", "rare", 3.0, 1.2, true, "fresh")
        val fRarePredatorFreshTraira = upsertFish("Трайра", "rare", 2.5, 1.0, true, "fresh")
        val fRarePredatorFreshAcestrinks = upsertFish("Ацестринх", "rare", 1.0, 0.5, true, "fresh")
        val fRarePredatorFreshOscar  = upsertFish("Оскар", "rare", 1.5, 0.7, true, "fresh")
        val fRarePredatorFreshPiranhaBlack = upsertFish("Пиранья чёрная", "rare", 1.3, 0.6, true, "fresh")
        val fRarePredatorFreshSchuchyaCihlida = upsertFish("Щучья цихлида", "rare", 0.9, 0.4, true, "fresh")

        // --- Fish: epic пресные мирные---
        val fEpicPeaceFreshSterlyad= upsertFish("Стерлядь", "epic", 3.0, 1.2, false, "fresh")
        val fEpicPeaceFreshTambaki = upsertFish("Тамбаки", "epic", 12.0, 6.0, false, "fresh")
        val fEpicPeaceFreshDiscus = upsertFish("Дискус", "epic", 0.20, 0.10, false, "fresh")

        // --- Fish: epic пресные хищные---
        val fEpicPredatorFreshEelEuropean = upsertFish("Угорь европейский", "epic", 1.5, 0.7, true, "fresh")
        val fEpicPredatorFreshSom = upsertFish("Сом", "epic", 8.0, 4.0, true, "fresh")
        val fEpicPredatorFreshArcticGolets = upsertFish("Голец арктический", "epic", 2.8, 1.2, true, "fresh")
        val fEpicPredatorFreshForelKumzha = upsertFish("Форель кумжа", "epic", 4.0, 1.5, true, "fresh")
        val fEpicPredatorFreshElectricEel = upsertFish("Электрический угорь", "epic", 15.0, 5.0, true, "fresh")
        val fEpicPredatorFreshRedTailSom = upsertFish("Краснохвостый сом", "epic", 30.0, 12.0, true, "fresh")
        val fEpicPredatorFreshAimara = upsertFish("Аймара", "epic", 8.0, 4.0, true, "fresh")
        val fEpicPredatorFreshTigerPseudoplatistoma   = upsertFish("Псевдоплатистома тигровая", "epic", 10.0, 5.0, true, "fresh")
        val fEpicPredatorFreshPeacockOkun = upsertFish("Павлиний окунь", "epic", 4.0, 2.0, true, "fresh")

        // --- Fish: mythic пресные мирные---
        val fKoiKohaku      = upsertFish("Карп кои (Кохаку)",           "mythic", 2.5, 1.2, false, "fresh")
        val fKoiSanke       = upsertFish("Карп кои (Тайсё Сансёку)",    "mythic", 2.6, 1.3, false, "fresh")
        val fKoiShowa       = upsertFish("Карп кои (Сёва Сансёку)",     "mythic", 2.8, 1.4, false, "fresh")
        val fKoiUtsuri      = upsertFish("Карп кои (Уцуримоно)",        "mythic", 3.0, 1.5, false, "fresh")
        val fKoiBekko       = upsertFish("Карп кои (Бэкко)",            "mythic", 2.4, 1.1, false, "fresh")
        val fKoiTancho      = upsertFish("Карп кои (Тантё)",            "mythic", 2.7, 1.2, false, "fresh")
        val fKoiAsagi       = upsertFish("Карп кои (Асаги)",            "mythic", 3.2, 1.6, false, "fresh")
        val fKoiShusui      = upsertFish("Карп кои (Сюсуй)",            "mythic", 3.0, 1.4, false, "fresh")
        val fKoiKoromo      = upsertFish("Карп кои (Коромо)",           "mythic", 2.6, 1.2, false, "fresh")
        val fKoiGinrin      = upsertFish("Карп кои (Кингинрин)",        "mythic", 2.3, 1.0, false, "fresh")
        val fKoiKawarimono  = upsertFish("Карп кои (Каваримоно)",       "mythic", 3.0, 1.5, false, "fresh")
        val fKoiOgon        = upsertFish("Карп кои (Огон)",             "mythic", 3.4, 1.7, false, "fresh")
        val fKoiHikariM     = upsertFish("Карп кои (Хикари-моёмоно)",   "mythic", 2.9, 1.3, false, "fresh")
        val fKoiGoshiki     = upsertFish("Карп кои (Госики)",           "mythic", 2.8, 1.3, false, "fresh")
        val fKoiKumonryu    = upsertFish("Карп кои (Кумонрю)",          "mythic", 2.5, 1.1, false, "fresh")
        val fKoiDoitsu      = upsertFish("Карп кои (Дойцу-гои)",        "mythic", 2.6, 1.1, false, "fresh")
        val fMythicPeaceFreshBlackAmur      = upsertFish("Чёрный амур",                 "mythic",12.0, 5.0, false, "fresh")

        // --- Fish: mythic пресные хищные---
        val fMythicPredatorFreshNorthSnakehead  = upsertFish("Змееголов северный", "mythic", 4.0, 2.0, true, "fresh")
        val fMythicPredatorFreshAmurPike   = upsertFish("Амурская щука", "mythic", 5.0, 2.0, true, "fresh")
        val fMythicPredatorFreshKristivomer  = upsertFish("Кристивомер", "mythic", 10.0, 5.0, true, "fresh")
        val fMythicPredatorFreshDanubeLosos     = upsertFish("Дунайский лосось", "mythic", 10.0, 4.0, true, "fresh")
        val fMythicPredatorFreshZungaro    = upsertFish("Зунгаро", "mythic", 25.0, 10.0, true, "fresh")
        val fMythicPredatorFreshSkatMotoro     = upsertFish("Скат моторо", "mythic", 12.0, 5.0, true, "fresh")

        // --- Fish: legendary пресные мирные---
        val fLegendaryPeaceFreshOsetr = upsertFish("Осётр", "legendary", 12.0, 6.0, false, "fresh")

        // --- Fish: legendary пресные хищные---
        val fLegendaryPredatorFreshTaimen  = upsertFish("Таймень", "legendary", 15.0, 7.0, true, "fresh")
        val fLegendaryPredatorFreshArapaima = upsertFish("Арапайма", "legendary", 80.0, 30.0, true, "fresh")
        val fLegendaryPredatorFreshPiraiba = upsertFish("Пираиба", "legendary", 70.0, 35.0, true, "fresh")
        val fLegendaryPredatorFreshBlackArowana  = upsertFish("Аравана чёрная", "legendary", 6.0, 3.0, true, "fresh")

        // --- Fish: common морские мирные---
        val fCommonPeaceSaltSeld  = upsertFish("Сельдь", "common", 0.3, 0.1, false, "salt")
        val fCommonPeaceSaltKilka = upsertFish("Килька", "common", 0.03, 0.01, false, "salt")
        val fCommonPeaceSaltMoiva = upsertFish("Мойва", "common", 0.04, 0.015, false, "salt")
        val fCommonPeaceSaltSardina = upsertFish("Сардина", "common", 0.12, 0.05, false, "salt")
        val fCommonPeaceSaltAnchous = upsertFish("Анчоус", "common", 0.02, 0.01, false, "salt")
        val fCommonPeaceSaltStainedKefal  = upsertFish("Пятнистая кефаль", "common", 0.8, 0.4, false, "salt")
        val fCommonPeaceSaltTropicAnchous = upsertFish("Анчоус тропический", "common", 0.02, 0.01, false, "salt")
        val fCommonPeaceSaltSardinaIndian  = upsertFish("Сардина индийская", "common", 0.10, 0.05, false, "salt")
        val fCommonPeaceSaltBlueHrisiptera = upsertFish("Синяя хризиптера", "common", 0.04, 0.02, false, "salt")
        val fCommonPeaceSaltFusilerYellowTail  = upsertFish("Фузилёр жёлтохвостый", "common", 0.3, 0.1, false, "salt")

        // --- Fish: common морские хищные---
        val fCommonPredatorSaltStavrida  = upsertFish("Ставрида", "common", 0.25, 0.1, true, "salt")
        val fCommonPredatorSaltKoryushka  = upsertFish("Корюшка", "common", 0.06, 0.02, true, "salt")
        val fCommonPredatorSaltBychok = upsertFish("Бычок", "common", 0.12, 0.06, true, "salt")

        // --- Fish: uncommon морские мирные---
        val fUncommonPeaceSaltKefal  = upsertFish("Кефаль", "uncommon", 1.0, 0.5, false, "salt")
        val fUncommonPeaceSaltKambala  = upsertFish("Камбала", "uncommon", 0.8, 0.4, false, "salt")
        val fUncommonPeaceSaltPiksha = upsertFish("Пикша", "uncommon", 2.0, 1.0, false, "salt")
        val fUncommonPeaceSaltSaira = upsertFish("Сайра", "uncommon", 0.3, 0.15, false, "salt")
        val fUncommonPeaceSaltFlyFish = upsertFish("Летучая рыба", "uncommon", 0.4, 0.2, false, "salt")
        val fUncommonPeaceSaltTilyapiaMozambik   = upsertFish("Тиляпия мозамбикская", "uncommon", 1.0, 0.5, false, "salt")
        val fUncommonPeaceSaltParrotFish     = upsertFish("Рыба-попугай", "uncommon", 2.0, 1.0, false, "salt")
        val fUncommonPeaceSaltButterlyThreadnose = upsertFish("Нитеносная бабочка", "uncommon", 0.2, 0.08, false, "salt")
        val fUncommonPeaceSaltBarabulkaTropic   = upsertFish("Барабулька тропическая", "uncommon", 0.5, 0.2, false, "salt")

        // --- Fish: uncommon морские хищные---
        val fUncommonPredatorSaltSaida  = upsertFish("Сайда", "uncommon", 2.0, 1.0, true, "salt")
        val fUncommonPredatorSaltAtlanticSkumbria = upsertFish("Скумбрия атлантическая", "uncommon", 0.6, 0.25, true, "salt")
        val fUncommonPredatorSaltSeaSom   = upsertFish("Морской сом", "uncommon", 1.2, 0.6, true, "salt")
        val fUncommonPredatorSaltSeaSargan   = upsertFish("Морской сарган", "uncommon", 0.7, 0.3, true, "salt")
        val fUncommonPredatorSaltBychokMangroves = upsertFish("Бычок мангровый", "uncommon", 0.07, 0.03, true, "salt")

        // --- Fish: rare морские мирные---
        val fRarePeaceSaltGoldenSigan   = upsertFish("Золотистый сиган", "rare", 0.5, 0.2, false, "salt")
        val fRarePeaceSaltBlueHirurg   = upsertFish("Голубой хирург", "rare", 0.4, 0.15, false, "salt")

        // --- Fish: rare морские хищные---
        val fRarePredatorSaltTreska  = upsertFish("Треска", "rare", 3.0, 1.5, true, "salt")
        val fRarePredatorSaltSeaForel  = upsertFish("Морская форель", "rare", 1.5, 0.7, true, "salt")
        val fRarePredatorSaltLavrak = upsertFish("Лаврак", "rare", 2.0, 1.0, true, "salt")
        val fRarePredatorSaltDorado = upsertFish("Дорадо", "rare", 6.0, 3.0, true, "salt")
        val fRarePredatorSaltWahoo = upsertFish("Ваху", "rare", 10.0, 5.0, true, "salt")
        val fRarePredatorSaltAlbakor = upsertFish("Альбакор", "rare", 18.0, 7.0, true, "salt")
        val fRarePredatorSaltSnook= upsertFish("Снук", "rare", 4.0, 2.0, true, "salt")
        val fRarePredatorSaltMangrovesLucian= upsertFish("Мангровый луциан", "rare", 2.0, 1.0, true, "salt")
        val fRarePredatorSaltAlbula= upsertFish("Альбула", "rare", 3.0, 1.0, true, "salt")
        val fRarePredatorSaltSpanishSkumbria= upsertFish("Испанская скумбрия", "rare", 4.0, 2.0, true, "salt")
        val fRarePredatorSaltTitanSpinorog= upsertFish("Спинорог-титан", "rare", 3.0, 1.0, true, "salt")

        // --- Fish: epic морские мирные---
        val fEpicPeaceSaltTurbo = upsertFish("Тюрбо", "epic", 6.0, 3.0, false, "salt")
        val fEpicPeaceSaltMoonFish= upsertFish("Рыба-луна", "epic", 220.0, 90.0, false, "salt")
        val fEpicPeaceSaltMilkfish= upsertFish("Молочная рыба", "epic", 3.5, 1.5, false, "salt")
        val fEpicPeaceSaltEmperorAngel    = upsertFish("Императорский ангел", "epic", 0.5, 0.2, false, "salt")

        // --- Fish: epic морские хищные---
        val fEpicPredatorSaltLososAtlantic = upsertFish("Лосось атлантический", "epic", 6.0, 3.0, true, "salt")
        val fEpicPredatorSaltParusnik= upsertFish("Парусник", "epic", 30.0, 12.0, true, "salt")
        val fEpicPredatorSaltSwordFish = upsertFish("Рыба-меч", "epic", 55.0, 25.0, true, "salt")
        val fEpicPredatorSaltAkulaMako = upsertFish("Акула мако", "epic", 70.0, 30.0, true, "salt")
        val fEpicPredatorSaltBarramundi    = upsertFish("Баррамунди", "epic", 10.0, 5.0, true, "salt")
        val fEpicPredatorSaltBlueKaranks  = upsertFish("Голубой каранкс", "epic", 5.0, 2.0, true, "salt")
        val fEpicPredatorSaltBigBarracuda = upsertFish("Большая барракуда", "epic", 12.0, 6.0, true, "salt")
        val fEpicPredatorSaltPermit   = upsertFish("Пермит", "epic", 6.0, 3.0, true, "salt")
        val fEpicPredatorSaltCoralForel = upsertFish("Коралловая форель", "epic", 5.0, 2.0, true, "salt")

        // --- Fish: mythic морские мирные---
        val fMythicPeaceSaltPelingas= upsertFish("Пеленгас","mythic", 3.0, 1.2, false, "salt")

        // --- Fish: mythic морские хищные---
        val fMythicPredatorSaltCubera     = upsertFish("Кубера", "mythic", 12.0, 5.0, true, "salt")
        val fMythicPredatorSaltWolffish   = upsertFish("Зубатка пятнистая", "mythic", 8.0, 3.0, true, "salt")
        val fMythicPredatorSaltYellowTunec     = upsertFish("Тунец желтоперый", "mythic", 50.0, 20.0, true, "salt")
        val fMythicPredatorSaltBlackSnook = upsertFish("Чёрный снук", "mythic", 8.0, 3.0, true, "salt")
        val fMythicPredatorSaltNapoleonFish   = upsertFish("Рыба-наполеон", "mythic", 35.0, 15.0, true, "salt")

        // --- Fish: legendary морские мирные---
        val fLegendaryPeaceSaltSeldyanoyKorol = upsertFish("Сельдяной король", "legendary", 120.0, 50.0, false, "salt")

        // --- Fish: legendary морские хищные---
        val fLegendaryPredatorSaltPaltus = upsertFish("Палтус", "legendary", 20.0, 10.0, true, "salt")
        val fLegendaryPredatorSaltBeluga = upsertFish("Белуга", "legendary", 40.0, 20.0, true, "salt")
        val fLegendaryPredatorSaltBlueMarlin = upsertFish("Марлин синий", "legendary", 180.0, 80.0, true, "salt")
        val fLegendaryPredatorSaltBlueTunec = upsertFish("Тунец синеперый", "legendary", 200.0, 90.0, true, "salt")
        val fLegendaryPredatorSaltTarpon   = upsertFish("Тарпон", "legendary", 25.0, 12.0, true, "salt")
        val fLegendaryPredatorSaltGiantKaranks = upsertFish("Гигантский каранкс", "legendary", 25.0, 10.0, true, "salt")


        // --- Rods ---
        upsertRod("spark", "Искра", 0.0, null, null)
        upsertRod("dew",   "Роса",  15.0, "fresh", false)
        upsertRod("stream","Поток", 150.0, "fresh", true)
        upsertRod("abyss", "Глубь", 450.0, "salt",  false)
        upsertRod("storm", "Шторм", 1000.0, "salt", true)

        // --- Weights per location ---

        // Пруд мирные
        setLFWeight(pond, fCommonPeaceFreshKaras, 1.1)
        setLFWeight(pond, fCommonPeaceFreshPlotva, 1.0)
        setLFWeight(pond, fCommonPredatorFreshOkun, 0.9)
        setLFWeight(pond, fCommonPeaceFreshUkleyka, 0.7)
        setLFWeight(pond, fCommonPeaceFreshPeskar, 0.7)
        setLFWeight(pond, fCommonPeaceFreshKrasnoperka, 0.7)
        setLFWeight(pond, fCommonPeaceFreshGustera, 0.5)
        setLFWeight(pond, fCommonPeaceFreshVerhoplavka, 0.5)
        setLFWeight(pond, fUncommonPeaceFreshLesch, 0.4)
        setLFWeight(pond, fUncommonPeaceFreshLin, 0.2)
        setLFWeight(pond, fRarePeaceFreshKarp, 0.15)
        setLFWeight(pond, fKoiKohaku, 0.01)
        setLFWeight(pond, fKoiSanke, 0.01)
        setLFWeight(pond, fKoiShowa, 0.01)
        setLFWeight(pond, fKoiUtsuri, 0.01)
        setLFWeight(pond, fKoiBekko, 0.01)
        setLFWeight(pond, fKoiTancho, 0.01)
        setLFWeight(pond, fKoiAsagi, 0.01)
        setLFWeight(pond, fKoiShusui, 0.01)
        setLFWeight(pond, fKoiKoromo, 0.01)
        setLFWeight(pond, fKoiGinrin, 0.01)
        setLFWeight(pond, fKoiKawarimono, 0.01)
        setLFWeight(pond, fKoiOgon, 0.01)
        setLFWeight(pond, fKoiHikariM, 0.01)
        setLFWeight(pond, fKoiGoshiki, 0.01)
        setLFWeight(pond, fKoiKumonryu, 0.01)
        setLFWeight(pond, fKoiDoitsu, 0.01)

        // Пруд хищные
        setLFWeight(pond, fCommonPredatorFreshErsch, 0.7)
        setLFWeight(pond, fCommonPredatorFreshRotan, 0.6)
        setLFWeight(pond, fCommonPredatorFreshOkun, 0.4)
        setLFWeight(pond, fUncommonPredatorFreshYaz, 0.25)
        setLFWeight(pond, fRarePredatorFreshSchuka, 0.12)
        setLFWeight(pond, fEpicPredatorFreshEelEuropean, 0.06)
        setLFWeight(pond, fEpicPredatorFreshSom, 0.03)

        // Болото мирные
        setLFWeight(swamp, fCommonPeaceFreshKaras, 1.3)
        setLFWeight(swamp, fCommonPeaceFreshKrasnoperka, 0.8)
        setLFWeight(swamp, fCommonPeaceFreshGustera, 0.7)
        setLFWeight(swamp, fCommonPeaceFreshPlotva, 0.5)
        setLFWeight(swamp, fCommonPeaceFreshVerhoplavka, 0.5)
        setLFWeight(swamp, fCommonPeaceFreshPeskar, 0.4)
        setLFWeight(swamp, fUncommonPeaceFreshLin, 0.3)
        setLFWeight(swamp, fRarePeaceFreshKarp, 0.2)

        // Болото хищные
        setLFWeight(swamp, fCommonPredatorFreshRotan, 1.0)
        setLFWeight(swamp, fCommonPredatorFreshErsch, 0.7)
        setLFWeight(swamp, fCommonPredatorFreshOkun, 0.6)
        setLFWeight(swamp, fUncommonPredatorFreshYaz, 0.4)
        setLFWeight(swamp, fRarePredatorFreshSchuka, 0.2);
        setLFWeight(swamp, fEpicPredatorFreshEelEuropean, 0.1)
        setLFWeight(swamp, fEpicPredatorFreshSom, 0.04)
        setLFWeight(swamp, fMythicPredatorFreshNorthSnakehead, 0.02)

        // Река мирные
        setLFWeight(river, fCommonPeaceFreshPlotva, 0.9)
        setLFWeight(river, fCommonPeaceFreshKaras, 0.8)
        setLFWeight(river, fCommonPeaceFreshPeskar, 0.7)
        setLFWeight(river, fCommonPeaceFreshGustera, 0.65)
        setLFWeight(river, fCommonPeaceFreshElets, 0.6);
        setLFWeight(river, fCommonPeaceFreshKrasnoperka, 0.6)
        setLFWeight(river, fUncommonPeaceFreshLesch, 0.5)
        setLFWeight(river, fUncommonPeaceFreshSig, 0.45);
        setLFWeight(river, fUncommonPeaceFreshChehon, 0.4)
        setLFWeight(river, fRarePeaceFreshKarp, 0.3)
        setLFWeight(river, fRarePeaceFreshTolstolobik, 0.2)
        setLFWeight(river, fRarePeaceFreshWhiteAmur, 0.15)
        setLFWeight(river, fEpicPeaceFreshSterlyad, 0.05)
        setLFWeight(river, fLegendaryPeaceFreshOsetr, 0.02)

        // Река хищные
        setLFWeight(river, fCommonPredatorFreshOkun, 0.9)
        setLFWeight(river, fCommonPredatorFreshErsch, 0.8)
        setLFWeight(river, fUncommonPredatorFreshGolavl, 0.6)
        setLFWeight(river, fUncommonPredatorFreshYaz, 0.5)
        setLFWeight(river, fUncommonPredatorFreshNalim, 0.4)
        setLFWeight(river, fRarePredatorFreshSudak, 0.35)
        setLFWeight(river, fRarePredatorFreshZhereh, 0.3)
        setLFWeight(river, fRarePredatorFreshSchuka, 0.25)
        setLFWeight(river, fEpicPredatorFreshEelEuropean, 0.1)
        setLFWeight(river, fEpicPredatorFreshSom, 0.08)
        setLFWeight(river, fMythicPredatorFreshAmurPike, 0.03)
        setLFWeight(river, fMythicPredatorFreshNorthSnakehead, 0.02)

        // Озеро мирные
        setLFWeight(lake, fCommonPeaceFreshKaras, 0.9)
        setLFWeight(lake, fCommonPeaceFreshPlotva, 0.8)
        setLFWeight(lake, fCommonPeaceFreshGustera, 0.7)
        setLFWeight(lake, fCommonPeaceFreshKrasnoperka, 0.6)
        setLFWeight(lake, fCommonPeaceFreshPeskar, 0.3)
        setLFWeight(lake, fCommonPeaceFreshElets, 0.3)
        setLFWeight(lake, fUncommonPeaceFreshLesch, 0.5)
        setLFWeight(lake, fUncommonPeaceFreshSig, 0.4)
        setLFWeight(lake, fUncommonPeaceFreshChehon, 0.35)
        setLFWeight(lake, fRarePeaceFreshKarp, 0.35)
        setLFWeight(lake, fRarePeaceFreshTolstolobik, 0.3)
        setLFWeight(lake, fRarePeaceFreshWhiteAmur, 0.2)
        setLFWeight(lake, fEpicPeaceFreshSterlyad, 0.07)
        setLFWeight(lake, fLegendaryPeaceFreshOsetr, 0.05)

        // Озеро хищные
        setLFWeight(lake, fCommonPredatorFreshOkun, 0.9)
        setLFWeight(lake, fCommonPredatorFreshErsch, 0.8)
        setLFWeight(lake, fUncommonPredatorFreshYaz, 0.55)
        setLFWeight(lake, fUncommonPredatorFreshGolavl, 0.5)
        setLFWeight(lake, fUncommonPredatorFreshNalim, 0.4)
        setLFWeight(lake, fRarePredatorFreshSchuka, 0.35)
        setLFWeight(lake, fRarePredatorFreshHarius, 0.3)
        setLFWeight(lake, fEpicPredatorFreshSom, 0.12)
        setLFWeight(lake, fEpicPredatorFreshEelEuropean, 0.08)
        setLFWeight(lake, fMythicPredatorFreshKristivomer, 0.05)
        setLFWeight(lake, fLegendaryPredatorFreshTaimen, 0.03)

        // Водохранилище мирные
        setLFWeight(reservoir, fCommonPeaceFreshPlotva, 0.8)
        setLFWeight(reservoir, fCommonPeaceFreshKaras, 0.75)
        setLFWeight(reservoir, fCommonPeaceFreshKrasnoperka, 0.75)
        setLFWeight(reservoir, fCommonPeaceFreshUkleyka, 0.7)
        setLFWeight(reservoir, fCommonPeaceFreshGustera, 0.6)
        setLFWeight(reservoir, fCommonPeaceFreshPeskar, 0.5)
        setLFWeight(reservoir, fUncommonPeaceFreshLesch, 0.45)
        setLFWeight(reservoir, fUncommonPeaceFreshLin, 0.25)
        setLFWeight(reservoir, fRarePeaceFreshKarp, 0.25)
        setLFWeight(reservoir, fRarePeaceFreshTolstolobik, 0.2)
        setLFWeight(reservoir, fRarePeaceFreshWhiteAmur, 0.2)
        setLFWeight(reservoir, fEpicPeaceFreshSterlyad, 0.05)
        setLFWeight(reservoir, fMythicPeaceFreshBlackAmur, 0.035)
        setLFWeight(reservoir, fLegendaryPeaceFreshOsetr, 0.02)

        // Водохранилище хищные
        setLFWeight(reservoir, fCommonPredatorFreshOkun, 0.9)
        setLFWeight(reservoir, fCommonPredatorFreshErsch, 0.75)
        setLFWeight(reservoir, fUncommonPredatorFreshYaz, 0.5)
        setLFWeight(reservoir, fUncommonPredatorFreshNalim, 0.45)
        setLFWeight(reservoir, fRarePredatorFreshSchuka, 0.3)
        setLFWeight(reservoir, fRarePredatorFreshSudak, 0.25)
        setLFWeight(reservoir, fRarePredatorFreshZhereh, 0.25)
        setLFWeight(reservoir, fEpicPredatorFreshSom, 0.15)
        setLFWeight(reservoir, fEpicPredatorFreshEelEuropean, 0.1)

        // Горная река мирные
        setLFWeight(mtnRiver, fCommonPeaceFreshPlotva, 0.8)
        setLFWeight(mtnRiver, fCommonPeaceFreshUkleyka, 0.7)
        setLFWeight(mtnRiver, fCommonPeaceFreshGolyan, 0.65)
        setLFWeight(mtnRiver, fCommonPeaceFreshPeskar, 0.6)
        setLFWeight(mtnRiver, fCommonPeaceFreshKaras, 0.6)
        setLFWeight(mtnRiver, fCommonPeaceFreshElets, 0.5)
        setLFWeight(mtnRiver, fUncommonPeaceFreshSig, 0.4)
        setLFWeight(mtnRiver, fRarePeaceFreshKarp, 0.35)
        setLFWeight(mtnRiver, fEpicPeaceFreshSterlyad, 0.05)
        setLFWeight(mtnRiver, fLegendaryPeaceFreshOsetr, 0.02)

        // Горная река хищные
        setLFWeight(mtnRiver, fCommonPredatorFreshOkun, 0.9)
        setLFWeight(mtnRiver, fCommonPredatorFreshErsch, 0.6)
        setLFWeight(mtnRiver, fUncommonPredatorFreshGolavl, 0.5)
        setLFWeight(mtnRiver, fUncommonPredatorFreshNalim, 0.5)
        setLFWeight(mtnRiver, fUncommonPredatorFreshYaz, 0.45)
        setLFWeight(mtnRiver, fRarePredatorFreshForelStream, 0.4)
        setLFWeight(mtnRiver, fRarePredatorFreshHarius, 0.3)
        setLFWeight(mtnRiver, fRarePredatorFreshSchuka, 0.15)
        setLFWeight(mtnRiver, fEpicPredatorFreshArcticGolets, 0.1)
        setLFWeight(mtnRiver, fEpicPredatorFreshForelKumzha, 0.07)
        setLFWeight(mtnRiver, fMythicPredatorFreshDanubeLosos, 0.04)
        setLFWeight(mtnRiver, fLegendaryPredatorFreshTaimen, 0.03)

        // Дельта реки пресная мирная
        setLFWeight(delta, fCommonPeaceFreshPlotva, 0.8)
        setLFWeight(delta, fCommonPeaceFreshGustera, 0.75)
        setLFWeight(delta, fCommonPeaceFreshKrasnoperka, 0.75)
        setLFWeight(delta, fCommonPeaceFreshPeskar, 0.6)
        setLFWeight(delta, fUncommonPeaceFreshChehon, 0.5)
        setLFWeight(delta, fUncommonPeaceFreshLesch, 0.5)
        setLFWeight(delta, fRarePeaceFreshTolstolobik, 0.3)
        setLFWeight(delta, fRarePeaceFreshWhiteAmur, 0.25)
        setLFWeight(delta, fEpicPeaceFreshSterlyad, 0.08)
        setLFWeight(delta, fMythicPeaceFreshBlackAmur, 0.04)
        setLFWeight(delta, fLegendaryPeaceFreshOsetr, 0.03)

        // Дельта реки пресная хищная
        setLFWeight(delta, fCommonPredatorFreshErsch, 0.9)
        setLFWeight(delta, fCommonPredatorFreshOkun, 0.9)
        setLFWeight(delta, fUncommonPredatorFreshYaz, 0.55)
        setLFWeight(delta, fUncommonPredatorFreshGolavl, 0.5)
        setLFWeight(delta, fUncommonPredatorFreshNalim, 0.5)
        setLFWeight(delta, fRarePredatorFreshSudak, 0.35)
        setLFWeight(delta, fRarePredatorFreshSchuka, 0.3)
        setLFWeight(delta, fRarePredatorFreshZhereh, 0.25)
        setLFWeight(delta, fRarePredatorSaltLavrak, 0.18)
        setLFWeight(delta, fEpicPredatorFreshSom, 0.12)
        setLFWeight(delta, fEpicPredatorFreshEelEuropean, 0.07)
        setLFWeight(river, fMythicPredatorFreshAmurPike, 0.02)

        // Дельта реки соленая мирная
        setLFWeight(delta, fCommonPeaceSaltSeld, 0.9)
        setLFWeight(delta, fCommonPeaceSaltKilka, 0.8)
        setLFWeight(delta, fCommonPeaceSaltSardina, 0.6)
        setLFWeight(delta, fCommonPeaceSaltMoiva, 0.6)
        setLFWeight(delta, fCommonPeaceSaltAnchous, 0.55)
        setLFWeight(delta, fCommonPeaceSaltStainedKefal, 0.4)
        setLFWeight(delta, fCommonPeaceSaltSardinaIndian, 0.2)
        setLFWeight(delta, fUncommonPeaceSaltKefal, 0.45)
        setLFWeight(delta, fUncommonPeaceSaltKambala, 0.2)
        setLFWeight(delta, fEpicPeaceSaltMilkfish, 0.08)
        setLFWeight(delta, fMythicPeaceSaltPelingas, 0.03)

        // Дельта реки соленая хищная
        setLFWeight(delta, fCommonPredatorSaltBychok, 0.9)
        setLFWeight(delta, fCommonPredatorSaltKoryushka, 0.85)
        setLFWeight(delta, fUncommonPredatorSaltSeaSargan, 0.5)
        setLFWeight(delta, fUncommonPredatorSaltSeaSom, 0.35)
        setLFWeight(delta, fRarePredatorSaltSeaForel, 0.25)
        setLFWeight(delta, fRarePredatorSaltLavrak, 0.2)
        setLFWeight(delta, fEpicPredatorSaltLososAtlantic, 0.1)
        setLFWeight(delta, fLegendaryPredatorSaltBeluga, 0.02)

        // Прибрежье моря мирная
        setLFWeight(coast, fCommonPeaceSaltSeld, 0.9)
        setLFWeight(coast, fCommonPeaceSaltSardina, 0.8);
        setLFWeight(coast, fCommonPeaceSaltAnchous, 0.8)
        setLFWeight(coast, fCommonPeaceSaltKilka, 0.8);
        setLFWeight(coast, fCommonPeaceSaltMoiva, 0.7);
        setLFWeight(coast, fUncommonPeaceSaltKefal, 0.5);
        setLFWeight(coast, fUncommonPeaceSaltBarabulkaTropic, 0.5)
        setLFWeight(coast, fUncommonPeaceSaltKambala, 0.35);
        setLFWeight(coast, fEpicPeaceSaltTurbo, 0.08)
        setLFWeight(coast, fMythicPeaceSaltPelingas, 0.04)

        // Прибрежье моря хищная
        setLFWeight(coast, fCommonPredatorSaltKoryushka, 0.9)
        setLFWeight(coast, fCommonPredatorSaltBychok, 0.8)
        setLFWeight(coast, fCommonPredatorSaltStavrida, 0.6)
        setLFWeight(coast, fUncommonPredatorSaltAtlanticSkumbria, 0.5)
        setLFWeight(coast, fUncommonPredatorSaltSeaSargan, 0.45)
        setLFWeight(coast, fRarePredatorSaltSpanishSkumbria, 0.4)
        setLFWeight(coast, fRarePredatorSaltDorado, 0.4)
        setLFWeight(coast, fRarePredatorSaltSeaForel, 0.35)
        setLFWeight(coast, fRarePredatorSaltLavrak, 0.3)
        setLFWeight(coast, fRarePredatorSaltWahoo, 0.15)
        setLFWeight(coast, fEpicPredatorSaltLososAtlantic, 0.1)
        setLFWeight(coast, fEpicPredatorSaltParusnik, 0.04)
        setLFWeight(coast, fEpicPredatorSaltBlueKaranks, 0.04)
        setLFWeight(coast, fEpicPredatorSaltBigBarracuda, 0.03)
        setLFWeight(coast, fMythicPredatorSaltCubera, 0.04)
        setLFWeight(coast, fLegendaryPredatorSaltBeluga, 0.02)
        setLFWeight(coast, fLegendaryPredatorSaltTarpon, 0.02)

        // Русло Амазонки мирные
        setLFWeight(amazon, fCommonPeaceFreshNeonTetra, 0.95)
        setLFWeight(amazon, fCommonPeaceFreshProhilodus, 0.85)
        setLFWeight(amazon, fCommonPeaceFreshOtocinklyus, 0.75)
        setLFWeight(amazon, fUncommonPeaceFreshAncistrus, 0.6)
        setLFWeight(amazon, fUncommonPeaceFreshTernecia, 0.5)
        setLFWeight(amazon, fUncommonPeaceFreshPacuBlack, 0.4)
        setLFWeight(amazon, fRarePeaceFreshRamiresi, 0.25)
        setLFWeight(amazon, fEpicPeaceFreshTambaki, 0.1)
        setLFWeight(amazon, fEpicPeaceFreshDiscus, 0.08)

        // Русло Амазонки хищные
        setLFWeight(amazon, fUncommonPredatorFreshPiranhaRed, 0.5)
        setLFWeight(amazon, fRarePredatorFreshOscar, 0.45)
        setLFWeight(amazon, fRarePredatorFreshSchuchyaCihlida, 0.4)
        setLFWeight(amazon, fRarePredatorFreshAcestrinks, 0.4)
        setLFWeight(amazon, fRarePredatorFreshPiranhaBlack, 0.35)
        setLFWeight(amazon, fRarePredatorFreshTraira, 0.2)
        setLFWeight(amazon, fEpicPredatorFreshPeacockOkun, 0.1)
        setLFWeight(amazon, fEpicPredatorFreshElectricEel, 0.08)
        setLFWeight(amazon, fEpicPredatorFreshRedTailSom, 0.06)
        setLFWeight(amazon, fMythicPredatorFreshZungaro, 0.04)
        setLFWeight(amazon, fMythicPredatorFreshSkatMotoro, 0.04)
        setLFWeight(amazon, fLegendaryPredatorFreshPiraiba, 0.03)
        setLFWeight(amazon, fLegendaryPredatorFreshArapaima, 0.02)

        // Игапо, затопленный лес мирные
        setLFWeight(igapo, fCommonPeaceFreshNannostomus, 0.85)
        setLFWeight(igapo, fCommonPeaceFreshCardinalTetra, 0.85)
        setLFWeight(igapo, fCommonPeaceFreshOtocinklyus, 0.75)
        setLFWeight(igapo, fUncommonPeaceFreshScalaria, 0.5)
        setLFWeight(igapo, fUncommonPeaceFreshCoridorusPanda, 0.45)
        setLFWeight(igapo, fUncommonPeaceFreshAgassisa, 0.35)
        setLFWeight(igapo, fUncommonPeaceFreshPacuBlack, 0.3)
        setLFWeight(igapo, fRarePeaceFreshRamiresi, 0.2)
        setLFWeight(igapo, fEpicPeaceFreshDiscus, 0.1)
        setLFWeight(igapo, fEpicPeaceFreshTambaki, 0.06)

        // Игапо, затопленный лес хищные
        setLFWeight(igapo, fUncommonPredatorFreshPiranhaRed, 0.5)
        setLFWeight(igapo, fRarePredatorFreshOscar, 0.25)
        setLFWeight(igapo, fRarePredatorFreshPiranhaBlack, 0.18)
        setLFWeight(igapo, fRarePredatorFreshSchuchyaCihlida, 0.5)
        setLFWeight(igapo, fRarePredatorFreshAcestrinks, 0.4)
        setLFWeight(igapo, fEpicPredatorFreshElectricEel, 0.08)
        setLFWeight(igapo, fEpicPredatorFreshPeacockOkun, 0.06)
        setLFWeight(igapo, fEpicPredatorFreshAimara, 0.06)
        setLFWeight(igapo, fEpicPredatorFreshRedTailSom, 0.05)
        setLFWeight(igapo, fEpicPredatorFreshTigerPseudoplatistoma, 0.05)
        setLFWeight(igapo, fMythicPredatorFreshSkatMotoro, 0.04)
        setLFWeight(igapo, fMythicPredatorFreshZungaro, 0.03)
        setLFWeight(igapo, fLegendaryPredatorFreshBlackArowana, 0.02)

        // Мангровые заросли мирные
        setLFWeight(mangrove, fCommonPeaceSaltTropicAnchous, 0.9)
        setLFWeight(mangrove, fCommonPeaceSaltStainedKefal, 0.8)
        setLFWeight(mangrove, fCommonPeaceSaltSardinaIndian, 0.8)
        setLFWeight(mangrove, fUncommonPeaceSaltTilyapiaMozambik, 0.5)
        setLFWeight(mangrove, fRarePeaceSaltGoldenSigan, 0.2)
        setLFWeight(mangrove, fEpicPeaceSaltMilkfish, 0.08)

        // Мангровые заросли хищные
        setLFWeight(mangrove, fUncommonPredatorSaltBychokMangroves, 0.5)
        setLFWeight(mangrove, fUncommonPredatorSaltSeaSargan, 0.4)
        setLFWeight(mangrove, fUncommonPredatorSaltSeaSom, 0.25)
        setLFWeight(mangrove, fRarePredatorSaltSnook, 0.20)
        setLFWeight(mangrove, fRarePredatorSaltMangrovesLucian, 0.15)
        setLFWeight(mangrove, fEpicPredatorSaltBarramundi, 0.08)
        setLFWeight(mangrove, fEpicPredatorSaltBlueKaranks, 0.06)
        setLFWeight(mangrove, fMythicPredatorSaltBlackSnook, 0.045)
        setLFWeight(mangrove, fMythicPredatorSaltCubera, 0.04)
        setLFWeight(mangrove, fLegendaryPredatorSaltTarpon, 0.02)

        // Коралловые отмели мирные
        setLFWeight(coralFlats, fCommonPeaceSaltBlueHrisiptera, 0.9)
        setLFWeight(coralFlats, fCommonPeaceSaltFusilerYellowTail, 0.85)
        setLFWeight(coralFlats, fUncommonPeaceSaltBarabulkaTropic, 0.5)
        setLFWeight(coralFlats, fUncommonPeaceSaltButterlyThreadnose, 0.5)
        setLFWeight(coralFlats, fUncommonPeaceSaltParrotFish, 0.45)
        setLFWeight(coralFlats, fRarePeaceSaltGoldenSigan, 0.3)
        setLFWeight(coralFlats, fRarePeaceSaltBlueHirurg, 0.25)
        setLFWeight(coralFlats, fEpicPeaceSaltEmperorAngel, 0.08)

        // Коралловые отмели хищные
        setLFWeight(coralFlats, fRarePredatorSaltSpanishSkumbria, 0.5)
        setLFWeight(coralFlats, fRarePredatorSaltAlbula, 0.25)
        setLFWeight(coralFlats, fRarePredatorSaltTitanSpinorog, 0.22)
        setLFWeight(coralFlats, fRarePredatorSaltMangrovesLucian, 0.17)
        setLFWeight(coralFlats, fEpicPredatorSaltPermit, 0.08)
        setLFWeight(coralFlats, fEpicPredatorSaltBigBarracuda, 0.06)
        setLFWeight(coralFlats, fEpicPredatorSaltCoralForel, 0.06)
        setLFWeight(coralFlats, fMythicPredatorSaltNapoleonFish, 0.045)
        setLFWeight(coralFlats, fMythicPredatorSaltCubera, 0.04)
        setLFWeight(coralFlats, fLegendaryPredatorSaltGiantKaranks, 0.02)

        // Фьорд — мирные и хищные
        setLFWeight(fjord, fUncommonPeaceSaltKambala, 0.60)
        setLFWeight(fjord, fUncommonPeaceSaltPiksha, 0.50)
        setLFWeight(fjord, fCommonPeaceSaltSeld, 0.60); setLFWeight(fjord, fCommonPeaceSaltSardina, 0.12); setLFWeight(fjord, fCommonPeaceSaltAnchous, 0.08)
        setLFWeight(fjord, fRarePredatorSaltTreska, 0.70); setLFWeight(fjord, fUncommonPredatorSaltSaida, 0.55); setLFWeight(fjord, fRarePredatorSaltSeaForel, 0.35)
        setLFWeight(fjord, fLegendaryPredatorSaltPaltus, 0.06); setLFWeight(fjord, fEpicPredatorSaltLososAtlantic, 0.10)
        setLFWeight(fjord, fMythicPredatorSaltWolffish, 0.070)
        setLFWeight(fjord, fCommonPredatorSaltKoryushka, 0.12)
        setLFWeight(fjord, fEpicPeaceSaltTurbo, 0.08)
        setLFWeight(fjord, fCommonPeaceSaltMoiva, 0.20); setLFWeight(fjord, fCommonPeaceSaltKilka, 0.20); setLFWeight(fjord, fCommonPredatorSaltBychok, 0.10)

        // Открытый океан — косяки + мирные эпики
        setLFWeight(openOcean, fUncommonPredatorSaltAtlanticSkumbria, 0.80); setLFWeight(openOcean, fCommonPredatorSaltStavrida, 0.40)
        setLFWeight(openOcean, fRarePredatorSaltDorado, 0.60); setLFWeight(openOcean, fRarePredatorSaltWahoo, 0.50)
        setLFWeight(openOcean, fEpicPredatorSaltParusnik, 0.18); setLFWeight(openOcean, fEpicPredatorSaltSwordFish, 0.16); setLFWeight(openOcean, fEpicPredatorSaltAkulaMako, 0.10)
        setLFWeight(openOcean, fLegendaryPredatorSaltBlueMarlin, 0.05); setLFWeight(openOcean, fLegendaryPredatorSaltBlueTunec, 0.04); setLFWeight(openOcean, fRarePredatorSaltAlbakor, 0.12)
        setLFWeight(openOcean, fMythicPredatorSaltYellowTunec, 0.090)
        setLFWeight(openOcean, fCommonPeaceSaltSardina, 0.15); setLFWeight(openOcean, fCommonPeaceSaltAnchous, 0.12)
        setLFWeight(openOcean, fUncommonPeaceSaltSaira, 0.35); setLFWeight(openOcean, fUncommonPeaceSaltFlyFish, 0.25)
        setLFWeight(openOcean, fEpicPeaceSaltMoonFish, 0.06) // EPIC мирный
        setLFWeight(openOcean, fLegendaryPeaceSaltSeldyanoyKorol, 0.02)  // LEGENDARY мирный

        // --- Lures ---
        val presnMir = upsertLure("Пресная мирная", false, "fresh")
        upsertLure("Пресная хищная", true, "fresh")
        upsertLure("Морская мирная", false, "salt")
        upsertLure("Морская хищная", true, "salt")
        upsertLure("Пресная мирная+", false, "fresh", 0.3)
        upsertLure("Пресная хищная+", true, "fresh", 0.3)
        upsertLure("Морская мирная+", false, "salt", 0.3)
        upsertLure("Морская хищная+", true, "salt", 0.3)

        // default lure
        Users.update({ Users.currentLureId.isNull() }) { it[Users.currentLureId] = presnMir }
    }

    private fun migrateCoins() {
        val needsMigration = Catches
            .slice(Catches.id)
            .select { Catches.coins.isNull() }
            .limit(1)
            .map { it[Catches.id] }
            .isNotEmpty()

        if (!needsMigration) return

        val zone = ZoneId.systemDefault()
        val tierByLocation = Locations.selectAll()
            .map { it[Locations.id].value to it[Locations.unlockKg] }
            .sortedBy { it.second }
            .mapIndexed { index, (id, _) -> id to index }
            .toMap()

        val dailyTotals = mutableMapOf<Long, MutableMap<LocalDate, Long>>()
        val processedUsers = mutableSetOf<Long>()

        val catchesToMigrate = (Catches innerJoin Fish innerJoin Locations)
            .slice(
                Catches.id,
                Catches.userId,
                Catches.weight,
                Catches.locationId,
                Catches.createdAt,
                Fish.rarity,
                Fish.water,
            )
            .select { Catches.coins.isNull() }
            .orderBy(
                Catches.userId to SortOrder.ASC,
                Catches.createdAt to SortOrder.ASC,
                Catches.id to SortOrder.ASC,
            )

        catchesToMigrate.forEach { row ->
            val userId = row[Catches.userId].value
            val locationId = row[Catches.locationId].value
            val rarity = row[Fish.rarity]
            val water = row[Fish.water]
            val weight = row[Catches.weight]
            val createdAt = row[Catches.createdAt]
            val tier = tierByLocation[locationId] ?: 0
            val localDate = createdAt.atZone(zone).toLocalDate()
            val earnedBefore = dailyTotals
                .getOrPut(userId) { mutableMapOf() }
                .getOrDefault(localDate, 0L)
            val coinsAwarded = CoinCalculator.computeCoins(
                weight,
                rarity,
                tier,
                water,
                earnedBefore.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            )
            dailyTotals[userId]!![localDate] = earnedBefore + coinsAwarded
            processedUsers += userId
            Catches.update({ Catches.id eq row[Catches.id].value }) {
                it[Catches.coins] = coinsAwarded
            }
        }

        val sumExpr = Catches.coins.sum()
        if (sumExpr == null) {
            processedUsers.forEach { userId ->
                Users.update({ Users.id eq userId }) {
                    it[Users.coins] = 0L
                }
            }
            return
        }
        val totals = Catches
            .slice(Catches.userId, sumExpr)
            .select { Catches.coins.isNotNull() }
            .groupBy(Catches.userId)
            .associate { row ->
                val total = row[sumExpr] ?: 0
                row[Catches.userId].value to total.toLong()
            }

        totals.forEach { (userId, total) ->
            Users.update({ Users.id eq userId }) {
                it[Users.coins] = total
            }
        }

        processedUsers.filterNot { it in totals }.forEach { userId ->
            Users.update({ Users.id eq userId }) {
                it[Users.coins] = 0L
            }
        }
    }
}

// Table definitions

object Users : LongIdTable() {
    val tgId = long("tg_id").uniqueIndex()
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val username = varchar("username", 100).nullable()
    val nickname = varchar("nickname", 100).nullable()
    val language = varchar("language", 10).default("en")
    val level = integer("level")
    val xp = integer("xp")
    val coins = long("coins").default(0L)
    val createdAt = timestamp("created_at")
    val lastDailyAt = timestamp("last_daily_at").nullable()
    val dailyStreak = integer("daily_streak").default(0)
    val currentLocationId = reference("current_location_id", Locations).nullable()
    val currentLureId = reference("current_lure_id", Lures).nullable()
    val currentRodId = reference("current_rod_id", Rods).nullable()
    val castLureId = reference("cast_lure_id", Lures).nullable()
    val isCasting = bool("is_casting").default(false)
    val lastCastAt = timestamp("last_cast_at").nullable()
    val autoFishUntil = timestamp("auto_fish_until").nullable()
    val referredBy = reference("referred_by", Users).nullable()
}

object Locations : LongIdTable() {
    val name = varchar("name", 100)
    val unlockKg = double("unlock_kg").default(0.0)
    val sizeMultiplier = double("size_multiplier").default(1.0)
}

object Fish : LongIdTable() {
    val name = varchar("name", 100)
    val rarity = varchar("rarity", 50)
    val meanKg = double("mean_kg")
    val varKg = double("var_kg")
    val predator = bool("predator").default(false)
    val water = varchar("water", 20).default("fresh")
}

object Lures : LongIdTable() {
    val name = varchar("name", 100)
    val priceStars = integer("price_stars").nullable()
    val modsJson = text("mods_json")
    val predator = bool("predator").default(false)
    val water = varchar("water", 20).default("fresh")
    val rarityBonus = double("rarity_bonus").default(0.0)
}

object Rods : LongIdTable() {
    val code = varchar("code", 50).uniqueIndex()
    val name = varchar("name", 100)
    val priceStars = integer("price_stars").nullable()
    val modsJson = text("mods_json")
    val unlockKg = double("unlock_kg").default(0.0)
    val bonusWater = varchar("bonus_water", 20).nullable()
    val bonusPredator = bool("bonus_predator").nullable()
}

object InventoryLures : Table() {
    val userId = reference("user_id", Users)
    val lureId = reference("lure_id", Lures)
    val qty = integer("qty")
    override val primaryKey = PrimaryKey(userId, lureId)
}

object InventoryRods : Table() {
    val userId = reference("user_id", Users)
    val rodId = reference("rod_id", Rods)
    val qty = integer("qty")
    override val primaryKey = PrimaryKey(userId, rodId)
}

object Catches : LongIdTable() {
    val userId = reference("user_id", Users)
    val fishId = reference("fish_id", Fish)
    val weight = double("weight")
    val locationId = reference("location_id", Locations)
    val createdAt = timestamp("created_at")
    val coins = integer("coins").nullable()
}

object PendingCatches : Table() {
    val userId = reference("user_id", Users)
    val fishId = reference("fish_id", Fish)
    val weight = double("weight")
    val locationId = reference("location_id", Locations)
    val lureId = reference("lure_id", Lures)
    val waitSeconds = integer("wait_seconds")
    val reactionTime = double("reaction_time")
    val autoCatch = bool("auto_catch")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(userId)
}

object LocationFishWeights : Table() {
    val locationId = reference("location_id", Locations)
    val fishId = reference("fish_id", Fish)
    val weight = double("weight")
    override val primaryKey = PrimaryKey(locationId, fishId)
}

object Payments : LongIdTable() {
    val userId = reference("user_id", Users)
    val packageId = varchar("package_id", 100)
    val providerChargeId = varchar("provider_charge_id", 255).nullable()
    val telegramChargeId = varchar("telegram_charge_id", 255)
    val amount = integer("amount").default(0)
    val currency = varchar("currency", 16).default("XTR")
    val refunded = bool("refunded").default(false)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

object PaySupportRequests : LongIdTable() {
    val userId = reference("user_id", Users)
    val paymentId = reference("payment_id", Payments).nullable()
    val reason = text("reason")
    val status = varchar("status", 20)
    val adminMessage = text("admin_message").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

object Tournaments : LongIdTable() {
    val nameRu = varchar("name_ru", 100)
    val nameEn = varchar("name_en", 100)
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val fish = varchar("fish", 100).nullable()
    val location = varchar("location", 100).nullable()
    val metric = varchar("metric", 20)
    val prizePlaces = integer("prize_places")
    val prizesJson = text("prizes_json")
}

object UserPrizes : LongIdTable() {
    val userId = reference("user_id", Users)
    val tournamentId = reference("tournament_id", Tournaments)
    val packageId = varchar("package_id", 100)
    val qty = integer("qty").default(1)
    val claimed = bool("claimed").default(false)
}

object ReferralLinks : LongIdTable() {
    val userId = reference("user_id", Users)
    val token = varchar("token", 100).uniqueIndex()
    val createdAt = timestamp("created_at")
}

object ReferralRewards : LongIdTable() {
    val userId = reference("user_id", Users)
    val lureId = reference("lure_id", Lures).nullable()
    val packageId = varchar("package_id", 100).nullable()
    val qty = integer("qty").default(1)
    val claimed = bool("claimed").default(false)
}

object ShopDiscounts : LongIdTable() {
    val packageId = varchar("package_id", 100).uniqueIndex()
    val price = integer("price")
    val startDate = date("start_date")
    val endDate = date("end_date")
}
