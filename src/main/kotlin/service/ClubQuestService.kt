package service

import db.Catches
import db.ClubMembers
import db.ClubQuestMemberProgress
import db.ClubQuestProgress
import db.ClubQuestRewardRecipients
import db.Fish
import db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import util.Metrics
import util.Rng
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

class ClubQuestService {
    data class UpdateResult(
        val coinsAwardedToUser: Int = 0,
        val progressChanged: Boolean = false,
    )

    private data class CatchContext(
        val fishName: String,
        val rarity: String,
    )

    private sealed class QuestRule {
        abstract fun initialProgress(clubId: Long, periodStart: LocalDate): Int
        abstract fun initialProgressByMember(clubId: Long, periodStart: LocalDate): Map<Long, Int>
        abstract fun updatedProgress(current: Int, context: CatchContext, clubId: Long, periodStart: LocalDate): Int
    }

    private class RarityCountRule(private val rarities: Set<String>) : QuestRule() {
        override fun initialProgress(clubId: Long, periodStart: LocalDate): Int =
            Companion.countByRarity(clubId, periodStart, rarities)

        override fun initialProgressByMember(clubId: Long, periodStart: LocalDate): Map<Long, Int> =
            Companion.countByRarityByMember(clubId, periodStart, rarities)

        override fun updatedProgress(current: Int, context: CatchContext, clubId: Long, periodStart: LocalDate): Int {
            return if (context.rarity in rarities) current + 1 else current
        }
    }

    private class FishCountRule(private val fishNames: Set<String>) : QuestRule() {
        override fun initialProgress(clubId: Long, periodStart: LocalDate): Int =
            Companion.countByFishNames(clubId, periodStart, fishNames)

        override fun initialProgressByMember(clubId: Long, periodStart: LocalDate): Map<Long, Int> =
            Companion.countByFishNamesByMember(clubId, periodStart, fishNames)

        override fun updatedProgress(current: Int, context: CatchContext, clubId: Long, periodStart: LocalDate): Int {
            return if (context.fishName in fishNames) current + 1 else current
        }
    }

    private data class QuestDefinition(
        val code: String,
        val nameRu: String,
        val nameEn: String,
        val descRu: String,
        val descEn: String,
        val target: Int,
        val rewardCoins: Int,
        val rule: QuestRule,
    ) {
        fun name(lang: String) = if (lang.startsWith("en", ignoreCase = true)) nameEn else nameRu
        fun description(lang: String) = if (lang.startsWith("en", ignoreCase = true)) descEn else descRu
        fun initialProgress(clubId: Long, periodStart: LocalDate): Int =
            min(target, rule.initialProgress(clubId, periodStart))

        fun initialProgressByMember(clubId: Long, periodStart: LocalDate): Map<Long, Int> =
            rule.initialProgressByMember(clubId, periodStart)
                .mapValues { (_, progress) -> min(target, progress) }

        fun updatedProgress(current: Int, context: CatchContext, clubId: Long, periodStart: LocalDate): Int =
            min(target, rule.updatedProgress(current, context, clubId, periodStart))
    }

