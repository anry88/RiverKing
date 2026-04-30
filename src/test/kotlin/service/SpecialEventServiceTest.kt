package service

import db.Catches
import db.ClubMembers
import db.Clubs
import db.DB
import db.Fish
import db.InventoryLures
import db.Lures
import db.SpecialEventPrizes
import db.SpecialEventUserProgress
import db.Users
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import support.testEnv

class SpecialEventServiceTest {
    @Test
    fun createRejectsOverlappingEvents() {
        DB.init(testEnv("special-event-overlap"))
        val events = SpecialEventService()
        val fishId = fishId("Плотва")
        val start = Instant.parse("2026-04-01T00:00:00Z")

        events.createEvent(
            nameRu = "Апрельский залив",
            nameEn = "April Bay",
            start = start,
            end = start.plusSeconds(86_400),
            imagePath = null,
            castZone = defaultCastZone,
            fish = listOf(SpecialEventFishSpec(fishId, 10.0)),
            weightPrizes = noPrizes,
            countPrizes = noPrizes,
            fishPrizes = noPrizes,
        )

        val error = assertFailsWith<SpecialEventService.SpecialEventException> {
            events.createEvent(
                nameRu = "Второй залив",
                nameEn = "Second Bay",
                start = start.plusSeconds(3_600),
                end = start.plusSeconds(90_000),
                imagePath = null,
                castZone = defaultCastZone,
                fish = listOf(SpecialEventFishSpec(fishId, 1.0)),
                weightPrizes = noPrizes,
                countPrizes = noPrizes,
                fishPrizes = noPrizes,
            )
        }
        assertEquals("event_overlap", error.code)
    }

