package db

import app.Env
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

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

        // NEW tropical & Amazonia
        val amazon      = upsertLocation("Русло Амазонки",          900.0,  3.0)
        val igapo       = upsertLocation("Игапо, затопленный лес",  1100.0, 3.05)
        val mangrove    = upsertLocation("Мангровые заросли",       1500.0, 3.1)
        val coralFlats  = upsertLocation("Коралловые отмели",       2000.0, 3.2)

        val fjord       = upsertLocation("Фьорд",           2600.0, 3.4)
        val openOcean   = upsertLocation("Открытый океан",  5000.0, 4.2)

        // --- Fish: базовые пресные ---
        val fP  = upsertFish("Плотва", "common", 0.2, 0.05, false, "fresh")
        val fO  = upsertFish("Окунь", "common", 0.25, 0.07, true, "fresh")
        val fK  = upsertFish("Карась", "common", 0.3, 0.1, false, "fresh")
        val fL  = upsertFish("Лещ", "uncommon", 0.8, 0.2, false, "fresh")
        val fSh = upsertFish("Щука", "rare", 3.0, 1.2, true, "fresh")
        val fKa = upsertFish("Карп", "rare", 2.5, 1.0, false, "fresh")
        val fSo = upsertFish("Сом", "epic", 8.0, 4.0, true, "fresh")
        val fOs = upsertFish("Осётр", "legendary", 12.0, 6.0, false, "fresh")

        // пресные продвинутые
        val fUk  = upsertFish("Уклейка", "common", 0.05, 0.02, false, "fresh")
        val fLi  = upsertFish("Линь", "uncommon", 0.7, 0.3, false, "fresh")
        val fRo  = upsertFish("Ротан", "common", 0.15, 0.05, true, "fresh")
        val fZu  = upsertFish("Судак", "rare", 2.0, 1.0, true, "fresh")
        val fCh  = upsertFish("Чехонь", "uncommon", 0.4, 0.15, false, "fresh")
        val fHa  = upsertFish("Хариус", "rare", 0.6, 0.25, true, "fresh")
        val fFr  = upsertFish("Форель ручьевая", "rare", 1.2, 0.6, true, "fresh")
        val fTa  = upsertFish("Таймень", "legendary", 15.0, 7.0, true, "fresh")
        val fNa  = upsertFish("Налим", "uncommon", 1.5, 0.8, true, "fresh")
        val fSi  = upsertFish("Сиг", "uncommon", 1.2, 0.5, false, "fresh")
        val fGo  = upsertFish("Голавль", "uncommon", 0.8, 0.4, true, "fresh")
        val fJe  = upsertFish("Жерех", "rare", 2.0, 1.0, true, "fresh")
        val fTo  = upsertFish("Толстолобик", "rare", 4.0, 2.0, false, "fresh")
        val fGa  = upsertFish("Белый амур", "rare", 3.5, 1.5, false, "fresh")
        val fEel = upsertFish("Угорь европейский", "epic", 1.5, 0.7, true, "fresh")
        val fSter= upsertFish("Стерлядь", "epic", 3.0, 1.2, false, "fresh")

        // пресная «простая»
        val fEr  = upsertFish("Ёрш", "common", 0.08, 0.03, true, "fresh")
        val fPe  = upsertFish("Пескарь", "common", 0.07, 0.03, false, "fresh")
        val fGu2 = upsertFish("Густера", "common", 0.35, 0.12, false, "fresh")
        val fKr2 = upsertFish("Краснопёрка", "common", 0.15, 0.05, false, "fresh")
        val fEl2 = upsertFish("Елец", "common", 0.12, 0.05, false, "fresh")
        val fVh  = upsertFish("Верхоплавка", "common", 0.01, 0.005, false, "fresh")
        val fYa2 = upsertFish("Язь", "uncommon", 1.20, 0.50, true, "fresh")
        val fGly = upsertFish("Гольян", "common", 0.02, 0.01, false, "fresh")

        // морские/солоноватые (шельф/фьорд/прибрежье)
        val fMu  = upsertFish("Кефаль", "uncommon", 1.0, 0.5, false, "salt")
        val fFl  = upsertFish("Камбала", "uncommon", 0.8, 0.4, false, "salt") // мирная
        val fHe  = upsertFish("Сельдь", "common", 0.3, 0.1, false, "salt")
        val fSt  = upsertFish("Ставрида", "common", 0.25, 0.1, true, "salt")
        val fCo  = upsertFish("Треска", "rare", 3.0, 1.5, true, "salt")
        val fSa  = upsertFish("Сайда", "uncommon", 2.0, 1.0, true, "salt")
        val fSe  = upsertFish("Морская форель", "rare", 1.5, 0.7, true, "salt")
        val fHa2 = upsertFish("Палтус", "legendary", 20.0, 10.0, true, "salt")
        val fSm  = upsertFish("Корюшка", "common", 0.06, 0.02, true, "salt")
        val fSal = upsertFish("Лосось атлантический", "epic", 6.0, 3.0, true, "salt")
        val fBas = upsertFish("Лаврак", "rare", 2.0, 1.0, true, "salt")
        val fMac = upsertFish("Скумбрия атлантическая", "uncommon", 0.6, 0.25, true, "salt")
        val fBel = upsertFish("Белуга", "legendary", 40.0, 20.0, true, "salt")

        // морская «мелочь»
        val fBy2 = upsertFish("Бычок", "common", 0.12, 0.06, true, "salt")
        val fKi2 = upsertFish("Килька", "common", 0.03, 0.01, false, "salt")
        val fMo2 = upsertFish("Мойва", "common", 0.04, 0.015, false, "salt")
        val fSar = upsertFish("Сардина", "common", 0.12, 0.05, false, "salt")
        val fAnc = upsertFish("Анчоус", "common", 0.02, 0.01, false, "salt")

        // новые мирные/океанические
        val fHad = upsertFish("Пикша", "uncommon", 2.0, 1.0, false, "salt")
        val fTur = upsertFish("Тюрбо", "epic", 6.0, 3.0, false, "salt")
        val fSra = upsertFish("Сайра", "uncommon", 0.3, 0.15, false, "salt")
        val fFly = upsertFish("Летучая рыба", "uncommon", 0.4, 0.2, false, "salt")
        val fMola = upsertFish("Рыба-луна", "epic", 220.0, 90.0, false, "salt")
        val fOar = upsertFish("Сельдяной король", "legendary", 120.0, 50.0, false, "salt")

        // открытый океан — хищные
        val fDor = upsertFish("Дорадо", "rare", 6.0, 3.0, true, "salt")
        val fWah = upsertFish("Ваху", "rare", 10.0, 5.0, true, "salt")
        val fSail = upsertFish("Парусник", "epic", 30.0, 12.0, true, "salt")
        val fSwf = upsertFish("Рыба-меч", "epic", 55.0, 25.0, true, "salt")
        val fBmr = upsertFish("Марлин синий", "legendary", 180.0, 80.0, true, "salt")
        val fTbf = upsertFish("Тунец синеперый", "legendary", 200.0, 90.0, true, "salt")
        val fAlb = upsertFish("Альбакор", "rare", 18.0, 7.0, true, "salt")
        val fMak = upsertFish("Акула мако", "epic", 70.0, 30.0, true, "salt")

        // Эпики для Горной реки
        val fArc = upsertFish("Голец арктический", "epic", 2.8, 1.2, true, "fresh")
        val fKum = upsertFish("Форель кумжа", "epic", 4.0, 1.5, true, "fresh")

        // === NEW: Amazonia & Tropics (uni-name RU, no parentheses) ===
        // Amazon Riverbed — мирные
        val fTamb = upsertFish("Тамбаки", "epic", 12.0, 6.0, false, "fresh")
        val fPacuB = upsertFish("Паку чёрный", "uncommon", 6.0, 3.0, false, "fresh")
        val fProch = upsertFish("Прохилодус", "common", 1.5, 0.7, false, "fresh")
        val fAncF = upsertFish("Анциструс", "uncommon", 0.10, 0.05, false, "fresh")
        val fOtoF = upsertFish("Отоцинклюс", "common", 0.01, 0.005, false, "fresh")
        val fNeon = upsertFish("Неоновая тетра", "common", 0.003, 0.0015, false, "fresh")
        val fTern = upsertFish("Тернеция", "uncommon", 0.02, 0.01, false, "fresh")

        // Amazon Riverbed — хищные
        val fArap = upsertFish("Арапайма", "legendary", 80.0, 30.0, true, "fresh")
        val fPirRB = upsertFish("Пиранья краснобрюхая", "uncommon", 0.8, 0.4, true, "fresh")
        val fTraira = upsertFish("Трайра", "rare", 2.5, 1.0, true, "fresh")
        val fAcestr = upsertFish("Ацестринх", "rare", 1.0, 0.5, true, "fresh")
        val fEEel = upsertFish("Электрический угорь", "epic", 15.0, 5.0, true, "fresh")
        val fRTC = upsertFish("Краснохвостый сом", "epic", 30.0, 12.0, true, "fresh")
        val fPiraiba = upsertFish("Пираиба", "legendary", 70.0, 35.0, true, "fresh")

        // Igapo — мирные
        val fDiscus = upsertFish("Дискус", "epic", 0.20, 0.10, false, "fresh")
        val fScalar = upsertFish("Скалярия", "uncommon", 0.15, 0.08, false, "fresh")
        val fAgass  = upsertFish("Апистограмма Агассиза", "uncommon", 0.05, 0.02, false, "fresh")
        val fCard   = upsertFish("Кардинальная тетра", "common", 0.003, 0.0015, false, "fresh")
        val fPanda  = upsertFish("Коридорас панда", "uncommon", 0.05, 0.02, false, "fresh")
        val fNanno  = upsertFish("Нанностомус", "common", 0.004, 0.002, false, "fresh")
        val fRam    = upsertFish("Рамирези", "rare", 0.05, 0.02, false, "fresh")

        // Igapo — хищные
        val fBArow  = upsertFish("Аравана чёрная", "legendary", 6.0, 3.0, true, "fresh")
        val fOscar  = upsertFish("Оскар", "rare", 1.5, 0.7, true, "fresh")
        val fAimara = upsertFish("Аймара", "epic", 8.0, 4.0, true, "fresh")
        val fPSur   = upsertFish("Псевдоплатистома тигровая", "epic", 10.0, 5.0, true, "fresh")
        val fPirBlack = upsertFish("Пиранья чёрная", "rare", 1.3, 0.6, true, "fresh")
        val fPikeCic = upsertFish("Щучья цихлида", "rare", 0.9, 0.4, true, "fresh")
        val fPeacock = upsertFish("Павлиний окунь", "epic", 4.0, 2.0, true, "fresh")

        // Mangrove Estuary
        val fMilk     = upsertFish("Молочная рыба", "epic", 3.5, 1.5, false, "salt")
        val fMulSpot  = upsertFish("Пятнистая кефаль", "common", 0.8, 0.4, false, "salt")
        val fTilMoz   = upsertFish("Тиляпия мозамбикская", "uncommon", 1.0, 0.5, false, "salt")
        val fAnchTrop = upsertFish("Анчоус тропический", "common", 0.02, 0.01, false, "salt")
        val fSardInd  = upsertFish("Сардина индийская", "common", 0.10, 0.05, false, "salt")
        val fRabbit   = upsertFish("Золотистый сиган", "rare", 0.5, 0.2, false, "salt")
        val fGobyMang = upsertFish("Бычок мангровый", "uncommon", 0.07, 0.03, true, "salt")

        val fBarra    = upsertFish("Баррамунди", "epic", 10.0, 5.0, true, "salt")
        val fSnook    = upsertFish("Снук", "rare", 4.0, 2.0, true, "salt")
        val fSnapMang = upsertFish("Мангровый луциан", "rare", 2.0, 1.0, true, "salt")
        val fTarpon   = upsertFish("Тарпон", "legendary", 25.0, 12.0, true, "salt")
        val fSeaCat   = upsertFish("Морской сом", "uncommon", 1.2, 0.6, true, "salt")
        val fNeedle   = upsertFish("Морской сарган", "uncommon", 0.7, 0.3, true, "salt")
        val fTreBlue  = upsertFish("Голубой каранкс", "epic", 5.0, 2.0, true, "salt")

        // Coral Flats
        val fParrot     = upsertFish("Рыба-попугай", "uncommon", 2.0, 1.0, false, "salt")
        val fEmperor    = upsertFish("Императорский ангел", "epic", 0.5, 0.2, false, "salt")
        val fBlueTang   = upsertFish("Голубой хирург", "rare", 0.4, 0.15, false, "salt")
        val fButterThrd = upsertFish("Нитеносная бабочка", "uncommon", 0.2, 0.08, false, "salt")
        val fDamselBlue = upsertFish("Синяя хризиптера", "common", 0.04, 0.02, false, "salt")
        val fFusYellow  = upsertFish("Фузилёр жёлтохвостый", "common", 0.3, 0.1, false, "salt")
        val fGoatTrop   = upsertFish("Барабулька тропическая", "uncommon", 0.5, 0.2, false, "salt")

        val fBarraBig = upsertFish("Большая барракуда", "epic", 12.0, 6.0, true, "salt")
        val fTreGiant = upsertFish("Гигантский каранкс", "legendary", 25.0, 10.0, true, "salt")
        val fPermit   = upsertFish("Пермит", "epic", 6.0, 3.0, true, "salt")
        val fAlbula   = upsertFish("Альбула", "rare", 3.0, 1.0, true, "salt")
        val fSpanMack = upsertFish("Испанская скумбрия", "rare", 4.0, 2.0, true, "salt")
        val fCoralTrt = upsertFish("Коралловая форель", "epic", 5.0, 2.0, true, "salt")
        val fTitanTrig= upsertFish("Спинорог-титан", "rare", 3.0, 1.0, true, "salt")

        // --- Rods ---
        upsertRod("spark", "Искра", 0.0, null, null)
        upsertRod("dew",   "Роса",  15.0, "fresh", false)
        upsertRod("stream","Поток", 150.0, "fresh", true)
        upsertRod("abyss", "Глубь", 450.0, "salt",  false)
        upsertRod("storm", "Шторм", 1000.0, "salt", true)

        // --- Weights per location ---

        // Пруд
        setLFWeight(pond, fP, 1.0); setLFWeight(pond, fO, 0.9); setLFWeight(pond, fK, 1.1)
        setLFWeight(pond, fL, 0.4); setLFWeight(pond, fKa, 0.15)
        setLFWeight(pond, fUk, 0.7); setLFWeight(pond, fLi, 0.2); setLFWeight(pond, fRo, 0.6); setLFWeight(pond, fSh, 0.12)
        setLFWeight(pond, fEel, 0.06); setLFWeight(pond, fSo, 0.03)
        setLFWeight(pond, fPe, 0.7); setLFWeight(pond, fEr, 0.6); setLFWeight(pond, fGu2, 0.5); setLFWeight(pond, fKr2, 0.7); setLFWeight(pond, fVh, 0.5)

        // Река
        setLFWeight(river, fP, 0.7); setLFWeight(river, fO, 0.7); setLFWeight(river, fL, 0.6)
        setLFWeight(river, fSh, 0.25); setLFWeight(river, fSo, 0.10); setLFWeight(river, fGo, 0.5)
        setLFWeight(river, fJe, 0.3); setLFWeight(river, fZu, 0.4); setLFWeight(river, fNa, 0.2)
        setLFWeight(river, fKa, 0.15); setLFWeight(river, fEel, 0.10); setLFWeight(river, fSter, 0.05); setLFWeight(river, fOs, 0.02)
        setLFWeight(river, fPe, 0.6); setLFWeight(river, fEr, 0.5); setLFWeight(river, fEl2, 0.4); setLFWeight(river, fKr2, 0.5); setLFWeight(river, fYa2, 0.25)

        // Озеро
        setLFWeight(lake, fP, 0.6); setLFWeight(lake, fK, 0.9); setLFWeight(lake, fL, 0.5)
        setLFWeight(lake, fSh, 0.35); setLFWeight(lake, fKa, 0.3); setLFWeight(lake, fOs, 0.05)
        setLFWeight(lake, fSi, 0.35); setLFWeight(lake, fTo, 0.25); setLFWeight(lake, fGa, 0.2)
        setLFWeight(lake, fSo, 0.12); setLFWeight(lake, fEel, 0.08)
        setLFWeight(lake, fGu2, 0.6); setLFWeight(lake, fKr2, 0.6); setLFWeight(lake, fEr, 0.4); setLFWeight(lake, fPe, 0.5); setLFWeight(lake, fEl2, 0.3); setLFWeight(lake, fYa2, 0.2)

        // Болото
        setLFWeight(swamp, fK, 1.3); setLFWeight(swamp, fLi, 1.0); setLFWeight(swamp, fRo, 0.9)
        setLFWeight(swamp, fP, 0.5); setLFWeight(swamp, fO, 0.4); setLFWeight(swamp, fSh, 0.25); setLFWeight(swamp, fKa, 0.3)
        setLFWeight(swamp, fEel, 0.15); setLFWeight(swamp, fSo, 0.05)
        setLFWeight(swamp, fGu2, 0.7); setLFWeight(swamp, fKr2, 0.8); setLFWeight(swamp, fEr, 0.4); setLFWeight(swamp, fPe, 0.4); setLFWeight(swamp, fVh, 0.5)

        // Горная река (с эпиками)
        setLFWeight(mtnRiver, fHa, 0.9); setLFWeight(mtnRiver, fFr, 0.8); setLFWeight(mtnRiver, fNa, 0.4); setLFWeight(mtnRiver, fGo, 0.3)
        setLFWeight(mtnRiver, fArc, 0.06); setLFWeight(mtnRiver, fKum, 0.06) // EPIC
        setLFWeight(mtnRiver, fTa, 0.05) // легенда
        setLFWeight(mtnRiver, fEl2, 0.35); setLFWeight(mtnRiver, fPe, 0.35); setLFWeight(mtnRiver, fGly, 0.45); setLFWeight(mtnRiver, fSi, 0.10)

        // Водохранилище
        setLFWeight(reservoir, fZu, 0.9); setLFWeight(reservoir, fJe, 0.6); setLFWeight(reservoir, fL, 0.7)
        setLFWeight(reservoir, fKa, 0.5); setLFWeight(reservoir, fTo, 0.5); setLFWeight(reservoir, fSo, 0.22)
        setLFWeight(reservoir, fP, 0.4); setLFWeight(reservoir, fO, 0.4); setLFWeight(reservoir, fGa, 0.3)
        setLFWeight(reservoir, fSter, 0.03); setLFWeight(reservoir, fOs, 0.01)
        setLFWeight(reservoir, fGu2, 0.4); setLFWeight(reservoir, fEr, 0.3); setLFWeight(reservoir, fPe, 0.35); setLFWeight(reservoir, fYa2, 0.35)

        // Дельта реки (солоноватая)
        setLFWeight(delta, fCh, 0.7); setLFWeight(delta, fZu, 0.6); setLFWeight(delta, fL, 0.5)
        setLFWeight(delta, fEel, 0.10); setLFWeight(delta, fOs, 0.05)
        setLFWeight(delta, fMu, 0.45); setLFWeight(delta, fHe, 0.35); setLFWeight(delta, fSm, 0.25)
        setLFWeight(delta, fSar, 0.25); setLFWeight(delta, fAnc, 0.20)
        setLFWeight(delta, fBas, 0.18); setLFWeight(delta, fSal, 0.12); setLFWeight(delta, fBel, 0.01)
        setLFWeight(delta, fBy2, 0.50); setLFWeight(delta, fKi2, 0.25); setLFWeight(delta, fMo2, 0.20)
        setLFWeight(delta, fKr2, 0.35); setLFWeight(delta, fPe, 0.25)
        // cross-seed (новые)
        setLFWeight(delta, fNeedle, 0.20); setLFWeight(delta, fMulSpot, 0.30); setLFWeight(delta, fSardInd, 0.20)

        // Прибрежье моря
        setLFWeight(coast, fMu, 0.9); setLFWeight(coast, fHe, 0.9)
        setLFWeight(coast, fFl, 0.6); setLFWeight(coast, fSt, 0.6); setLFWeight(coast, fSe, 0.3)
        setLFWeight(coast, fSar, 0.6); setLFWeight(coast, fAnc, 0.5)
        setLFWeight(coast, fMac, 0.6); setLFWeight(coast, fBas, 0.25); setLFWeight(coast, fSal, 0.08)
        setLFWeight(coast, fBy2, 0.35); setLFWeight(coast, fKi2, 0.30); setLFWeight(coast, fMo2, 0.25); setLFWeight(coast, fSm, 0.10)
        setLFWeight(coast, fDor, 0.03); setLFWeight(coast, fWah, 0.02); setLFWeight(coast, fSail, 0.01)
        setLFWeight(coast, fBel, 0.01)
        setLFWeight(coast, fTur, 0.02) // редкий мирный трофей
        // cross-seed (новые)
        setLFWeight(coast, fNeedle, 0.30)
        setLFWeight(coast, fTreBlue, 0.05)
        setLFWeight(coast, fBarraBig, 0.03)
        setLFWeight(coast, fSpanMack, 0.15)
        setLFWeight(coast, fGoatTrop, 0.20)
        setLFWeight(coast, fTarpon, 0.01)

        // Русло Амазонки
        setLFWeight(amazon, fTamb, 0.06); setLFWeight(amazon, fPacuB, 0.40); setLFWeight(amazon, fProch, 0.85)
        setLFWeight(amazon, fAncF, 0.60); setLFWeight(amazon, fOtoF, 0.70); setLFWeight(amazon, fNeon, 0.85); setLFWeight(amazon, fTern, 0.50)
        setLFWeight(amazon, fArap, 0.03); setLFWeight(amazon, fPirRB, 0.60); setLFWeight(amazon, fTraira, 0.25)
        setLFWeight(amazon, fAcestr, 0.20); setLFWeight(amazon, fEEel, 0.05); setLFWeight(amazon, fRTC, 0.05); setLFWeight(amazon, fPiraiba, 0.02)

        // Игапо, затопленный лес
        setLFWeight(igapo, fDiscus, 0.05); setLFWeight(igapo, fScalar, 0.60); setLFWeight(igapo, fAgass, 0.60)
        setLFWeight(igapo, fCard, 0.85); setLFWeight(igapo, fPanda, 0.45); setLFWeight(igapo, fNanno, 0.85); setLFWeight(igapo, fRam, 0.20)
        setLFWeight(igapo, fBArow, 0.03); setLFWeight(igapo, fOscar, 0.25); setLFWeight(igapo, fAimara, 0.06)
        setLFWeight(igapo, fPSur, 0.05); setLFWeight(igapo, fPirBlack, 0.18); setLFWeight(igapo, fPikeCic, 0.20); setLFWeight(igapo, fPeacock, 0.06)
        // пересечения с Руслом
        setLFWeight(igapo, fTamb, 0.02); setLFWeight(igapo, fPacuB, 0.12); setLFWeight(igapo, fEEel, 0.02)

        // Мангровые заросли
        setLFWeight(mangrove, fMilk, 0.06); setLFWeight(mangrove, fMulSpot, 0.80); setLFWeight(mangrove, fTilMoz, 0.50)
        setLFWeight(mangrove, fAnchTrop, 0.90); setLFWeight(mangrove, fSardInd, 0.80); setLFWeight(mangrove, fRabbit, 0.20); setLFWeight(mangrove, fGobyMang, 0.60)
        setLFWeight(mangrove, fBarra, 0.08); setLFWeight(mangrove, fSnook, 0.25); setLFWeight(mangrove, fSnapMang, 0.25)
        setLFWeight(mangrove, fTarpon, 0.03); setLFWeight(mangrove, fSeaCat, 0.50); setLFWeight(mangrove, fNeedle, 0.40); setLFWeight(mangrove, fTreBlue, 0.06)

        // Коралловые отмели
        setLFWeight(coralFlats, fParrot, 0.55); setLFWeight(coralFlats, fEmperor, 0.08); setLFWeight(coralFlats, fBlueTang, 0.25)
        setLFWeight(coralFlats, fButterThrd, 0.50); setLFWeight(coralFlats, fDamselBlue, 0.90); setLFWeight(coralFlats, fFusYellow, 0.85); setLFWeight(coralFlats, fGoatTrop, 0.50)
        setLFWeight(coralFlats, fBarraBig, 0.06); setLFWeight(coralFlats, fTreGiant, 0.03); setLFWeight(coralFlats, fPermit, 0.08)
        setLFWeight(coralFlats, fAlbula, 0.25); setLFWeight(coralFlats, fSpanMack, 0.25); setLFWeight(coralFlats, fCoralTrt, 0.06); setLFWeight(coralFlats, fTitanTrig, 0.22)

        // Фьорд — мирные и хищные
        setLFWeight(fjord, fFl, 0.60)
        setLFWeight(fjord, fHad, 0.50)
        setLFWeight(fjord, fHe, 0.60); setLFWeight(fjord, fSar, 0.12); setLFWeight(fjord, fAnc, 0.08)
        setLFWeight(fjord, fCo, 0.70); setLFWeight(fjord, fSa, 0.55); setLFWeight(fjord, fSe, 0.35)
        setLFWeight(fjord, fHa2, 0.06); setLFWeight(fjord, fSal, 0.10)
        setLFWeight(fjord, fSm, 0.12)
        setLFWeight(fjord, fTur, 0.08)
        setLFWeight(fjord, fMo2, 0.20); setLFWeight(fjord, fKi2, 0.20); setLFWeight(fjord, fBy2, 0.10)

        // Открытый океан — косяки + мирные эпики
        setLFWeight(openOcean, fMac, 0.80); setLFWeight(openOcean, fSt, 0.40)
        setLFWeight(openOcean, fDor, 0.60); setLFWeight(openOcean, fWah, 0.50)
        setLFWeight(openOcean, fSail, 0.18); setLFWeight(openOcean, fSwf, 0.16); setLFWeight(openOcean, fMak, 0.10)
        setLFWeight(openOcean, fBmr, 0.05); setLFWeight(openOcean, fTbf, 0.04); setLFWeight(openOcean, fAlb, 0.12)
        setLFWeight(openOcean, fSar, 0.15); setLFWeight(openOcean, fAnc, 0.12)
        setLFWeight(openOcean, fSra, 0.35); setLFWeight(openOcean, fFly, 0.25)
        setLFWeight(openOcean, fMola, 0.06) // EPIC мирный
        setLFWeight(openOcean, fOar, 0.02)  // LEGENDARY мирный

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
