package db

import app.Env
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object DB {
    fun init(env: Env) {
        // SQLite single-file DB. Path from env.DATABASE_URL, e.g. jdbc:sqlite:/data/riverking.db
        Database.connect(url = env.dbUrl, driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users, Locations, Fish, Lures, Rods, InventoryLures, InventoryRods, Catches, LocationFishWeights
            )
            seedIfEmpty()
        }
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
                    it[name]   = n
                    it[rarity] = r
                    it[meanKg] = mean
                    it[varKg]  = vari
                    it[Fish.predator] = pred
                    it[Fish.water] = water
                }.value
            } else {
                val id = row[Fish.id].value
                Fish.update({ Fish.id eq id }) {
                    it[rarity] = r
                    it[meanKg] = mean
                    it[varKg]  = vari
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
                    it[locationId] = loc
                    it[fishId]     = fish
                    it[weight]     = w
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

        // --- Locations (базовые + новые) ---
        val pond  = upsertLocation("Пруд",  0.0,   1.0)
        val river = upsertLocation("Река", 10.0,   1.5)
        val lake  = upsertLocation("Озеро", 50.0,  2.0)

        val swamp      = upsertLocation("Болото",          5.0,   1.2)
        val mtnRiver   = upsertLocation("Горная река",     120.0, 2.2)
        val reservoir  = upsertLocation("Водохранилище",   200.0, 2.4)
        val delta      = upsertLocation("Дельта реки",     350.0, 2.6)
        val coast      = upsertLocation("Прибрежье моря",  600.0, 3.0)
        val fjord      = upsertLocation("Фьорд",           900.0, 3.5)

        // --- Fish (исправленные флаги + новые эпики/легендарки + простые виды) ---
        // Пресные (базовые)
        val fP  = upsertFish("Плотва",           "common",     0.2,  0.05, false, "fresh")
        val fO  = upsertFish("Окунь",            "common",     0.25, 0.07, true,  "fresh")
        val fK  = upsertFish("Карась",           "common",     0.3,  0.1,  false, "fresh")
        val fL  = upsertFish("Лещ",              "uncommon",   0.8,  0.2,  false, "fresh")
        val fSh = upsertFish("Щука",             "rare",       3.0,  1.2,  true,  "fresh")
        val fKa = upsertFish("Карп",             "rare",       2.5,  1.0,  false, "fresh")
        val fSo = upsertFish("Сом",              "epic",       8.0,  4.0,  true,  "fresh")
        val fOs = upsertFish("Осётр",            "legendary", 12.0,  6.0,  false, "fresh") // бентофаг

        // Пресные (продвинутые)
        val fUk = upsertFish("Уклейка",          "common",     0.05, 0.02, false, "fresh")
        val fLi = upsertFish("Линь",             "uncommon",   0.7,  0.3,  false, "fresh")
        val fRo = upsertFish("Ротан",            "common",     0.15, 0.05, true,  "fresh")
        val fZu = upsertFish("Судак",            "rare",       2.0,  1.0,  true,  "fresh")
        val fCh = upsertFish("Чехонь",           "uncommon",   0.4,  0.15, false, "fresh")
        val fHa = upsertFish("Хариус",           "rare",       0.6,  0.25, true,  "fresh")
        val fFr = upsertFish("Форель ручьевая",  "rare",       1.2,  0.6,  true,  "fresh")
        val fTa = upsertFish("Таймень",          "legendary", 15.0,  7.0,  true,  "fresh")
        val fNa = upsertFish("Налим",            "uncommon",   1.5,  0.8,  true,  "fresh")
        val fSi = upsertFish("Сиг",              "uncommon",   1.2,  0.5,  false, "fresh")
        val fGo = upsertFish("Голавль",          "uncommon",   0.8,  0.4,  true,  "fresh")
        val fJe = upsertFish("Жерех",            "rare",       2.0,  1.0,  true,  "fresh")
        val fTo = upsertFish("Толстолобик",      "rare",       4.0,  2.0,  false, "fresh")
        val fGa = upsertFish("Белый амур",       "rare",       3.5,  1.5,  false, "fresh")
        val fEel= upsertFish("Угорь европейский","epic",       1.5,  0.7,  true,  "fresh")
        val fSter=upsertFish("Стерлядь",         "epic",       3.0,  1.2,  false, "fresh")

        // Морские/солоноватые
        val fMu = upsertFish("Кефаль",           "uncommon",   1.0,  0.5,  false, "salt")
        val fFl = upsertFish("Камбала",          "uncommon",   0.8,  0.4,  true,  "salt")
        val fHe = upsertFish("Сельдь",           "common",     0.3,  0.1,  false, "salt")
        val fSt = upsertFish("Ставрида",         "common",     0.25, 0.1,  true,  "salt")
        val fCo = upsertFish("Треска",           "rare",       3.0,  1.5,  true,  "salt")
        val fSa = upsertFish("Сайда",            "uncommon",   2.0,  1.0,  true,  "salt")
        val fSe = upsertFish("Морская форель",   "rare",       1.5,  0.7,  true,  "salt")
        val fHa2= upsertFish("Палтус",           "legendary", 20.0, 10.0, true,  "salt")
        val fSm = upsertFish("Корюшка",          "common",     0.06, 0.02, true,  "salt") // используется в дельте/фьорде
        val fSal= upsertFish("Лосось атлантический","epic",    6.0,  3.0,  true,  "salt")
        val fBas= upsertFish("Лаврак",           "rare",       2.0,  1.0,  true,  "salt")
        val fMac= upsertFish("Скумбрия атлантическая","uncommon",0.6,0.25, true,  "salt")
        val fBel= upsertFish("Белуга",           "legendary", 40.0, 20.0, true,  "salt")

        // Пресные «простые» виды
        val fEr  = upsertFish("Ёрш",           "common",   0.08, 0.03, true,  "fresh")
        val fPe  = upsertFish("Пескарь",       "common",   0.07, 0.03, false, "fresh")
        val fGu2 = upsertFish("Густера",       "common",   0.35, 0.12, false, "fresh")
        val fKr2 = upsertFish("Краснопёрка",   "common",   0.15, 0.05, false, "fresh")
        val fEl2 = upsertFish("Елец",          "common",   0.12, 0.05, false, "fresh")
        val fVh  = upsertFish("Верхоплавка",   "common",   0.01, 0.005,false, "fresh")
        val fYa2 = upsertFish("Язь",           "uncommon", 1.20, 0.50, true,  "fresh")

        // Морская/солоноватая «мелочь»
        val fBy2 = upsertFish("Бычок",         "common",   0.12, 0.06, true,  "salt")
        val fKi2 = upsertFish("Килька",        "common",   0.03, 0.01, false, "salt")
        val fMo2 = upsertFish("Мойва",         "common",   0.04, 0.015,false, "salt")

        // --- Weights per location (вероятности спавна относительно друг друга) ---

        // Пруд — частый клёв + редкий эпик
        setLFWeight(pond,  fP,   1.0)
        setLFWeight(pond,  fO,   0.9)
        setLFWeight(pond,  fK,   1.1)
        setLFWeight(pond,  fL,   0.4)
        setLFWeight(pond,  fKa,  0.15)
        setLFWeight(pond,  fUk,  0.7)
        setLFWeight(pond,  fLi,  0.2)
        setLFWeight(pond,  fRo,  0.6)
        setLFWeight(pond,  fSh,  0.12)
        setLFWeight(pond,  fEel, 0.06) // epic
        setLFWeight(pond,  fSo,  0.03) // epic редкий

        // простые для пруда
        setLFWeight(pond,  fPe,  0.7)
        setLFWeight(pond,  fEr,  0.6)
        setLFWeight(pond,  fGu2, 0.5)
        setLFWeight(pond,  fKr2, 0.7)
        setLFWeight(pond,  fVh,  0.5)

        // Река — есть эпики и легендарка
        setLFWeight(river, fP,   0.7)
        setLFWeight(river, fO,   0.7)
        setLFWeight(river, fL,   0.6)
        setLFWeight(river, fSh,  0.25)
        setLFWeight(river, fSo,  0.10)
        setLFWeight(river, fGo,  0.5)
        setLFWeight(river, fJe,  0.3)
        setLFWeight(river, fZu,  0.4)
        setLFWeight(river, fNa,  0.2)
        setLFWeight(river, fKa,  0.15)
        setLFWeight(river, fEel, 0.10) // epic
        setLFWeight(river, fSter,0.05) // epic
        setLFWeight(river, fOs,  0.02) // legendary

        // простые для реки
        setLFWeight(river, fPe,  0.6)
        setLFWeight(river, fEr,  0.5)
        setLFWeight(river, fEl2, 0.4)
        setLFWeight(river, fKr2, 0.5)
        setLFWeight(river, fYa2, 0.25)

        // Озеро — эпики/легендарка + белая рыба
        setLFWeight(lake,  fP,   0.6)
        setLFWeight(lake,  fK,   0.9)
        setLFWeight(lake,  fL,   0.5)
        setLFWeight(lake,  fSh,  0.35)
        setLFWeight(lake,  fKa,  0.3)
        setLFWeight(lake,  fOs,  0.05) // legendary
        setLFWeight(lake,  fSi,  0.35)
        setLFWeight(lake,  fTo,  0.25)
        setLFWeight(lake,  fGa,  0.2)
        setLFWeight(lake,  fSo,  0.12) // epic
        setLFWeight(lake,  fEel, 0.08) // epic

        // простые для озера
        setLFWeight(lake,  fGu2, 0.6)
        setLFWeight(lake,  fKr2, 0.6)
        setLFWeight(lake,  fEr,  0.4)
        setLFWeight(lake,  fPe,  0.5)
        setLFWeight(lake,  fEl2, 0.3)
        setLFWeight(lake,  fYa2, 0.2)

        // Болото — стоячая вода + эпики
        setLFWeight(swamp, fK,   1.3)
        setLFWeight(swamp, fLi,  1.0)
        setLFWeight(swamp, fRo,  0.9)
        setLFWeight(swamp, fP,   0.5)
        setLFWeight(swamp, fO,   0.4)
        setLFWeight(swamp, fSh,  0.25)
        setLFWeight(swamp, fKa,  0.3)
        setLFWeight(swamp, fEel, 0.15) // epic
        setLFWeight(swamp, fSo,  0.05) // epic

        // простые для болота
        setLFWeight(swamp, fGu2, 0.7)
        setLFWeight(swamp, fKr2, 0.8)
        setLFWeight(swamp, fEr,  0.4)
        setLFWeight(swamp, fPe,  0.4)
        setLFWeight(swamp, fVh,  0.5)

        // Горная река — легендарный таймень
        setLFWeight(mtnRiver, fHa, 0.9)
        setLFWeight(mtnRiver, fFr, 0.8)
        setLFWeight(mtnRiver, fNa, 0.4)
        setLFWeight(mtnRiver, fGo, 0.3)
        setLFWeight(mtnRiver, fTa, 0.05) // legendary

        // немного мелочи для реализма
        setLFWeight(mtnRiver, fEl2, 0.25)
        setLFWeight(mtnRiver, fEr,  0.20)

        // Водохранилище — много хищника, редкая стерлядь/осётр
        setLFWeight(reservoir, fZu,  0.9)
        setLFWeight(reservoir, fJe,  0.6)
        setLFWeight(reservoir, fL,   0.7)
        setLFWeight(reservoir, fKa,  0.5)
        setLFWeight(reservoir, fTo,  0.5)
        setLFWeight(reservoir, fSo,  0.22) // epic
        setLFWeight(reservoir, fP,   0.4)
        setLFWeight(reservoir, fO,   0.4)
        setLFWeight(reservoir, fGa,  0.3)
        setLFWeight(reservoir, fSter,0.03) // epic
        setLFWeight(reservoir, fOs,  0.01) // legendary

        // простые для водохранилища
        setLFWeight(reservoir, fGu2, 0.4)
        setLFWeight(reservoir, fEr,  0.3)
        setLFWeight(reservoir, fPe,  0.35)
        setLFWeight(reservoir, fYa2, 0.35)

        // Дельта реки — смешанная солоноватая зона (пресные + морские)
        setLFWeight(delta, fCh,  0.7)   // fresh
        setLFWeight(delta, fZu,  0.6)   // fresh
        setLFWeight(delta, fL,   0.5)   // fresh
        setLFWeight(delta, fEel, 0.10)  // fresh epic
        setLFWeight(delta, fOs,  0.05)  // fresh legendary (проходной)
        setLFWeight(delta, fMu,  0.45)  // salt
        setLFWeight(delta, fHe,  0.35)  // salt
        setLFWeight(delta, fSm,  0.30)  // salt (корюшка)
        setLFWeight(delta, fBas, 0.18)  // salt (лаврак)
        setLFWeight(delta, fSal, 0.12)  // salt epic
        setLFWeight(delta, fBel, 0.01)  // salt legendary

        // простые для дельты (оба типа воды)
        setLFWeight(delta, fBy2, 0.50)  // salt
        setLFWeight(delta, fKi2, 0.25)  // salt
        setLFWeight(delta, fMo2, 0.20)  // salt
        setLFWeight(delta, fKr2, 0.35)  // fresh
        setLFWeight(delta, fPe,  0.25)  // fresh

        // Прибрежье моря — массовые морские + эпики
        setLFWeight(coast, fMu,  0.9)
        setLFWeight(coast, fFl,  0.6)
        setLFWeight(coast, fHe,  0.8)
        setLFWeight(coast, fSt,  0.6)
        setLFWeight(coast, fSe,  0.3)
        setLFWeight(coast, fMac, 0.6)   // скумбрия
        setLFWeight(coast, fBas, 0.25)  // лаврак
        setLFWeight(coast, fSal, 0.08)  // epic
        // простая мелочь
        setLFWeight(coast, fBy2, 0.35)
        setLFWeight(coast, fKi2, 0.30)
        setLFWeight(coast, fMo2, 0.25)
        // можно добавить немного корюшки и у берега:
        setLFWeight(coast, fSm,  0.10)

        // Фьорд — холодные глубины: легендарный палтус, эпик лосось
        setLFWeight(fjord, fCo,   0.9)
        setLFWeight(fjord, fSa,   0.7)
        setLFWeight(fjord, fHe,   0.6)
        setLFWeight(fjord, fSe,   0.4)
        setLFWeight(fjord, fHa2,  0.08) // legendary
        setLFWeight(fjord, fSal,  0.10) // epic
        setLFWeight(fjord, fSm,   0.12) // корюшка

        // простая мелочь для фьорда
        setLFWeight(fjord, fMo2, 0.20)
        setLFWeight(fjord, fKi2, 0.20)
        setLFWeight(fjord, fBy2, 0.10)

        // --- Приманки ---
        val presnMir = upsertLure("Пресная мирная", false, "fresh")
        upsertLure("Пресная хищная", true,  "fresh")
        upsertLure("Морская мирная", false, "salt")
        upsertLure("Морская хищная", true,  "salt")
        upsertLure("Пресная мирная+", false, "fresh", 0.3)
        upsertLure("Пресная хищная+", true,  "fresh", 0.3)
        upsertLure("Морская мирная+", false, "salt",  0.3)
        upsertLure("Морская хищная+", true,  "salt",  0.3)

        // set default current lure for existing users if null
        Users.update({ Users.currentLureId.isNull() }) { it[Users.currentLureId] = presnMir }
    }
}

// Table definitions

object Users : LongIdTable() {
    val tgId = long("tg_id").uniqueIndex()
    val level = integer("level")
    val xp = integer("xp")
    val createdAt = timestamp("created_at")
    val lastDailyAt = timestamp("last_daily_at").nullable()
    val currentLocationId = reference("current_location_id", Locations).nullable()
    val currentLureId = reference("current_lure_id", Lures).nullable()
    val lastCastAt = timestamp("last_cast_at").nullable()
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
    val name = varchar("name", 100)
    val priceStars = integer("price_stars").nullable()
    val modsJson = text("mods_json")
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

object LocationFishWeights : Table() {
    val locationId = reference("location_id", Locations)
    val fishId = reference("fish_id", Fish)
    val weight = double("weight")
    override val primaryKey = PrimaryKey(locationId, fishId)
}
