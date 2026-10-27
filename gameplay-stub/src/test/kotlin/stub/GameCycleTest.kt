package stub

import app.Env
import db.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import service.*
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.*

class GameCycleTest {
    private lateinit var fishing: FishingService
    private val cycle get() = GameCycle(waitSeconds = { 5 }, awaitBite = {})

    @BeforeTest
    fun init() {
        val db = Files.createTempFile("riverking-stub-test-", ".db")
        val config = Files.createTempFile("riverking-stub-test-", ".properties")
        Files.writeString(config, "BOT_TOKEN=TEST\nBOT_NAME=test\nPUBLIC_BASE_URL=http://localhost\nDATABASE_URL=jdbc:sqlite:${db}\n")
        DB.init(Env.fromConfig(config.toString()))
        Files.delete(config)
        db.toFile().deleteOnExit()
        fishing = FishingService()
    }

    @AfterTest
    fun close() = DB.close()

    @Test
    fun rosterHasFiftyStableAccountsWithBotSourceAndLanguageSplit() {
        val first = Roster.provision(fishing)
        assertEquals(first, Roster.provision(fishing))
        assertEquals(50, first.distinct().size)
        transaction {
            assertEquals(50L, Users.selectAll().count())
            assertEquals(25L, Users.select { Users.language eq "ru" }.count())
            assertEquals(25L, Users.select { Users.language eq "en" }.count())
            Users.selectAll().forEach {
                assertEquals("bot", it[Users.registrationSource])
                assertNull(it[Users.tgId])
                assertFalse(it[Users.canReceiveNotifications])
                assertFalse(it[Users.nickname].isNullOrBlank())
            }
            assertEquals(50L, AuthIdentities.select { AuthIdentities.provider eq Roster.PROVIDER }.count())
        }
        assertEquals(50, Roster.accounts.map { it.nickname }.distinct().size)
    }

    @Test
    fun clubSearchSkipsClosedAndLockedClubsAndPreservesMembership() {
        val uid = fishing.createUser()
        val closed = club("Closed", open = false)
        val locked = club("Locked", minKg = 1_000.0)
        val available = club("Available")
        cycle.joinClubIfNeeded(uid)
        transaction {
            assertEquals(available, ClubMembers.select { ClubMembers.userId eq uid }.single()[ClubMembers.clubId].value)
            assertEquals(0L, ClubMembers.select { ClubMembers.clubId eq closed }.count())
            assertEquals(0L, ClubMembers.select { ClubMembers.clubId eq locked }.count())
        }
        club("Another")
        cycle.joinClubIfNeeded(uid)
        transaction { assertEquals(available, ClubMembers.select { ClubMembers.userId eq uid }.single()[ClubMembers.clubId].value) }
    }

    @Test
    fun locationsAdvanceAndWrapIncludingActiveLockedAndExpiredEvent() {
        val uid = fishing.createUser()
        val regular = fishing.locations(uid)
        val first = regular.first()
        fishing.setLocation(uid, first.id)
        assertEquals(first.id, cycle.moveLocation(uid)) // Next regular location is locked.
        transaction { Users.update({ Users.id eq uid }) { it[totalCaughtKg] = 1_000_000.0 } }
        assertEquals(regular[1].id, cycle.moveLocation(uid))
        fishing.setLocation(uid, regular.last().id)
        assertEquals(first.id, cycle.moveLocation(uid))

        val eventId = activeEvent()
        val eventLoc = SpecialEventService().eventLocationId(eventId)!!
        fishing.setLocation(uid, regular.last().id)
        assertEquals(first.id, cycle.moveLocation(uid)) // Event is first but locked outside a club.
        val club = club("Event Crew")
        ClubService().joinClub(uid, club)
        fishing.setLocation(uid, regular.last().id)
        assertEquals(eventLoc, cycle.moveLocation(uid))
        assertEquals(first.id, cycle.moveLocation(uid))
        fishing.setLocation(uid, eventLoc)
        transaction { SpecialEvents.update({ SpecialEvents.id eq eventId }) { it[endTime] = Instant.now().minusSeconds(1) } }
        assertEquals(first.id, cycle.moveLocation(uid))
    }

    @Test
    fun coinRefillUsesStoreDeductsPriceAndRespectsInsufficientBalance() {
        val uid = fishing.createUser()
        fishing.setLocation(uid, fishing.locations(uid).first().id)
        transaction { InventoryLures.update({ InventoryLures.userId eq uid }) { it[qty] = 0 } }
        assertFalse(cycle.prepareBait(uid))
        fishing.addCoins(uid, 1_000)
        assertTrue(cycle.prepareBait(uid))
        val remaining = transaction { Users.select { Users.id eq uid }.single()[Users.coins] }
        assertEquals(685L, remaining) // Cheapest compatible pack: micro_pred_fresh, 315 coins.
        assertTrue(cycle.prepareBait(uid))
        transaction { assertEquals(remaining, Users.select { Users.id eq uid }.single()[Users.coins]) }
        assertTrue(fishing.listLures(uid).any { it.qty > 0 })
    }

