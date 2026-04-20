package service

import db.Catches
import db.ClubMembers
import db.ClubQuestRewardRecipients
import db.DB
import db.Fish
import db.Locations
import db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import support.testEnv
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import util.Metrics

class QuestServiceTest {
    @Test
    fun `fish based quests unlock only after matching locations become available`() {
        DB.init(testEnv("quest-availability"))

        val fishing = FishingService()
        val userId = fishing.ensureUserByTgId(7_001L)

        val initialDaily = QuestService.availableQuestCodes(userId, QuestService.QuestPeriod.DAILY)
        val initialWeekly = QuestService.availableQuestCodes(userId, QuestService.QuestPeriod.WEEKLY)
        assertTrue("daily_roach_3" in initialDaily)
        assertTrue("daily_crucian_3" in initialDaily)
        assertTrue("daily_bleak_3" in initialDaily)
        assertTrue("daily_herring_3" !in initialDaily)
        assertTrue("weekly_zander" !in initialWeekly)
        assertTrue("weekly_piranha_hunt" !in initialWeekly)
        assertTrue("weekly_pacu_hunt" !in initialWeekly)

        addProgressWeight(userId, 25.0)
        val riverWeekly = QuestService.availableQuestCodes(userId, QuestService.QuestPeriod.WEEKLY)
        assertTrue("weekly_zander" in riverWeekly)

        addProgressWeight(userId, 700.0)
        val seaDaily = QuestService.availableQuestCodes(userId, QuestService.QuestPeriod.DAILY)
        val seaWeekly = QuestService.availableQuestCodes(userId, QuestService.QuestPeriod.WEEKLY)
        assertTrue("daily_herring_3" in seaDaily)
        assertTrue("daily_sardine_3" in seaDaily)
        assertTrue("daily_sprat_3" in seaDaily)
        assertTrue("weekly_herring_10" in seaWeekly)
        assertTrue("weekly_sardine_10" in seaWeekly)
        assertTrue("weekly_sprat_10" in seaWeekly)

        addProgressWeight(userId, 1000.0)
        val amazonWeekly = QuestService.availableQuestCodes(userId, QuestService.QuestPeriod.WEEKLY)
        assertTrue("weekly_piranha_hunt" in amazonWeekly)
        assertTrue("weekly_pacu_hunt" in amazonWeekly)
    }

