package service

import db.*
import service.EventCastAreaDTO
import service.SpecialEventFishSpec
import service.SpecialEventService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import support.testEnv

class FishingServiceTest {
    private fun newService(dbName: String, clock: Clock = Clock.systemUTC()): FishingService {
        DB.init(testEnv(dbName))
        return FishingService(clock)
    }

    private class MutableClock(
        private var current: Instant,
        private val zoneId: ZoneId = ZoneOffset.UTC,
    ) : Clock() {
        override fun getZone(): ZoneId = zoneId
        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
        override fun instant(): Instant = current
        fun set(instant: Instant) {
            current = instant
        }
    }

    @Test
    fun hookChallengeUsesRarityAndWeightTapFormula() {
        assertEquals(2, FishingService.rarityTapCount("common"))
        assertEquals(4, FishingService.rarityTapCount("uncommon"))
        assertEquals(6, FishingService.rarityTapCount("rare"))
        assertEquals(8, FishingService.rarityTapCount("epic"))
        assertEquals(10, FishingService.rarityTapCount("mythic"))
        assertEquals(12, FishingService.rarityTapCount("legendary"))
        assertEquals(2, FishingService.rarityTapCount("unknown"))

        assertEquals(1, FishingService.weightTapCount(0.99))
        assertEquals(2, FishingService.weightTapCount(1.0))
        assertEquals(3, FishingService.weightTapCount(5.0))
        assertEquals(4, FishingService.weightTapCount(10.0))
        assertEquals(5, FishingService.weightTapCount(30.0))
        assertEquals(6, FishingService.weightTapCount(60.0))
        assertEquals(7, FishingService.weightTapCount(100.0))
        assertEquals(8, FishingService.weightTapCount(150.0))
        assertEquals(9, FishingService.weightTapCount(250.0))
        assertEquals(10, FishingService.weightTapCount(400.0))
    }

    @Test
    fun hookChallengeDurationAndIntensityScaleWithTapGoal() {
        val easy = FishingService.hookChallengeFor("common", 0.5)
        assertEquals(3, easy.tapGoal)
        assertEquals(5_000, easy.durationMs)
        assertEquals(0.0, easy.struggleIntensity, 0.0000001)

        val medium = FishingService.hookChallengeFor("rare", 60.0)
        assertEquals(12, medium.tapGoal)
        assertEquals(10_000, medium.durationMs)
        assertEquals((12.0 - 3.0) / (22.0 - 3.0), medium.struggleIntensity, 0.0000001)

        val hard = FishingService.hookChallengeFor("legendary", 400.0)
        assertEquals(22, hard.tapGoal)
        assertEquals(15_000, hard.durationMs)
        assertEquals(1.0, hard.struggleIntensity, 0.0000001)
    }

    @Test
    fun baseEscapeChanceProgressesByLocation() {
        val svc = newService("testdb_fish_escape")
        transaction {
            val idsByInsertion = Locations.selectAll().orderBy(Locations.id).map { it[Locations.id].value }
            // Shuffle unlock weights so that the ID order is no longer the same as the unlock order.
            idsByInsertion.reversed().forEachIndexed { idx, id ->
                Locations.update({ Locations.id eq id }) {
                    it[Locations.unlockKg] = (idx + 1) * 10.0
                }
            }

            val idsByUnlock = Locations
                .selectAll()
                .orderBy(Locations.unlockKg to SortOrder.ASC, Locations.id to SortOrder.ASC)
                .map { it[Locations.id].value }

            idsByUnlock.forEachIndexed { idx, id ->
                val expected = (0.05 * idx).coerceAtMost(0.5)
                assertEquals(expected, svc.baseEscapeChance(id), 0.0000001)
            }
        }
    }

