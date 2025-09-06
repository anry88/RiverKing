package service

import db.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import util.Rng
import java.time.*

@Serializable
data class LocationDTO(
    val id: Long,
    val name: String,
    val unlockKg: Double,
    val unlocked: Boolean,
)

@Serializable
data class RecentDTO(val fish: String, val weight: Double, val location: String, val rarity: String, val at: String)

class FishingService {
    fun ensureUserByTgId(tgId: Long): Long = transaction {
        Users.selectAll().where { Users.tgId eq tgId }.singleOrNull()?.get(Users.id)?.value ?: run {
            Users.insertAndGetId {
                it[Users.tgId] = tgId
                it[level] = 1; it[xp] = 0; it[createdAt] = Instant.now()
            }.value
        }
    }

    private fun totalKg(userId: Long) =
        Catches.slice(Catches.weight.sum()).selectAll().where { Catches.userId eq userId }
            .singleOrNull()?.get(Catches.weight.sum()) ?: 0.0

    fun totalCaughtKg(userId: Long): Double = transaction { totalKg(userId) }

    fun todayCaughtKg(userId: Long): Double = transaction {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        Catches.slice(Catches.weight.sum()).selectAll()
            .where { (Catches.userId eq userId) and (Catches.createdAt greaterEq start) }
            .singleOrNull()?.get(Catches.weight.sum()) ?: 0.0
    }

    fun locations(userId: Long): List<LocationDTO> = transaction {
        val total = totalKg(userId)
        Locations.selectAll().orderBy(Locations.unlockKg).map {
            val unlock = it[Locations.unlockKg]
            LocationDTO(
                it[Locations.id].value,
                it[Locations.name],
                unlock,
                unlock <= total,
            )
        }
    }

