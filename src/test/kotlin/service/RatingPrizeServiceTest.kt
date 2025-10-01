package service

import app.Env
import db.Catches
import db.DB
import db.Fish
import db.Locations
import db.RatingPrizes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
        val aggregated = pending.single()
        assertEquals(1, aggregated.qty, "Aggregate rating prize should be claimed in a single action")
        assertEquals(400, aggregated.coins)

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

    @Test
    fun distributeDailyPrizesSumMultiplePlacementsForPlayer() {
        val dbName = "testdb_rating_prize_distribution"
        DB.init(testEnv(dbName))
        val fishing = FishingService()
        val rating = RatingPrizeService()
        val mainUser = fishing.ensureUserByTgId(100L)
        val rivalA = fishing.ensureUserByTgId(101L)
        val rivalB = fishing.ensureUserByTgId(102L)

        val (pondId, swampId) = transaction {
            val ids = Locations.selectAll()
                .orderBy(Locations.id, SortOrder.ASC)
                .limit(2)
                .map { it[Locations.id].value }
            require(ids.size >= 2) { "Expected at least two locations in seed data" }
            ids[0] to ids[1]
        }
        val sampleFishId = transaction {
            Fish.selectAll()
                .orderBy(Fish.id, SortOrder.ASC)
                .limit(1)
                .single()[Fish.id].value
        }

        val prizeDate = LocalDate.of(2024, 1, 15)
        val zone = ZoneId.of("Europe/Belgrade")
        val base = prizeDate.atStartOfDay(zone).toInstant()

        fun insertCatch(userId: Long, locationId: Long, weight: Double, offsetHours: Long) {
            transaction {
                Catches.insert {
                    it[Catches.userId] = userId
                    it[Catches.fishId] = sampleFishId
                    it[Catches.weight] = weight
                    it[Catches.locationId] = locationId
                    it[Catches.createdAt] = base.plusSeconds(offsetHours * 3600)
                    it[Catches.coins] = null
                }
            }
        }

        // Pond results: main user takes 1st and 3rd, rivalA is 2nd.
        insertCatch(mainUser, pondId, 10.0, 1)
        insertCatch(rivalA, pondId, 9.5, 2)
        insertCatch(mainUser, pondId, 9.0, 3)

        // Swamp results: main user takes 1st and 4th, rivals take the middle spots.
        insertCatch(mainUser, swampId, 8.0, 4)
        insertCatch(rivalB, swampId, 7.5, 5)
        insertCatch(rivalA, swampId, 7.0, 6)
        insertCatch(mainUser, swampId, 6.0, 7)

        val distributionInstant = prizeDate.plusDays(1).atStartOfDay(zone).toInstant()
        rating.distributeDailyPrizes(now = distributionInstant, zone = zone)

        transaction {
            val prizes = RatingPrizes.select {
                (RatingPrizes.userId eq mainUser) and (RatingPrizes.prizeDate eq prizeDate)
            }
                .orderBy(RatingPrizes.locationId to SortOrder.ASC, RatingPrizes.rank to SortOrder.ASC)
                .map { it[RatingPrizes.rank] to it[RatingPrizes.coins] }
            assertEquals(
                listOf(1 to 150, 3 to 50, 1 to 200, 4 to 50),
                prizes,
                "Expected two prize placements per location for the main user",
            )
        }

        val pending = rating.pendingPrizes(mainUser)
        assertEquals(1, pending.size)
        val aggregated = pending.single()
        assertEquals(1, aggregated.qty)
        assertEquals(450, aggregated.coins)

        assertEquals(450, rating.claimAll(mainUser))
        assertTrue(rating.pendingPrizes(mainUser).isEmpty())
    }
}