    @Test
    fun eventLocationUsesSameBaseEscapeChanceAsPond() {
        val svc = newService("testdb_event_escape_matches_pond")
        val events = SpecialEventService()
        val eventId = events.createEvent(
            nameRu = "Тестовый ивент",
            nameEn = "Test Event",
            start = Instant.parse("2026-04-01T00:00:00Z"),
            end = Instant.parse("2026-04-02T00:00:00Z"),
            imagePath = null,
            castArea = EventCastAreaDTO(minX = 0.1, maxX = 0.9, farY = 0.4, nearY = 0.8),
            fish = listOf(SpecialEventFishSpec(fishId("Плотва"), 1.0)),
            weightPrizes = SpecialEventPrizeConfig(prizePlaces = 0, prizesJson = "[]"),
            countPrizes = SpecialEventPrizeConfig(prizePlaces = 0, prizesJson = "[]"),
            fishPrizes = SpecialEventPrizeConfig(prizePlaces = 0, prizesJson = "[]"),
        )

        transaction {
            val pondId = Locations
                .select { Locations.specialEventId.isNull() }
                .orderBy(Locations.unlockKg to SortOrder.ASC, Locations.id to SortOrder.ASC)
                .first()[Locations.id].value
            val eventLocationId = Locations
                .select { Locations.specialEventId eq eventId }
                .first()[Locations.id].value

            assertEquals(svc.baseEscapeChance(pondId), svc.baseEscapeChance(eventLocationId), 0.0000001)
            assertEquals(0.0, svc.baseEscapeChance(eventLocationId), 0.0000001)
        }
    }

