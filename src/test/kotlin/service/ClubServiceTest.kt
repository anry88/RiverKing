package service

import db.Catches
import db.ClubMembers
import db.Clubs
import db.Fish
import db.Locations
import db.Users
import db.DB
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import support.testEnv

class ClubServiceTest {
    @Test
    fun previousWeekSnapshotAndRewardsStayBoundToPreviousRoster() {
        DB.init(testEnv("club-week-snapshot"))

        val fishing = FishingService()
        val clubs = ClubService()
        val zone = ZoneId.of("Europe/Belgrade")
        val currentWeekStart = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val previousWeekStart = currentWeekStart.minusWeeks(1)
        val previousWeekInstant = previousWeekStart.atStartOfDay(zone).toInstant()
        val previousWeekMid = previousWeekStart.plusDays(2).atStartOfDay(zone).toInstant()

        val presidentId = fishing.ensureUserByTgId(1_001L)
        val veteranId = fishing.ensureUserByTgId(1_002L)
        val newcomerId = fishing.ensureUserByTgId(1_003L)

        val (locationId, fishId) = transaction {
            val location = Locations.selectAll()
                .orderBy(Locations.id, SortOrder.ASC)
                .limit(1)
                .single()[Locations.id].value
            val fish = Fish.selectAll()
                .orderBy(Fish.id, SortOrder.ASC)
                .limit(1)
                .single()[Fish.id].value
            location to fish
        }

        fishing.addCoins(presidentId, ClubService.CREATE_COST_COINS.toInt())
        transaction {
            listOf(
                presidentId to "president",
                veteranId to "veteran",
                newcomerId to "newcomer",
            ).forEach { (userId, username) ->
                Users.update({ Users.id eq userId }) { it[Users.username] = username }
            }
            Catches.insert {
                it[userId] = presidentId
                it[Catches.fishId] = fishId
                it[weight] = ClubService.MIN_CREATE_WEIGHT_KG + 250.0
                it[Catches.locationId] = locationId
                it[createdAt] = previousWeekMid
                it[coins] = null
            }
        }

        val createdClubId = clubs.createClub(presidentId, "Snapshot Club").id

        transaction {
            Clubs.update({ Clubs.id eq createdClubId }) { it[createdAt] = previousWeekInstant }
            ClubMembers.update({ ClubMembers.userId eq presidentId }) { it[joinedAt] = previousWeekInstant }
            ClubMembers.insert {
                it[ClubMembers.clubId] = createdClubId
                it[userId] = veteranId
                it[role] = "novice"
                it[joinedAt] = previousWeekInstant.plusSeconds(3_600)
            }
        }

        clubs.addContribution(presidentId, 100, previousWeekMid)
        clubs.addContribution(veteranId, 50, previousWeekMid.plusSeconds(3_600))

        clubs.leaveClub(veteranId)
        clubs.joinClub(newcomerId, createdClubId)

        val details = clubs.clubDetails(presidentId) ?: error("Club details missing")
        assertEquals(
            listOf(presidentId, newcomerId),
            details.currentWeek.members.map { it.userId },
            "Current week should reflect the current club roster.",
        )
        assertEquals(
            listOf(presidentId, veteranId),
            details.previousWeek.members.map { it.userId },
            "Previous week should stay fixed to the roster before the week rolled over.",
        )
        assertEquals(150, details.previousWeek.totalCoins)

        clubs.distributeWeeklyRewards()

        assertEquals(75, clubs.pendingRewards(presidentId).single().coins)
        assertEquals(75, clubs.pendingRewards(veteranId).single().coins)
        assertTrue(clubs.pendingRewards(newcomerId).isEmpty(), "New current-week members must not receive last week's club reward.")
    }
}