    companion object {
        private const val CLUB_PERIOD = "club"
        private val CLUB_ZONE: ZoneId = ZoneId.of("Europe/Belgrade")

        private val definitions: List<QuestDefinition> = listOf(
            QuestDefinition(
                code = "club_epic_20",
                nameRu = "Эпический улов клуба",
                nameEn = "Club Epic Catch",
                descRu = "Клубом поймайте 20 эпических рыб.",
                descEn = "Catch 20 epic fish as a club.",
                target = 20,
                rewardCoins = 5000,
                rule = RarityCountRule(setOf("epic")),
            ),
            QuestDefinition(
                code = "club_common_200",
                nameRu = "Общий простой улов",
                nameEn = "Shared Common Catch",
                descRu = "Клубом поймайте 200 простых рыб.",
                descEn = "Catch 200 common fish as a club.",
                target = 200,
                rewardCoins = 3000,
                rule = RarityCountRule(setOf("common")),
            ),
            QuestDefinition(
                code = "club_uncommon_100",
                nameRu = "Необычный запас",
                nameEn = "Uncommon Stockpile",
                descRu = "Клубом поймайте 100 необычных рыб.",
                descEn = "Catch 100 uncommon fish as a club.",
                target = 100,
                rewardCoins = 3500,
                rule = RarityCountRule(setOf("uncommon")),
            ),
            QuestDefinition(
                code = "club_ruffe_40",
                nameRu = "Ёршовая неделя",
                nameEn = "Ruffe Week",
                descRu = "Клубом поймайте 40 ершей.",
                descEn = "Catch 40 ruffe as a club.",
                target = 40,
                rewardCoins = 4000,
                rule = FishCountRule(setOf("Ёрш")),
            ),
            QuestDefinition(
                code = "club_bream_30",
                nameRu = "Лещовый заход",
                nameEn = "Bream Run",
                descRu = "Клубом поймайте 30 лещей.",
                descEn = "Catch 30 bream as a club.",
                target = 30,
                rewardCoins = 3500,
                rule = FishCountRule(setOf("Лещ")),
            ),
            QuestDefinition(
                code = "club_crucian_50",
                nameRu = "Карасёвый запас",
                nameEn = "Crucian Reserve",
                descRu = "Клубом поймайте 50 карасей.",
                descEn = "Catch 50 crucian carp as a club.",
                target = 50,
                rewardCoins = 3500,
                rule = FishCountRule(setOf("Карась")),
            ),
            QuestDefinition(
                code = "club_roach_50",
                nameRu = "Плотвиный косяк",
                nameEn = "Roach School",
                descRu = "Клубом поймайте 50 плотвиц.",
                descEn = "Catch 50 roach as a club.",
                target = 50,
                rewardCoins = 3500,
                rule = FishCountRule(setOf("Плотва")),
            ),
            QuestDefinition(
                code = "club_rare_50",
                nameRu = "Редкий клубный улов",
                nameEn = "Rare Club Catch",
                descRu = "Клубом поймайте 50 редких рыб.",
                descEn = "Catch 50 rare fish as a club.",
                target = 50,
                rewardCoins = 5000,
                rule = RarityCountRule(setOf("rare")),
            ),
            QuestDefinition(
                code = "club_perch_50",
                nameRu = "Окуневый строй",
                nameEn = "Perch Patrol",
                descRu = "Клубом поймайте 50 окуней.",
                descEn = "Catch 50 perch as a club.",
                target = 50,
                rewardCoins = 3500,
                rule = FishCountRule(setOf("Окунь")),
            ),
            QuestDefinition(
                code = "club_herring_50",
                nameRu = "Сельдяной косяк",
                nameEn = "Herring School",
                descRu = "Клубом поймайте 50 сельдей.",
                descEn = "Catch 50 herring as a club.",
                target = 50,
                rewardCoins = 4500,
                rule = FishCountRule(setOf("Сельдь")),
            ),
        )

        private fun memberIds(clubId: Long): List<Long> {
            return ClubMembers
                .slice(ClubMembers.userId)
                .select { ClubMembers.clubId eq clubId }
                .map { it[ClubMembers.userId].value }
        }

        private fun periodStartInstant(periodStart: LocalDate): Instant =
            periodStart.atStartOfDay(CLUB_ZONE).toInstant()

        private fun countByRarity(clubId: Long, periodStart: LocalDate, rarities: Set<String>): Int {
            val members = memberIds(clubId)
            if (members.isEmpty()) return 0
            val countExpr = Catches.id.count()
            return (Catches innerJoin Fish)
                .slice(countExpr)
                .select {
                    (Catches.userId inList members) and
                        (Catches.createdAt greaterEq periodStartInstant(periodStart)) and
                        (Fish.rarity inList rarities.toList())
                }
                .single()[countExpr]
                .toInt()
        }

        private fun countByRarityByMember(clubId: Long, periodStart: LocalDate, rarities: Set<String>): Map<Long, Int> {
            val members = memberIds(clubId)
            if (members.isEmpty()) return emptyMap()
            val countExpr = Catches.id.count()
            return (Catches innerJoin Fish)
                .slice(Catches.userId, countExpr)
                .select {
                    (Catches.userId inList members) and
                        (Catches.createdAt greaterEq periodStartInstant(periodStart)) and
                        (Fish.rarity inList rarities.toList())
                }
                .groupBy(Catches.userId)
                .associate { row ->
                    row[Catches.userId].value to row[countExpr].toInt()
                }
        }

        private fun countByFishNames(clubId: Long, periodStart: LocalDate, fishNames: Set<String>): Int {
            val members = memberIds(clubId)
            if (members.isEmpty()) return 0
            val countExpr = Catches.id.count()
            return (Catches innerJoin Fish)
                .slice(countExpr)
                .select {
                    (Catches.userId inList members) and
                        (Catches.createdAt greaterEq periodStartInstant(periodStart)) and
                        (Fish.name inList fishNames.toList())
                }
                .single()[countExpr]
                .toInt()
        }

        private fun countByFishNamesByMember(clubId: Long, periodStart: LocalDate, fishNames: Set<String>): Map<Long, Int> {
            val members = memberIds(clubId)
            if (members.isEmpty()) return emptyMap()
            val countExpr = Catches.id.count()
            return (Catches innerJoin Fish)
                .slice(Catches.userId, countExpr)
                .select {
                    (Catches.userId inList members) and
                        (Catches.createdAt greaterEq periodStartInstant(periodStart)) and
                        (Fish.name inList fishNames.toList())
                }
                .groupBy(Catches.userId)
                .associate { row ->
                    row[Catches.userId].value to row[countExpr].toInt()
                }
        }
    }