    @Test
    fun `club quests stay weekly stable and reward current roster on completion`() {
        DB.init(testEnv("club-quests"))

        val fishing = FishingService()
        val clubs = ClubService()
        val clubQuests = ClubQuestService()

        val presidentId = fishing.ensureUserByTgId(8_001L)
        val veteranId = fishing.ensureUserByTgId(8_002L)
        val leaverId = fishing.ensureUserByTgId(8_003L)

        transaction {
            listOf(
                presidentId to "president",
                veteranId to "veteran",
                leaverId to "leaver",
            ).forEach { (userId, username) ->
                Users.update({ Users.id eq userId }) { it[Users.username] = username }
            }
        }

        fishing.addCoins(presidentId, ClubService.CREATE_COST_COINS.toInt())
        addProgressWeight(presidentId, ClubService.MIN_CREATE_WEIGHT_KG + 100.0)
        allClubQuestCodes.forEachIndexed { index, code ->
            recordCatchOnly(presidentId, questScenario(code), currentClubWeekInstant().plusSeconds(index.toLong()))
        }

        clubs.createClub(presidentId, "Quest Club")
        val clubId = findClubId(presidentId)
        clubs.joinClub(veteranId, clubId)
        clubs.joinClub(leaverId, clubId)

        val firstList = clubQuests.list(presidentId, "ru")
        val secondList = clubQuests.list(presidentId, "ru")
        assertTrue(firstList.available)
        assertEquals(2, firstList.quests.size)
        assertEquals(firstList.quests.map { it.code }, secondList.quests.map { it.code })
        firstList.quests.forEach { quest ->
            assertEquals(
                0,
                quest.progress,
                "Club quest ${quest.code} must not include fish caught before the club was created.",
            )
        }

        val chosenQuest = firstList.quests.first()
        val scenario = questScenario(chosenQuest.code)
        val initialWeekView = clubs.clubDetails(presidentId)?.currentQuestWeek
            ?.quests
            ?.single { it.code == chosenQuest.code }
            ?: error("Current club quest week missing before catch updates")
        val leaverUpdate = recordClubCatch(clubQuests, leaverId, scenario, currentClubWeekInstant(dayOffset = 1))
        val veteranUpdate = recordClubCatch(clubQuests, veteranId, scenario, currentClubWeekInstant(dayOffset = 1, secondOffset = 300))
        assertTrue(leaverUpdate.progressChanged)
        assertTrue(veteranUpdate.progressChanged)

        val weekViewBeforeLeave = clubs.clubDetails(presidentId)?.currentQuestWeek
            ?.quests
            ?.single { it.code == chosenQuest.code }
            ?: error("Current club quest week missing")
        assertEquals(initialWeekView.progress + 2, weekViewBeforeLeave.progress)
        assertEquals(
            initialWeekView.members.single { it.userId == presidentId }.progress,
            weekViewBeforeLeave.members.single { it.userId == presidentId }.progress,
        )
        assertEquals(
            initialWeekView.members.single { it.userId == veteranId }.progress + 1,
            weekViewBeforeLeave.members.single { it.userId == veteranId }.progress,
        )
        assertEquals(
            initialWeekView.members.single { it.userId == leaverId }.progress + 1,
            weekViewBeforeLeave.members.single { it.userId == leaverId }.progress,
        )

        clubs.addContribution(leaverId, 75, currentClubWeekInstant(dayOffset = 1, secondOffset = 600))
        clubs.leaveClub(leaverId)

        val weekViewAfterLeave = clubs.clubDetails(presidentId)?.currentQuestWeek
            ?.quests
            ?.single { it.code == chosenQuest.code }
            ?: error("Current club quest week missing after leave")
        assertEquals(listOf(presidentId, veteranId), weekViewAfterLeave.members.map { it.userId }.sorted())

        clubs.joinClub(leaverId, clubId)
        val detailsAfterRejoin = clubs.clubDetails(presidentId) ?: error("Club details missing after rejoin")
        val weekViewAfterRejoin = detailsAfterRejoin.currentQuestWeek.quests
            .single { it.code == chosenQuest.code }
        assertEquals(
            weekViewBeforeLeave.members.single { it.userId == leaverId }.progress,
            weekViewAfterRejoin.members.single { it.userId == leaverId }.progress,
            "Rejoining the same club must preserve current-week club quest progress.",
        )
        assertEquals(
            75,
            detailsAfterRejoin.currentWeek.members.single { it.userId == leaverId }.coins,
            "Rejoining the same club must preserve current-week rating contribution.",
        )
        clubs.leaveClub(leaverId)

        val progressAfterLeave = chosenQuest.target - 2
        repeat(progressAfterLeave) { index ->
            val actorId = if (index % 2 == 0) presidentId else veteranId
            recordClubCatch(
                clubQuests,
                actorId,
                scenario,
                currentClubWeekInstant(dayOffset = 1, secondOffset = 3_600L + index.toLong()),
            )
        }

        val completed = clubQuests.list(presidentId, "ru")
            .quests
            .single { it.code == chosenQuest.code }
        assertTrue(completed.completed)
        assertEquals(chosenQuest.target, completed.progress)

        val expectedShare = chosenQuest.rewardCoins / 2
        val presidentCoins = transaction { Users.select { Users.id eq presidentId }.single()[Users.coins] }
        val veteranCoins = transaction { Users.select { Users.id eq veteranId }.single()[Users.coins] }
        val leaverCoins = transaction { Users.select { Users.id eq leaverId }.single()[Users.coins] }
        assertEquals(expectedShare.toLong(), presidentCoins)
        assertEquals(expectedShare.toLong(), veteranCoins)
        assertEquals(0L, leaverCoins)

        val recipientCount = transaction {
            ClubQuestRewardRecipients.select {
                (ClubQuestRewardRecipients.clubId eq clubId) and
                    (ClubQuestRewardRecipients.code eq chosenQuest.code)
            }.count()
        }
        assertEquals(2L, recipientCount)

        val metrics = Metrics.dump()
        assertTrue(metrics.lineSequence().any { it.startsWith("quests_complete_total{period=\"club\"} ") })
        assertTrue(metrics.lineSequence().any { it.startsWith("club_quests_complete_total{code=\"${chosenQuest.code}\"} ") })
    }