    @Test
    fun eventFishingAllowsMixedLureCompatibility() {
        DB.init(testEnv("special-event-mixed-lures"))
        val fishing = FishingService()
        val events = SpecialEventService()
        val now = Instant.now()
        val eventId = events.createEvent(
            nameRu = "Смешанный канал",
            nameEn = "Mixed Canal",
            start = now.minusSeconds(3_600),
            end = now.plusSeconds(3_600),
            imagePath = null,
            castZone = defaultCastZone,
            fish = listOf(SpecialEventFishSpec(fishId("Плотва"), 1.0)),
            weightPrizes = noPrizes,
            countPrizes = noPrizes,
            fishPrizes = noPrizes,
        )
        val locationId = events.eventLocationId(eventId) ?: error("event location missing")
        val userId = fishing.ensureUserByTgId(10_900L, username = "mixed-lure")
        createClub(userId, "Mixed Lures")
        fishing.setLocation(userId, locationId)

        transaction {
            val saltPredatorLureId = Lures
                .select { Lures.name eq "Морская хищная" }
                .single()[Lures.id].value
            val existing = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq saltPredatorLureId)
            }.singleOrNull()
            if (existing == null) {
                InventoryLures.insert {
                    it[InventoryLures.userId] = userId
                    it[InventoryLures.lureId] = saltPredatorLureId
                    it[InventoryLures.qty] = 2
                }
            } else {
                InventoryLures.update({
                    (InventoryLures.userId eq userId) and (InventoryLures.lureId eq saltPredatorLureId)
                }) {
                    it[InventoryLures.qty] = 2
                }
            }
            Users.update({ Users.id eq userId }) {
                it[Users.currentLureId] = saltPredatorLureId
            }
        }

        fishing.startCast(userId)
    }

    @Test
    fun eventCatchesUpdateClubWeightAndCountLeaderboards() {
        DB.init(testEnv("special-event-club-progress"))
        val fishing = FishingService()
        val events = SpecialEventService()
        val eventId = createActiveEvent(events)
        val locationId = events.eventLocationId(eventId) ?: error("event location missing")
        val captainId = fishing.ensureUserByTgId(10_001L, username = "captain")
        val memberId = fishing.ensureUserByTgId(10_002L, username = "member")
        val clubId = createClub(captainId, "Alpha")
        addMember(memberId, clubId)
        val caughtAt = Instant.parse("2026-04-01T12:00:00Z")

        recordEventCatch(events, captainId, locationId, "Плотва", 5.5, caughtAt)
        recordEventCatch(events, memberId, locationId, "Щука", 2.0, caughtAt.plusSeconds(10))

        val leaderboard = events.leaderboard(eventId, captainId) ?: error("leaderboard missing")
        assertEquals(7.5, leaderboard.weight.single().value, 0.000001)
        assertEquals(2.0, leaderboard.count.single().value, 0.000001)
        assertEquals(clubId, leaderboard.mineWeight?.clubId)
        assertEquals(clubId, leaderboard.mineCount?.clubId)
        assertEquals("Щука", leaderboard.fish.first().fish)
    }

    @Test
    fun leavingClubSubtractsActiveProgressButKeepsPersonalTopFish() {
        DB.init(testEnv("special-event-leave"))
        val fishing = FishingService()
        val events = SpecialEventService()
        val eventId = createActiveEvent(events)
        val locationId = events.eventLocationId(eventId) ?: error("event location missing")
        val userId = fishing.ensureUserByTgId(20_001L, username = "leaver")
        createClub(userId, "Leavers")
        val caughtAt = Instant.parse("2026-04-01T13:00:00Z")

        recordEventCatch(events, userId, locationId, "Щука", 10.0, caughtAt)
        transaction {
            events.deactivateActiveProgressForUserTx(userId, caughtAt.plusSeconds(60))
            ClubMembers.deleteWhere { ClubMembers.userId eq userId }
        }

        val leaderboard = events.leaderboard(eventId, userId) ?: error("leaderboard missing")
        assertEquals(emptyList(), leaderboard.weight)
        assertNull(leaderboard.mineWeight)
        assertNotNull(leaderboard.mineFish)
        assertEquals("Щука", leaderboard.mineFish?.fish)
    }

    @Test
    fun rejoinStartsClubProgressFromZero() {
        DB.init(testEnv("special-event-rejoin"))
        val fishing = FishingService()
        val events = SpecialEventService()
        val eventId = createActiveEvent(events)
        val locationId = events.eventLocationId(eventId) ?: error("event location missing")
        val userId = fishing.ensureUserByTgId(30_001L, username = "returning")
        val clubId = createClub(userId, "Returners")
        val caughtAt = Instant.parse("2026-04-01T14:00:00Z")

        recordEventCatch(events, userId, locationId, "Плотва", 6.0, caughtAt)
        transaction {
            events.deactivateActiveProgressForUserTx(userId, caughtAt.plusSeconds(60))
            ClubMembers.deleteWhere { ClubMembers.userId eq userId }
        }
        addMember(userId, clubId, ClubService.ROLE_PRESIDENT)
        recordEventCatch(events, userId, locationId, "Карась", 3.0, caughtAt.plusSeconds(120))

        val leaderboard = events.leaderboard(eventId, userId) ?: error("leaderboard missing")
        assertEquals(3.0, leaderboard.weight.single().value, 0.000001)
        assertEquals(1.0, leaderboard.count.single().value, 0.000001)
        val progressRows = transaction {
            SpecialEventUserProgress.select {
                (SpecialEventUserProgress.eventId eq eventId) and (SpecialEventUserProgress.userId eq userId)
            }.count()
        }
        assertEquals(2L, progressRows)
    }

    @Test
    fun prizeDistributionGrantsClubRewardsToCurrentMembersAndPersonalRewardsToRankedPlayers() {
        DB.init(testEnv("special-event-prizes"))
        val fishing = FishingService()
        val events = SpecialEventService()
        val fishId = fishId("Щука")
        val start = Instant.parse("2026-04-01T00:00:00Z")
        val eventId = events.createEvent(
            nameRu = "Призовой залив",
            nameEn = "Prize Bay",
            start = start,
            end = start.plusSeconds(7_200),
            imagePath = null,
            castZone = defaultCastZone,
            fish = listOf(SpecialEventFishSpec(fishId, 1.0)),
            weightPrizes = SpecialEventPrizeConfig(1, """[{"pack":"coins","qty":1000,"coins":1000}]"""),
            countPrizes = SpecialEventPrizeConfig(1, """[{"pack":"coins","qty":500,"coins":500}]"""),
            fishPrizes = SpecialEventPrizeConfig(1, """[{"pack":"coins","qty":100,"coins":100}]"""),
        )
        val locationId = events.eventLocationId(eventId) ?: error("event location missing")
        val winnerId = fishing.ensureUserByTgId(40_001L, username = "winner")
        val teammateId = fishing.ensureUserByTgId(40_002L, username = "teammate")
        val clubId = createClub(winnerId, "Prizers")
        addMember(teammateId, clubId)

        recordEventCatch(events, winnerId, locationId, "Щука", 12.0, start.plusSeconds(1_000))
        events.distributePrizes(start.plusSeconds(7_201))
        events.distributePrizes(start.plusSeconds(7_202))

        val prizes = transaction {
            SpecialEventPrizes.select { SpecialEventPrizes.eventId eq eventId }
                .map { row ->
                    row[SpecialEventPrizes.userId].value to
                        (row[SpecialEventPrizes.category] to row[SpecialEventPrizes.qty])
                }
        }
        assertEquals(
            listOf(1000, 500, 100),
            prizes.filter { it.first == winnerId }.map { it.second.second }.sortedDescending(),
        )
        assertEquals(
            listOf(1000, 500),
            prizes.filter { it.first == teammateId }.map { it.second.second }.sortedDescending(),
        )
        assertEquals(5, prizes.size, "Prize distribution must be idempotent.")

        val winnerAchievement = AchievementService.list(winnerId, "ru")
            .single { it.code == "event_laureate" }
        val winnerAchievementEn = AchievementService.list(winnerId, "en")
            .single { it.code == "event_laureate" }
        val teammateAchievement = AchievementService.list(teammateId, "ru")
            .single { it.code == "event_laureate" }
        assertEquals("Призёр событий", winnerAchievement.name)
        assertEquals("Event Laureate", winnerAchievementEn.name)
        assertEquals(1.0, winnerAchievement.progress, "Multiple event prize categories must count as one event.")
        assertEquals(1.0, teammateAchievement.progress, "Club prize categories from one event must count as one event.")
        assertEquals(1, winnerAchievement.levelIndex)
        assertEquals(3, winnerAchievement.target)
    }

    private fun createActiveEvent(events: SpecialEventService): Long {
        val fishIds = listOf(fishId("Плотва"), fishId("Карась"), fishId("Щука"))
        return events.createEvent(
            nameRu = "Событийная бухта",
            nameEn = "Event Bay",
            start = Instant.parse("2026-04-01T00:00:00Z"),
            end = Instant.parse("2026-04-02T00:00:00Z"),
            imagePath = null,
            castZone = defaultCastZone,
            fish = fishIds.mapIndexed { index, fishId -> SpecialEventFishSpec(fishId, (index + 1).toDouble()) },
            weightPrizes = noPrizes,
            countPrizes = noPrizes,
            fishPrizes = noPrizes,
        )
    }

    private fun fishId(name: String): Long = transaction {
        Fish.select { Fish.name eq name }.single()[Fish.id].value
    }

    private fun createClub(userId: Long, name: String): Long = transaction {
        val clubId = Clubs.insertAndGetId {
            it[Clubs.name] = name
            it[presidentId] = userId
            it[createdAt] = Instant.now()
        }.value
        ClubMembers.insert {
            it[ClubMembers.clubId] = clubId
            it[ClubMembers.userId] = userId
            it[role] = ClubService.ROLE_PRESIDENT
            it[joinedAt] = Instant.now()
        }
        clubId
    }

    private fun addMember(userId: Long, clubId: Long, role: String = ClubService.ROLE_NOVICE) = transaction {
        ClubMembers.insert {
            it[ClubMembers.clubId] = clubId
            it[ClubMembers.userId] = userId
            it[ClubMembers.role] = role
            it[joinedAt] = Instant.now()
        }
    }

    private fun recordEventCatch(
        events: SpecialEventService,
        userId: Long,
        locationId: Long,
        fishName: String,
        weight: Double,
        caughtAt: Instant,
    ): Long = transaction {
        val fish = Fish.select { Fish.name eq fishName }.single()
        val fishId = fish[Fish.id].value
        val catchId = Catches.insertAndGetId {
            it[Catches.userId] = userId
            it[Catches.fishId] = fishId
            it[Catches.weight] = weight
            it[Catches.locationId] = locationId
            it[Catches.createdAt] = caughtAt
            it[Catches.coins] = null
        }.value
        events.recordCatchTx(
            userId = userId,
            catchId = catchId,
            fishId = fishId,
            rarity = fish[Fish.rarity],
            weight = weight,
            locationId = locationId,
            caughtAt = caughtAt,
        )
        catchId
    }

    private companion object {
        val defaultCastZone = CastZoneDTO(
            points = listOf(
                CastZonePointDTO(0.1, 0.4),
                CastZonePointDTO(0.9, 0.4),
                CastZonePointDTO(0.9, 0.8),
                CastZonePointDTO(0.1, 0.8),
            ),
        )
        val noPrizes = SpecialEventPrizeConfig(prizePlaces = 0, prizesJson = "[]")
    }
}