    private inline fun <T> inTxn(crossinline block: () -> T): T {
        val current = TransactionManager.currentOrNull()
        return if (current == null) transaction { block() } else block()
    }

    fun list(userId: Long, lang: String): QuestService.ClubQuestSectionDTO = inTxn {
        val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull()
            ?: return@inTxn QuestService.ClubQuestSectionDTO(
                available = false,
                message = if (lang == "ru") {
                    "Вступи в клуб, чтобы открыть клубные задания."
                } else {
                    "Join a club to unlock club quests."
                },
                quests = emptyList(),
            )
        val clubId = membership[ClubMembers.clubId].value
        val start = weekStart(LocalDate.now(CLUB_ZONE))
        ensureCurrentQuests(clubId, start)
        val quests = ClubQuestProgress
            .select {
                (ClubQuestProgress.clubId eq clubId) and
                    (ClubQuestProgress.periodStart eq start)
            }
            .orderBy(ClubQuestProgress.createdAt)
            .mapNotNull { row ->
                val def = definitionFor(row[ClubQuestProgress.code]) ?: return@mapNotNull null
                QuestService.QuestDTO(
                    code = def.code,
                    period = CLUB_PERIOD,
                    name = def.name(lang),
                    description = def.description(lang),
                    progress = row[ClubQuestProgress.progress],
                    target = row[ClubQuestProgress.target],
                    rewardCoins = row[ClubQuestProgress.rewardCoins],
                    completed = row[ClubQuestProgress.completedAt] != null,
                )
            }
        QuestService.ClubQuestSectionDTO(
            available = true,
            message = null,
            quests = quests,
        )
    }

    fun weekView(
        clubId: Long,
        periodStart: LocalDate,
        lang: String,
        roster: List<ClubService.ClubMemberContribution>,
        ensureAssigned: Boolean,
    ): ClubService.ClubQuestWeekView = inTxn {
        if (ensureAssigned) {
            ensureCurrentQuests(clubId, periodStart)
        }
        val progressByCodeAndUser = ClubQuestMemberProgress
            .select {
                (ClubQuestMemberProgress.clubId eq clubId) and
                    (ClubQuestMemberProgress.periodStart eq periodStart)
            }
            .groupBy { it[ClubQuestMemberProgress.code] }
            .mapValues { (_, rows) ->
                rows.associate { row ->
                    row[ClubQuestMemberProgress.userId].value to row[ClubQuestMemberProgress.progress]
                }
            }

        val quests = ClubQuestProgress
            .select {
                (ClubQuestProgress.clubId eq clubId) and
                    (ClubQuestProgress.periodStart eq periodStart)
            }
            .orderBy(ClubQuestProgress.createdAt)
            .mapNotNull { row ->
                val def = definitionFor(row[ClubQuestProgress.code]) ?: return@mapNotNull null
                val memberProgress = progressByCodeAndUser[def.code].orEmpty()
                val members = roster
                    .map { member ->
                        ClubService.ClubQuestMemberView(
                            userId = member.userId,
                            name = member.name,
                            role = member.role,
                            progress = memberProgress[member.userId] ?: 0,
                        )
                    }
                    .sortedWith(
                        compareByDescending<ClubService.ClubQuestMemberView> { it.progress }
                            .thenBy { roleRank(it.role) }
                            .thenBy { it.name ?: "" }
                            .thenBy { it.userId }
                    )
                ClubService.ClubQuestView(
                    code = def.code,
                    name = def.name(lang),
                    description = def.description(lang),
                    progress = row[ClubQuestProgress.progress],
                    target = row[ClubQuestProgress.target],
                    rewardCoins = row[ClubQuestProgress.rewardCoins],
                    completed = row[ClubQuestProgress.completedAt] != null,
                    members = members,
                )
            }
        ClubService.ClubQuestWeekView(
            weekStart = periodStart,
            quests = quests,
        )
    }