    @Test
    fun rewardsAreGrantedOnceIncludingAchievementPackagesAndRatingCoins() {
        val uid = fishing.createUser()
        val fishId = transaction { Fish.selectAll().first()[Fish.id].value }
        val location = fishing.locations(uid).first().id
        transaction {
            RatingPrizes.insert {
                it[userId] = uid
                it[prizeDate] = LocalDate.now().minusDays(1)
                it[locationId] = location
                it[rank] = 1
                it[coins] = 100
                it[claimed] = false
            }
            repeat(10) {
                Catches.insert {
                    it[userId] = uid
                    it[Catches.fishId] = fishId
                    it[locationId] = location
                    it[weight] = 1.0
                    it[createdAt] = Instant.now()
                }
            }
        }
        val achievements = AchievementService.list(uid, "ru").filter { it.claimable }
        assertTrue(achievements.isNotEmpty())
        cycle.claimRewards(uid)
        val lures = fishing.listLures(uid).associate { it.id to it.qty }
        val coins = transaction { Users.select { Users.id eq uid }.single()[Users.coins] }
        assertTrue(coins >= 100)
        assertFalse(fishing.canClaimDaily(uid))
        assertTrue(AchievementService.list(uid, "ru").none { it.claimable })
        cycle.claimRewards(uid)
        assertEquals(lures, fishing.listLures(uid).associate { it.id to it.qty })
        transaction { assertEquals(coins, Users.select { Users.id eq uid }.single()[Users.coins]) }
    }

    @Test
    fun fullCycleRecordsRealCatchInTournamentAndEventAndSkipsFreshCast() = runBlocking {
        val uid = fishing.createUser()
        val clubId = club("Competitors")
        val event = activeEvent()
        val start = Instant.now().minusSeconds(60)
        val tournament = TournamentService().createTournament("Test", "Test", start, start.plusSeconds(3_600), null, null, "count", 1, "[]")
        assertEquals(GameCycle.Result.CAUGHT, cycle.run(uid))
        transaction {
            assertEquals(clubId, ClubMembers.select { ClubMembers.userId eq uid }.single()[ClubMembers.clubId].value)
            assertEquals(1L, Catches.select { Catches.userId eq uid }.count())
            assertEquals(1L, SpecialEventCatches.select { SpecialEventCatches.userId eq uid }.count())
            assertEquals(event, SpecialEventCatches.selectAll().single()[SpecialEventCatches.eventId].value)
            assertFalse(Users.select { Users.id eq uid }.single()[Users.isCasting])
        }
        val t = TournamentService().getTournament(tournament)!!
        assertEquals(uid, TournamentService().leaderboard(t, uid, 1).first.single().userId)
        fishing.startCast(uid)
        assertEquals(GameCycle.Result.BUSY, cycle.run(uid))
        transaction { assertEquals(1L, Catches.select { Catches.userId eq uid }.count()) }
    }

    @Test
    fun staleInterruptedCastIsRecoveredOnlyForTheProcessedAccount() = runBlocking {
        val uid = fishing.createUser()
        val other = fishing.createUser()
        fishing.startCast(uid)
        fishing.startCast(other)
        transaction { Users.update({ Users.id eq uid }) { it[lastCastAt] = Instant.now().minusSeconds(120) } }
        assertEquals(GameCycle.Result.CAUGHT, cycle.run(uid))
        transaction { assertTrue(Users.select { Users.id eq other }.single()[Users.isCasting]) }
    }

    private fun club(name: String, open: Boolean = true, minKg: Double = 0.0): Long = transaction {
        val president = fishing.createUser()
        Clubs.insertAndGetId {
            it[Clubs.name] = name
            it[presidentId] = president
            it[createdAt] = Instant.now()
            it[recruitingOpen] = open
            it[minJoinWeightKg] = minKg
        }.value
    }

    private fun activeEvent(): Long {
        val start = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant()
        val fish = transaction { Fish.selectAll().first()[Fish.id].value }
        val none = SpecialEventPrizeConfig(0, "[]")
        return SpecialEventService().createEvent("Тестовый залив", "Test Bay", start, start.plusSeconds(86_400), null,
            CastZoneDTO(listOf(CastZonePointDTO(.1, .1), CastZonePointDTO(.9, .1), CastZonePointDTO(.5, .9))),
            listOf(SpecialEventFishSpec(fish, 1.0)), none, none, none)
    }
}
