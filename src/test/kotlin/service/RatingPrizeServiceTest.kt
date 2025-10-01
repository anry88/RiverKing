package service

import app.Env
import db.DB
import db.Locations
import db.RatingPrizes
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class RatingPrizeServiceTest {
    private fun testEnv(name: String) = Env(
        botToken = "",
        telegramWebhookSecret = "",
        publicBaseUrl = "http://localhost",
        dbUrl = "jdbc:sqlite:file:$name?mode=memory&cache=shared",
        dbUser = "",
        dbPass = "",
        port = 0,
        devMode = true,
        adminTgId = 0L,
        providerToken = "",
        botName = "",
    )

    @Test
    fun aggregatePrizeIdIsPreciselyRepresentable() {
        val asDouble = RATING_AGGREGATE_PRIZE_ID.toDouble()
        assertEquals(
            RATING_AGGREGATE_PRIZE_ID,
            asDouble.toLong(),
            "Aggregate prize id must remain within the JavaScript safe integer range.",
        )
        assertTrue(
            RATING_AGGREGATE_PRIZE_ID in (-9007199254740991L..0L),
            "Aggregate prize id should stay negative and within ±2^53-1.",
        )
    }

    @Test
    fun claimAllAggregatesCoinsAcrossPrizes() {
        val dbName = "testdb_rating_prize_sum"
        DB.init(testEnv(dbName))
        val fishing = FishingService()
        val rating = RatingPrizeService()
        val userId = fishing.ensureUserByTgId(42L)

        val (pondId, swampId) = transaction {
            val ids = Locations.selectAll()
                .orderBy(Locations.id, SortOrder.ASC)
                .limit(2)
                .map { it[Locations.id].value }
            require(ids.size >= 2) { "Expected at least two locations in seed data" }
            ids[0] to ids[1]
        }
        val prizeDate = LocalDate.of(2024, 1, 15)

        transaction {
            RatingPrizes.insert {
                it[RatingPrizes.userId] = userId
                it[RatingPrizes.locationId] = pondId
                it[RatingPrizes.prizeDate] = prizeDate
                it[RatingPrizes.rank] = 1
                it[RatingPrizes.coins] = 250
                it[RatingPrizes.claimed] = false
                it[RatingPrizes.createdAt] = Instant.now()
            }
            RatingPrizes.insert {
                it[RatingPrizes.userId] = userId
                it[RatingPrizes.locationId] = swampId
                it[RatingPrizes.prizeDate] = prizeDate
                it[RatingPrizes.rank] = 3
                it[RatingPrizes.coins] = 150
                it[RatingPrizes.claimed] = false
                it[RatingPrizes.createdAt] = Instant.now()
            }
        }

        val pending = rating.pendingPrizes(userId)
        assertEquals(1, pending.size, "Rating prizes should expose a single aggregate entry")
        assertEquals(400, pending.single().coins)

        val totalClaimed = rating.claimAll(userId)
        assertEquals(400, totalClaimed)

        val afterClaim = rating.pendingPrizes(userId)
        assertTrue(afterClaim.isEmpty(), "All rating prizes must be marked claimed")

        transaction {
            val claimedFlags = RatingPrizes.select { RatingPrizes.userId eq userId }
                .map { it[RatingPrizes.claimed] }
            assertTrue(claimedFlags.all { it }, "Every stored rating prize should be flagged as claimed")
        }
    }
}