    fun updateOnCatch(
        userId: Long,
        fishName: String,
        rarity: String,
        caughtAt: Instant = Instant.now(),
    ): UpdateResult = inTxn {
        val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull()
            ?: return@inTxn UpdateResult()
        val clubId = membership[ClubMembers.clubId].value
        val start = weekStart(ZonedDateTime.ofInstant(caughtAt, CLUB_ZONE).toLocalDate())
        val existingCodes = ClubQuestProgress
            .slice(ClubQuestProgress.code)
            .select {
                (ClubQuestProgress.clubId eq clubId) and
                    (ClubQuestProgress.periodStart eq start)
            }
            .mapTo(mutableSetOf()) { it[ClubQuestProgress.code] }
        val createdCodes = ensureCurrentQuests(clubId, start)
        val context = CatchContext(fishName = fishName, rarity = rarity)
        val now = Instant.now()
        var coinsAwardedToUser = 0
        var progressChanged = createdCodes.isNotEmpty()

        ClubQuestProgress.select {
            (ClubQuestProgress.clubId eq clubId) and
                (ClubQuestProgress.periodStart eq start)
        }.forEach { row ->
            val code = row[ClubQuestProgress.code]
            val def = definitionFor(code) ?: return@forEach
            val completedAt = row[ClubQuestProgress.completedAt]
            if (code in createdCodes && code !in existingCodes) {
                if (completedAt != null) {
                    coinsAwardedToUser += grantReward(clubId, def.code, start, def.rewardCoins, userId, now)
                }
                return@forEach
            }

            val current = row[ClubQuestProgress.progress]
            val updated = def.updatedProgress(current, context, clubId, start)
            if (updated != current) {
                progressChanged = true
                ClubQuestProgress.update({
                    (ClubQuestProgress.clubId eq clubId) and
                        (ClubQuestProgress.code eq def.code) and
                        (ClubQuestProgress.periodStart eq start)
                }) {
                    it[ClubQuestProgress.progress] = updated
                    it[ClubQuestProgress.updatedAt] = now
                }
                incrementMemberProgress(clubId, def.code, start, userId, now)
            }
            if (updated >= def.target && completedAt == null) {
                val marked = ClubQuestProgress.update({
                    (ClubQuestProgress.clubId eq clubId) and
                        (ClubQuestProgress.code eq def.code) and
                        (ClubQuestProgress.periodStart eq start) and
                        ClubQuestProgress.completedAt.isNull()
                }) {
                    it[ClubQuestProgress.completedAt] = now
                    it[ClubQuestProgress.updatedAt] = now
                }
                if (marked > 0) {
                    progressChanged = true
                    coinsAwardedToUser += grantReward(clubId, def.code, start, def.rewardCoins, userId, now)
                }
            }
        }

        UpdateResult(
            coinsAwardedToUser = coinsAwardedToUser,
            progressChanged = progressChanged,
        )
    }

    private fun definitionFor(code: String): QuestDefinition? = definitions.find { it.code == code }

