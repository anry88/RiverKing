package service

import db.Catches
import db.ClubMembers
import db.Clubs
import db.Fish
import db.Locations
import db.Users
import db.DB
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    @Test
    fun memberChatMessagesAreStructuredMaskedAndPaged() {
        DB.init(testEnv("club-chat-member-messages"))

        val fishing = FishingService()
        val clubs = ClubService()
        val presidentId = fishing.ensureUserByTgId(2_001L)

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
            Users.update({ Users.id eq presidentId }) { it[Users.nickname] = "Anry" }
            Catches.insert {
                it[userId] = presidentId
                it[Catches.fishId] = fishId
                it[weight] = ClubService.MIN_CREATE_WEIGHT_KG + 10.0
                it[Catches.locationId] = locationId
                it[createdAt] = java.time.Instant.now()
                it[coins] = null
            }
        }
        clubs.createClub(presidentId, "Chat Club")

        repeat(125) { index ->
            val suffix = index + 1
            val text = if (suffix == 1) "hello fuck" else "message $suffix"
            clubs.sendChatMessage(presidentId, text)
        }

        val latest = clubs.clubChat(presidentId)
        assertEquals(ClubService.CHAT_LIMIT, latest.size)
        assertEquals("message 26", latest.first().memberText())
        assertEquals("message 125", latest.last().memberText())

        val older = clubs.clubChat(presidentId, beforeId = latest.first().id, limit = 20)
        assertEquals(20, older.size)
        assertEquals("message 6", older.first().memberText())
        assertEquals("message 25", older.last().memberText())

        val firstMessage = clubs.clubChat(presidentId, beforeId = older.first().id, limit = 10).first()
        val payload = firstMessage.payload()
        assertEquals("clubChatMemberMessage", payload["key"]?.jsonPrimitive?.content)
        val params = assertNotNull(payload["params"]?.jsonObject)
        assertEquals("Anry", params["sender"]?.jsonPrimitive?.content)
        assertEquals(ClubService.ROLE_PRESIDENT, params["rank"]?.jsonPrimitive?.content)
        assertEquals("hello ****", params["text"]?.jsonPrimitive?.content)
    }

    private fun ClubService.ClubChatMessage.payload() =
        Json.parseToJsonElement(message).jsonObject

    private fun ClubService.ClubChatMessage.memberText(): String =
        payload()["params"]?.jsonObject?.get("text")?.jsonPrimitive?.content.orEmpty()
}