    @Test
    fun restoreCastingLuresReturnsLastUsedBait() {
        val svc = newService("testdb_restore_cast")
        val userId = svc.ensureUserByTgId(1L)
        val lureId = transaction {
            Lures.select { Lures.name eq "Пресная мирная" }.single()[Lures.id].value
        }
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.isCasting] = true
                it[Users.castLureId] = lureId
            }
            InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }) {
                it[InventoryLures.qty] = 9
            }
        }

        val restored = svc.restoreCastingLuresOnStartup()
        assertEquals(1, restored)

        transaction {
            val qty = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId)
            }.single()[InventoryLures.qty]
            assertEquals(10, qty)
            val userRow = Users.select { Users.id eq userId }.single()
            assertEquals(false, userRow[Users.isCasting])
            assertEquals(null, userRow[Users.castLureId])
        }
    }

    @Test
    fun restoreCastingLuresFallsBackToDefaultWhenUnknown() {
        val svc = newService("testdb_restore_fallback")
        val userId = svc.ensureUserByTgId(2L)
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.isCasting] = true
                it[Users.castLureId] = null
            }
        }

        val restored = svc.restoreCastingLuresOnStartup()
        assertEquals(1, restored)

        transaction {
            val fallbackId = Lures.select { Lures.name eq "Пресная хищная+" }.single()[Lures.id].value
            val qty = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq fallbackId)
            }.single()[InventoryLures.qty]
            assertEquals(1, qty)
            val userRow = Users.select { Users.id eq userId }.single()
            assertEquals(false, userRow[Users.isCasting])
            assertEquals(null, userRow[Users.castLureId])
        }
    }

    @Test
    fun shopDiscountsAppliedAndRemoved() {
        val svc = newService("testdb_shop_discount")
        val now = LocalDate.now(ZoneOffset.UTC)
        val packId = "fresh_topup_s"

        svc.setDiscount(packId, price = 10, start = now, end = now.plusDays(2))

        val discounted = svc.listShop("ru").flatMap { it.packs }.first { it.id == packId }
        assertEquals(10, discounted.price)
        assertEquals(20, discounted.originalPrice)
        assertEquals(now.plusDays(2), discounted.discountEnd)

        val discounts = svc.listDiscounts().associateBy { it.packageId }
        val stored = discounts[packId]
        requireNotNull(stored)
        assertEquals(10, stored.price)
        assertEquals(now, stored.startDate)

        svc.removeDiscount(packId)

        val restored = svc.listShop("ru").flatMap { it.packs }.first { it.id == packId }
        assertEquals(20, restored.price)
        assertEquals(null, restored.originalPrice)
    }

    @Test
    fun discountExpiresAtStartOfEndDate() {
        val start = LocalDate.of(2025, 9, 22)
        val endExclusive = start.plusDays(1)
        val zone = ZoneOffset.UTC
        val clock = MutableClock(start.atTime(12, 0).atZone(zone).toInstant(), zone)
        val svc = newService("testdb_discount_end_exclusive", clock)
        val packId = "fresh_topup_s"

        svc.setDiscount(packId, price = 10, start = start, end = endExclusive)

        val activePack = svc.listShop("ru").flatMap { it.packs }.first { it.id == packId }
        assertEquals(10, activePack.price)
        assertEquals(20, activePack.originalPrice)

        clock.set(endExclusive.atStartOfDay(zone).toInstant())

        val expiredPack = svc.listShop("ru").flatMap { it.packs }.first { it.id == packId }
        assertEquals(20, expiredPack.price)
        assertEquals(null, expiredPack.originalPrice)
    }

    @Test
    fun listDiscountsOmitsExpiredEntries() {
        val start = LocalDate.of(2025, 10, 1)
        val endExclusive = start.plusDays(1)
        val zone = ZoneOffset.UTC
        val clock = MutableClock(start.minusDays(1).atStartOfDay(zone).toInstant(), zone)
        val svc = newService("testdb_discount_list_filter", clock)
        val packId = "fresh_topup_s"

        svc.setDiscount(packId, price = 10, start = start, end = endExclusive)

        val scheduled = svc.listDiscounts().associateBy { it.packageId }
        assertEquals(1, scheduled.size)
        assertEquals(10, scheduled[packId]?.price)

        clock.set(endExclusive.atStartOfDay(zone).toInstant())

        val expired = svc.listDiscounts()
        assertEquals(0, expired.size)
    }

    @Test
    fun listRodsAppliesDiscountPriceWhenActive() {
        val zone = ZoneOffset.UTC
        val start = LocalDate.of(2025, 1, 15)
        val clock = MutableClock(start.atTime(12, 0).atZone(zone).toInstant(), zone)
        val svc = newService("testdb_rod_discount_price", clock)
        val userId = svc.ensureUserByTgId(400L)

        svc.setDiscount("rod_dew", price = 7, start = start, end = start.plusDays(1))

        val rods = svc.listRods(userId).associateBy { it.code }
        assertEquals(7, rods["dew"]?.priceStars)
        assertEquals(40, rods["stream"]?.priceStars)
    }

    @Test
    fun startCastReturnsDiscountedRecommendedRodPrice() {
        val zone = ZoneOffset.UTC
        val start = LocalDate.of(2025, 2, 1)
        val clock = MutableClock(start.atTime(12, 0).atZone(zone).toInstant(), zone)
        val svc = newService("testdb_recommended_rod_discount", clock)
        val userId = svc.ensureUserByTgId(500L)

        svc.setDiscount("rod_stream", price = 50, start = start, end = start.plusDays(1))

        transaction {
            val freshPeaceId = Lures.select { Lures.name eq "Пресная мирная" }.single()[Lures.id].value
            InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq freshPeaceId) }) {
                it[InventoryLures.qty] = 1
            }
        }

        val result = svc.startCast(userId)

        assertEquals("stream", result.recommendedRodCode)
        assertEquals("rod_stream", result.recommendedRodPackId)
        assertEquals(50, result.recommendedRodPriceStars)
    }

    @Test
    fun globalTopDailyPrizePreviewAllowsMultiplePlacementsPerPlayer() {
        val svc = newService("testdb_daily_preview")
        val mainUser = svc.ensureUserByTgId(200L)
        val rivalA = svc.ensureUserByTgId(201L)
        val rivalB = svc.ensureUserByTgId(202L)

        val locationId = transaction {
            Locations.selectAll()
                .orderBy(Locations.id to SortOrder.ASC)
                .limit(1)
                .single()[Locations.id].value
        }

        val fishId = transaction {
            Fish.selectAll()
                .orderBy(Fish.id to SortOrder.ASC)
                .limit(1)
                .single()[Fish.id].value
        }

        val zone = ZoneId.of("Europe/Belgrade")
        val today = ZonedDateTime.now(zone).toLocalDate()
        val base = today.atStartOfDay(zone).toInstant()

        fun insertCatch(userId: Long, weight: Double, offsetHours: Long) {
            transaction {
                Catches.insert {
                    it[Catches.userId] = userId
                    it[Catches.fishId] = fishId
                    it[Catches.weight] = weight
                    it[Catches.locationId] = locationId
                    it[Catches.createdAt] = base.plusSeconds(offsetHours * 3600)
                    it[Catches.coins] = null
                }
            }
        }

        insertCatch(mainUser, 10.0, 1)
        insertCatch(rivalA, 9.5, 2)
        insertCatch(mainUser, 9.0, 3)
        insertCatch(rivalB, 8.5, 4)
        insertCatch(mainUser, 8.0, 5)

        val results = svc.globalTopByLocation(locationId, period = "today")

        val prizeSummary = results.take(5).map { it.userId to (it.rank to it.prizeCoins) }
        assertEquals(
            listOf(
                mainUser to (1 to 150),
                rivalA to (2 to 100),
                mainUser to (3 to 50),
                rivalB to (null to null),
                mainUser to (null to null),
            ),
            prizeSummary,
        )
    }

    private fun fishId(name: String): Long = transaction {
        Fish.select { Fish.name eq name }.single()[Fish.id].value
    }
}