    private fun ensureCurrentQuests(clubId: Long, periodStart: LocalDate): Set<String> {
        val existing = ClubQuestProgress
            .slice(ClubQuestProgress.code)
            .select {
                (ClubQuestProgress.clubId eq clubId) and
                    (ClubQuestProgress.periodStart eq periodStart)
            }
            .mapTo(mutableSetOf()) { it[ClubQuestProgress.code] }
        if (existing.isNotEmpty()) return emptySet()

        val selected = definitions.shuffled(Rng.fast()).take(2)
        val now = Instant.now()
        selected.forEach { def ->
            val initial = def.initialProgress(clubId, periodStart)
            val byMember = def.initialProgressByMember(clubId, periodStart)
            ClubQuestProgress.insert {
                it[ClubQuestProgress.clubId] = clubId
                it[ClubQuestProgress.code] = def.code
                it[ClubQuestProgress.periodStart] = periodStart
                it[ClubQuestProgress.progress] = initial
                it[ClubQuestProgress.target] = def.target
                it[ClubQuestProgress.rewardCoins] = def.rewardCoins
                it[ClubQuestProgress.completedAt] = if (initial >= def.target) now else null
                it[ClubQuestProgress.createdAt] = now
                it[ClubQuestProgress.updatedAt] = now
            }
            byMember.forEach { (userId, progress) ->
                if (progress > 0) {
                    ClubQuestMemberProgress.insert {
                        it[ClubQuestMemberProgress.clubId] = clubId
                        it[ClubQuestMemberProgress.code] = def.code
                        it[ClubQuestMemberProgress.periodStart] = periodStart
                        it[ClubQuestMemberProgress.userId] = userId
                        it[ClubQuestMemberProgress.progress] = progress
                        it[ClubQuestMemberProgress.updatedAt] = now
                    }
                }
            }
        }
        return selected.mapTo(linkedSetOf()) { it.code }
    }

    private fun incrementMemberProgress(
        clubId: Long,
        code: String,
        periodStart: LocalDate,
        userId: Long,
        now: Instant,
    ) {
        val existing = ClubQuestMemberProgress.select {
            (ClubQuestMemberProgress.clubId eq clubId) and
                (ClubQuestMemberProgress.code eq code) and
                (ClubQuestMemberProgress.periodStart eq periodStart) and
                (ClubQuestMemberProgress.userId eq userId)
        }.singleOrNull()
        if (existing == null) {
            ClubQuestMemberProgress.insert {
                it[ClubQuestMemberProgress.clubId] = clubId
                it[ClubQuestMemberProgress.code] = code
                it[ClubQuestMemberProgress.periodStart] = periodStart
                it[ClubQuestMemberProgress.userId] = userId
                it[ClubQuestMemberProgress.progress] = 1
                it[ClubQuestMemberProgress.updatedAt] = now
            }
        } else {
            ClubQuestMemberProgress.update({
                (ClubQuestMemberProgress.clubId eq clubId) and
                    (ClubQuestMemberProgress.code eq code) and
                    (ClubQuestMemberProgress.periodStart eq periodStart) and
                    (ClubQuestMemberProgress.userId eq userId)
            }) {
                it[ClubQuestMemberProgress.progress] = existing[ClubQuestMemberProgress.progress] + 1
                it[ClubQuestMemberProgress.updatedAt] = now
            }
        }
    }

    private fun grantReward(
        clubId: Long,
        code: String,
        periodStart: LocalDate,
        rewardCoins: Int,
        currentUserId: Long,
        completedAt: Instant,
    ): Int {
        val members = ClubMembers
            .slice(ClubMembers.userId)
            .select { ClubMembers.clubId eq clubId }
            .map { it[ClubMembers.userId].value }
        if (members.isEmpty()) return 0

        val perMember = rewardCoins / members.size
        members.forEach { memberId ->
            val currentBalance = Users.select { Users.id eq memberId }.single()[Users.coins]
            Users.update({ Users.id eq memberId }) {
                it[Users.coins] = currentBalance + perMember
            }
            ClubQuestRewardRecipients.insert {
                it[ClubQuestRewardRecipients.clubId] = clubId
                it[ClubQuestRewardRecipients.code] = code
                it[ClubQuestRewardRecipients.periodStart] = periodStart
                it[ClubQuestRewardRecipients.userId] = memberId
                it[ClubQuestRewardRecipients.rewardCoins] = perMember
                it[ClubQuestRewardRecipients.createdAt] = completedAt
            }
        }
        Metrics.counter("quests_complete_total", mapOf("period" to CLUB_PERIOD))
        Metrics.counter("club_quests_complete_total", mapOf("code" to code))
        return if (currentUserId in members) perMember else 0
    }

    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun roleRank(role: String): Int = when (role) {
        ClubService.ROLE_PRESIDENT -> 0
        ClubService.ROLE_HEIR -> 1
        ClubService.ROLE_VETERAN -> 2
        ClubService.ROLE_NOVICE -> 3
        else -> 9
    }

}
