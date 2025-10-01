package service

import db.Catches
import db.Fish
import db.Locations
import db.RatingPrizes
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

// The aggregate rating prize identifier is exposed via JSON to the web client,
// which parses numbers using double precision. Any value outside the
// JavaScript "safe" integer range (±2^53-1) would lose precision and break the
// claim endpoint. Keep this identifier within the safe range to avoid that.
const val RATING_AGGREGATE_PRIZE_ID: Long = -1L

class RatingPrizeService {
    @Volatile
    private var lastDistributedDate: LocalDate? = null
    private val distributionLock = Any()

    private data class CatchRow(
        val catchId: Long,
        val userId: Long,
        val locationId: Long,
        val rarity: String,
        val weight: Double,
    )

    private fun rarityRank(r: String) = when (r.lowercase()) {
        "legendary" -> 6
        "mythic" -> 5
        "epic" -> 4
        "rare" -> 3
        "uncommon" -> 2
        "common" -> 1
        else -> 0
    }

    fun distributeDailyPrizes(now: Instant = Instant.now(), zone: ZoneId = ZoneId.of("Europe/Belgrade")) {
        val zoned = ZonedDateTime.ofInstant(now, zone)
        val targetDate = zoned.minusDays(1).toLocalDate()
        synchronized(distributionLock) {
            val lastDate = lastDistributedDate
            if (lastDate != null && !targetDate.isAfter(lastDate)) {
                return
            }

            val start = targetDate.atStartOfDay(zone).toInstant()
            val end = start.plus(Duration.ofDays(1))

            transaction {
                val awarded = RatingPrizes
                    .slice(RatingPrizes.locationId)
                    .select { RatingPrizes.prizeDate eq targetDate }
                    .map { it[RatingPrizes.locationId].value }
                    .toMutableSet()

                val rows = ((Catches innerJoin Fish) innerJoin Locations)
                    .select { (Catches.createdAt greaterEq start) and (Catches.createdAt less end) }
                    .map { row ->
                        CatchRow(
                            catchId = row[Catches.id].value,
                            userId = row[Catches.userId].value,
                            locationId = row[Catches.locationId].value,
                            rarity = row[Fish.rarity],
                            weight = row[Catches.weight],
                        )
                    }

                val byLocation = rows.groupBy { it.locationId }
                for ((locationId, list) in byLocation) {
                    if (locationId in awarded) continue
                    if (list.isEmpty()) continue
                    val maxPlaces = minOf(list.size, 10)
                    val sorted = list.sortedWith(
                        compareByDescending<CatchRow> { rarityRank(it.rarity) }
                            .thenByDescending { it.weight }
                            .thenBy { it.catchId }
                    )
                    sorted.take(maxPlaces).forEachIndexed { index, catch ->
                        val coins = (maxPlaces - index) * 50
                        RatingPrizes.insert {
                            it[RatingPrizes.userId] = catch.userId
                            it[RatingPrizes.locationId] = locationId
                            it[prizeDate] = targetDate
                            it[rank] = index + 1
                            it[RatingPrizes.coins] = coins
                            it[claimed] = false
                            it[createdAt] = now
                        }
                    }
                    awarded += locationId
                }
            }

            lastDistributedDate = targetDate
        }
    }

    fun pendingPrizes(userId: Long): List<UserPrize> = transaction {
        val rows = RatingPrizes
            .select { (RatingPrizes.userId eq userId) and (RatingPrizes.claimed eq false) }
            .toList()
        if (rows.isEmpty()) {
            emptyList()
        } else {
            val totalCoins = rows.sumOf { it[RatingPrizes.coins] }
            if (totalCoins <= 0) {
                emptyList()
            } else {
                listOf(
                    UserPrize(
                        id = RATING_AGGREGATE_PRIZE_ID,
                        packageId = COIN_PRIZE_ID,
                        qty = 1,
                        rank = 0,
                        coins = totalCoins,
                    )
                )
            }
        }
    }

    fun claimAll(userId: Long): Int = transaction {
        val rows = RatingPrizes
            .select { (RatingPrizes.userId eq userId) and (RatingPrizes.claimed eq false) }
            .toList()
        if (rows.isEmpty()) {
            0
        } else {
            RatingPrizes.update({
                (RatingPrizes.userId eq userId) and (RatingPrizes.claimed eq false)
            }) { it[claimed] = true }
            rows.sumOf { it[RatingPrizes.coins] }
        }
    }

    fun claimPrize(userId: Long, prizeId: Long, fishing: FishingService) {
        val coins = transaction {
            val row = RatingPrizes.select {
                (RatingPrizes.id eq prizeId) and (RatingPrizes.userId eq userId) and (RatingPrizes.claimed eq false)
            }.singleOrNull() ?: error("not found")
            RatingPrizes.update({ RatingPrizes.id eq prizeId }) { it[claimed] = true }
            row[RatingPrizes.coins]
        }
        if (coins > 0) {
            fishing.addCoins(userId, coins)
        }
    }
}