    fun giveDailyBaits(userId: Long, qty: Int = 15): Boolean = transaction {
        val today = LocalDate.now()
        val row = Users.selectAll().where { Users.id eq userId }.forUpdate().single()
        val last = row[Users.lastDailyAt]?.atZone(ZoneId.systemDefault())?.toLocalDate()
        if (last == today) return@transaction false
        val basicId = Lures.selectAll().where { Lures.name eq "Basic Bait" }.single()[Lures.id].value
        val cur = InventoryLures.selectAll().where { (InventoryLures.userId eq userId) and (InventoryLures.lureId eq basicId) }
            .singleOrNull()?.get(InventoryLures.qty) ?: 0
        if (cur == 0) InventoryLures.insert {
            it[InventoryLures.userId] = userId
            it[InventoryLures.lureId] = basicId
            it[InventoryLures.qty] = qty
        } else InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq basicId) }) {
            it[InventoryLures.qty] = cur + qty
        }
        Users.update({ Users.id eq userId }) { it[lastDailyAt] = Instant.now() }
        true
    }

    fun canClaimDaily(userId: Long): Boolean = transaction {
        val today = LocalDate.now()
        val last = Users.selectAll().where { Users.id eq userId }.singleOrNull()?.get(Users.lastDailyAt)
            ?.atZone(ZoneId.systemDefault())?.toLocalDate()
        last != today
    }

    fun getBaits(userId: Long): Int = transaction {
        val basicId = Lures.selectAll().where { Lures.name eq "Basic Bait" }.single()[Lures.id].value
        InventoryLures.selectAll().where { (InventoryLures.userId eq userId) and (InventoryLures.lureId eq basicId) }
            .singleOrNull()?.get(InventoryLures.qty) ?: 0
    }

    fun setLocation(userId: Long, locationId: Long) = transaction {
        val loc = Locations.selectAll().where { Locations.id eq locationId }.singleOrNull()
            ?: error("bad location")
        val total = totalKg(userId)
        require(loc[Locations.unlockKg] <= total) { "locked" }
        Users.update({ Users.id eq userId }) { it[currentLocationId] = locationId }
    }

    @Serializable
    data class CatchDTO(val fish: String, val weight: Double, val location: String, val rarity: String)

    @Serializable
    data class CastResultDTO(val caught: Boolean, val catch: CatchDTO? = null)

    private fun rarityModifier(rarity: String, factor: Double): Double = when (rarity) {
        "common" -> 1.0 - 0.8 * factor
        "uncommon" -> 0.6 + 0.4 * factor
        "rare" -> 0.3 + 0.7 * factor
        "epic" -> 0.2 + 0.8 * factor
        "legendary" -> 0.1 + 0.9 * factor
        else -> 1.0
    }

    fun cast(userId: Long, waitSeconds: Int, reactionTime: Double): CastResultDTO = transaction {
        // consume 1 Basic Bait
        val basicId = Lures.selectAll().where { Lures.name eq "Basic Bait" }.single()[Lures.id].value
        val row = InventoryLures.selectAll().where { (InventoryLures.userId eq userId) and (InventoryLures.lureId eq basicId) }
            .forUpdate().singleOrNull() ?: error("No baits")
        val q = row[InventoryLures.qty]; require(q > 0) { "No baits" }
        InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq basicId) }) {
            it[InventoryLures.qty] = q - 1
        }

        val total = totalKg(userId)
        val locId = Users.selectAll().where { Users.id eq userId }.single()[Users.currentLocationId]?.value
            ?: Locations.selectAll().where { Locations.unlockKg lessEq total }.orderBy(Locations.unlockKg).first()[Locations.id].value
        val locRow = Locations.selectAll().where { Locations.id eq locId }.single()
        require(locRow[Locations.unlockKg] <= total) { "locked" }
        val pool = (LocationFishWeights innerJoin Fish)
            .slice(Fish.id, Fish.name, Fish.meanKg, Fish.varKg, Fish.rarity, LocationFishWeights.weight)
            .selectAll().where { LocationFishWeights.locationId eq locId }
            .toList()
        require(pool.isNotEmpty()) { "Empty location" }

        val wait = waitSeconds.coerceIn(5, 30)
        val factor = (wait - 5).toDouble() / 25.0
        val rnd = Rng.fast()
        val totalWeight = pool.sumOf { it[LocationFishWeights.weight] * rarityModifier(it[Fish.rarity], factor) }
        var roll = rnd.nextDouble() * totalWeight
        val picked = pool.first { row2 ->
            roll -= row2[LocationFishWeights.weight] * rarityModifier(row2[Fish.rarity], factor)
            roll <= 0.0
        }

        if (reactionTime >= 3.0) return@transaction CastResultDTO(false)
        val catchChance = 1.0 - reactionTime / 3.0
        if (rnd.nextDouble() > catchChance) return@transaction CastResultDTO(false)

        val fishId = picked[Fish.id].value
        val fishName = picked[Fish.name]
        val rarity = picked[Fish.rarity]
        val weight = Rng.logNormalKg(picked[Fish.meanKg], picked[Fish.varKg]) * locRow[Locations.sizeMultiplier]

        Catches.insert {
            it[Catches.userId] = userId
            it[Catches.fishId] = fishId
            it[Catches.weight] = weight
            it[Catches.locationId] = locId
            it[Catches.createdAt] = Instant.now()
        }
        val locName = locRow[Locations.name]
        CastResultDTO(true, CatchDTO(fishName, weight, locName, rarity))
    }

    fun recent(userId: Long, limit: Int = 5): List<RecentDTO> = transaction {
        (Catches innerJoin Fish innerJoin Locations)
            .slice(Fish.name, Fish.rarity, Catches.weight, Catches.createdAt, Locations.name)
            .selectAll().where { Catches.userId eq userId }
            .orderBy(Catches.createdAt, SortOrder.DESC)
            .limit(limit)
            .map {
                RecentDTO(
                    it[Fish.name],
                    it[Catches.weight],
                    it[Locations.name],
                    it[Fish.rarity],
                    it[Catches.createdAt].toString()
                )
            }
    }
}
