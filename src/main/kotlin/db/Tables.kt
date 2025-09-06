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
        if (Locations.selectAll().empty()) {
            val pond = Locations.insertAndGetId { it[name] = "Пруд"; it[levelReq] = 1 }.value
            val river = Locations.insertAndGetId { it[name] = "Река"; it[levelReq] = 1 }.value
            val lake  = Locations.insertAndGetId { it[name] = "Озеро"; it[levelReq] = 2 }.value

            fun addFish(n: String, r: String, mean: Double, vari: Double) =
                Fish.insertAndGetId { it[name] = n; it[rarity] = r; it[meanKg] = mean; it[varKg] = vari }.value

            val fP = addFish("Плотва","common",0.2,0.05)
            val fO = addFish("Окунь","common",0.25,0.07)
            val fK = addFish("Карась","common",0.3,0.1)
            val fL = addFish("Лещ","uncommon",0.8,0.2)
            val fSh= addFish("Щука","rare",3.0,1.2)
            val fKa= addFish("Карп","rare",2.5,1.0)
            val fSo= addFish("Сом","epic",8.0,4.0)
            val fOs= addFish("Осётр","legendary",12.0,6.0)

            fun lw(loc: Long, id: Long, w: Double) = LocationFishWeights.insert {
                it[locationId] = loc; it[fishId] = id; it[weight] = w
            }
            // Пруд
            lw(pond,fP,1.0); lw(pond,fO,0.9); lw(pond,fK,1.1); lw(pond,fL,0.4); lw(pond,fKa,0.15)
            // Река
            lw(river,fP,0.7); lw(river,fO,0.7); lw(river,fL,0.6); lw(river,fSh,0.25); lw(river,fSo,0.08)
            // Озеро
            lw(lake,fP,0.6); lw(lake,fK,0.9); lw(lake,fL,0.5); lw(lake,fSh,0.35); lw(lake,fKa,0.3)
        }
        if (Lures.select { Lures.name eq "Basic Bait" }.empty()) {
            Lures.insert { it[name] = "Basic Bait"; it[priceStars] = null; it[modsJson] = "{\"rare\":1.0}" }
        }
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
    val lastCastAt = timestamp("last_cast_at").nullable()
}

object Locations : LongIdTable() {
    val name = varchar("name", 100)
    val levelReq = integer("level_req")
}

object Fish : LongIdTable() {
    val name = varchar("name", 100)
    val rarity = varchar("rarity", 50)
    val meanKg = double("mean_kg")
    val varKg = double("var_kg")
}

object Lures : LongIdTable() {
    val name = varchar("name", 100)
    val priceStars = integer("price_stars").nullable()
    val modsJson = text("mods_json")
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
