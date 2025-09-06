package service

import db.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import util.Rng
import java.time.*

@Serializable
data class LocationDTO(val id: Long, val name: String, val desc: String)

@Serializable
data class RecentDTO(val fish: String, val weight: Double, val at: String)

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

    fun locations(userId: Long): List<LocationDTO> = transaction {
        val total = totalKg(userId)
        Locations.selectAll().where { Locations.unlockKg lessEq total }.orderBy(Locations.unlockKg).map {
            LocationDTO(it[Locations.id].value, it[Locations.name], "…")
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

    private fun rateLimit(userId: Long) = transaction {
        val now = Instant.now()
        val last = Users.selectAll().where { Users.id eq userId }.single()[Users.lastCastAt]
        if (last != null && Duration.between(last, now).seconds < 10) false else {
            Users.update({ Users.id eq userId }) { it[lastCastAt] = now }
            true
        }
    }

    @Serializable
    data class CatchDTO(val fish: String, val weight: Double, val location: String)

    fun cast(userId: Long): CatchDTO = transaction {
        require(rateLimit(userId)) { "Too fast" }
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
            .slice(Fish.id, Fish.name, Fish.meanKg, Fish.varKg, LocationFishWeights.weight)
            .selectAll().where { LocationFishWeights.locationId eq locId }
            .toList()
        require(pool.isNotEmpty()) { "Empty location" }

        val rnd = Rng.fast()
        val totalWeight = pool.sumOf { it[LocationFishWeights.weight] }
        var roll = rnd.nextDouble() * totalWeight
        val picked = pool.first { row2 ->
            roll -= row2[LocationFishWeights.weight]
            roll <= 0.0
        }

        val fishId = picked[Fish.id].value
        val fishName = picked[Fish.name]
        val weight = Rng.logNormalKg(picked[Fish.meanKg], picked[Fish.varKg]) * locRow[Locations.sizeMultiplier]

        Catches.insert {
            it[Catches.userId] = userId
            it[Catches.fishId] = fishId
            it[Catches.weight] = weight
            it[Catches.locationId] = locId
            it[Catches.createdAt] = Instant.now()
        }
        val locName = locRow[Locations.name]
        CatchDTO(fishName, weight, locName)
    }

    fun recent(userId: Long, limit: Int = 5): List<RecentDTO> = transaction {
        (Catches innerJoin Fish)
            .slice(Fish.name, Catches.weight, Catches.createdAt)
            .selectAll().where { Catches.userId eq userId }
            .orderBy(Catches.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { RecentDTO(it[Fish.name], it[Catches.weight], it[Catches.createdAt].toString()) }
    }
}