    private data class QuestScenario(
        val fishName: String,
        val rarity: String,
        val locationName: String,
        val weight: Double,
    )

    private fun questScenario(code: String): QuestScenario = when (code) {
        "club_epic_20" -> QuestScenario("Паку бурый", "epic", "Русло Амазонки", 12.0)
        "club_common_200" -> QuestScenario("Уклейка", "common", "Пруд", 0.2)
        "club_uncommon_100" -> QuestScenario("Пелядь", "uncommon", "Пруд", 1.1)
        "club_ruffe_40" -> QuestScenario("Ёрш", "common", "Пруд", 0.2)
        "club_bream_30" -> QuestScenario("Лещ", "uncommon", "Пруд", 1.2)
        "club_crucian_50" -> QuestScenario("Карась", "common", "Пруд", 0.4)
        "club_roach_50" -> QuestScenario("Плотва", "common", "Пруд", 0.25)
        "club_rare_50" -> QuestScenario("Карп", "rare", "Пруд", 2.6)
        "club_perch_50" -> QuestScenario("Окунь", "common", "Пруд", 0.3)
        "club_herring_50" -> QuestScenario("Сельдь", "common", "Прибрежье моря", 0.4)
        else -> error("Unexpected quest code: $code")
    }

    private val allClubQuestCodes = listOf(
        "club_epic_20",
        "club_common_200",
        "club_uncommon_100",
        "club_ruffe_40",
        "club_bream_30",
        "club_crucian_50",
        "club_roach_50",
        "club_rare_50",
        "club_perch_50",
        "club_herring_50",
    )

    private fun currentClubWeekInstant(dayOffset: Long = 0, secondOffset: Long = 0): Instant {
        val zone = ZoneId.of("Europe/Belgrade")
        val weekStart = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return weekStart
            .plusDays(dayOffset)
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .plusSeconds(secondOffset)
    }

    private fun recordCatchOnly(
        userId: Long,
        scenario: QuestScenario,
        at: Instant,
    ) {
        val (fishId, locationId) = transaction {
            val fishId = Fish.select { Fish.name eq scenario.fishName }.single()[Fish.id].value
            val locationId = Locations.select { Locations.name eq scenario.locationName }.single()[Locations.id].value
            fishId to locationId
        }
        transaction {
            Catches.insert {
                it[Catches.userId] = userId
                it[Catches.fishId] = fishId
                it[Catches.weight] = scenario.weight
                it[Catches.locationId] = locationId
                it[Catches.createdAt] = at
                it[Catches.coins] = null
            }
        }
    }

    private fun recordClubCatch(
        clubQuests: ClubQuestService,
        userId: Long,
        scenario: QuestScenario,
        at: Instant,
    ): ClubQuestService.UpdateResult {
        val (fishId, locationId) = transaction {
            val fishId = Fish.select { Fish.name eq scenario.fishName }.single()[Fish.id].value
            val locationId = Locations.select { Locations.name eq scenario.locationName }.single()[Locations.id].value
            fishId to locationId
        }
        transaction {
            Catches.insert {
                it[Catches.userId] = userId
                it[Catches.fishId] = fishId
                it[Catches.weight] = scenario.weight
                it[Catches.locationId] = locationId
                it[Catches.createdAt] = at
                it[Catches.coins] = null
            }
        }
        return clubQuests.updateOnCatch(userId, scenario.fishName, scenario.rarity, at)
    }

    private fun addProgressWeight(userId: Long, weightKg: Double) {
        val (fishId, locationId) = transaction {
            val locationId = Locations.selectAll()
                .orderBy(Locations.id, SortOrder.ASC)
                .limit(1)
                .single()[Locations.id].value
            val fishId = Fish.selectAll()
                .orderBy(Fish.id, SortOrder.ASC)
                .limit(1)
                .single()[Fish.id].value
            fishId to locationId
        }
        transaction {
            Catches.insert {
                it[Catches.userId] = userId
                it[Catches.fishId] = fishId
                it[Catches.weight] = weightKg
                it[Catches.locationId] = locationId
                it[Catches.createdAt] = Instant.now()
                it[Catches.coins] = null
            }
        }
    }

    private fun findClubId(userId: Long): Long = transaction {
        ClubMembers.select { ClubMembers.userId eq userId }.single()[ClubMembers.clubId].value
    }
}
