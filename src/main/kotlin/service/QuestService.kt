package service

import db.Catches
import db.Fish
import db.Locations
import db.QuestProgress
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import util.Metrics
import util.Rng
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

object QuestService {
    enum class QuestPeriod(val code: String) {
        DAILY("daily"),
        WEEKLY("weekly"),
    }

    @Serializable
    data class QuestDTO(
        val code: String,
        val period: String,
        val name: String,
        val description: String,
        val progress: Int,
        val target: Int,
        val rewardCoins: Int,
        val completed: Boolean,
    )

    @Serializable
    data class QuestListDTO(
        val daily: List<QuestDTO>,
        val weekly: List<QuestDTO>,
    )

    @Serializable
    data class QuestUpdate(
        val code: String,
        val period: String,
        val rewardCoins: Int,
        val name: String = "",
    )

    private data class CatchContext(
        val fishName: String,
        val rarity: String,
        val locationId: Long,
        val weight: Double,
    )

    private data class AvailabilityContext(
        val unlockedLocations: Int,
        val hasMountainRiver: Boolean,
    )

    private sealed class QuestRule {
        abstract fun initialProgress(userId: Long, periodStart: LocalDate): Int
        abstract fun updatedProgress(current: Int, context: CatchContext, userId: Long, periodStart: LocalDate): Int
    }

    private class RarityCountRule(private val rarities: Set<String>) : QuestRule() {
        override fun initialProgress(userId: Long, periodStart: LocalDate): Int =
            countByRarity(userId, periodStart, rarities)

        override fun updatedProgress(current: Int, context: CatchContext, userId: Long, periodStart: LocalDate): Int {
            return if (context.rarity in rarities) current + 1 else current
        }
    }

    private class FishCountRule(private val fishName: String) : QuestRule() {
        override fun initialProgress(userId: Long, periodStart: LocalDate): Int =
            countByFish(userId, periodStart, fishName)

        override fun updatedProgress(current: Int, context: CatchContext, userId: Long, periodStart: LocalDate): Int {
            return if (context.fishName == fishName) current + 1 else current
        }
    }

    private object DistinctLocationsRule : QuestRule() {
        override fun initialProgress(userId: Long, periodStart: LocalDate): Int =
            countDistinctLocations(userId, periodStart)

        override fun updatedProgress(current: Int, context: CatchContext, userId: Long, periodStart: LocalDate): Int =
            countDistinctLocations(userId, periodStart)
    }

    private class WeightThresholdRule(
        private val minWeight: Double,
        private val inclusive: Boolean,
    ) : QuestRule() {
        override fun initialProgress(userId: Long, periodStart: LocalDate): Int {
            val count = countByWeight(userId, periodStart, minWeight, inclusive)
            return if (count > 0) 1 else 0
        }

        override fun updatedProgress(current: Int, context: CatchContext, userId: Long, periodStart: LocalDate): Int {
            val meets = if (inclusive) context.weight >= minWeight else context.weight > minWeight
            return if (meets) maxOf(current, 1) else current
        }
    }

    private data class QuestDefinition(
        val code: String,
        val period: QuestPeriod,
        val nameRu: String,
        val nameEn: String,
        val descRu: String,
        val descEn: String,
        val target: Int,
        val rewardCoins: Int,
        val rule: QuestRule,
        val availability: (AvailabilityContext) -> Boolean = { true },
    ) {
        fun name(lang: String) = if (lang.startsWith("en", ignoreCase = true)) nameEn else nameRu
        fun description(lang: String) = if (lang.startsWith("en", ignoreCase = true)) descEn else descRu
        fun initialProgress(userId: Long, periodStart: LocalDate): Int =
            min(target, rule.initialProgress(userId, periodStart))

        fun updatedProgress(current: Int, context: CatchContext, userId: Long, periodStart: LocalDate): Int =
            min(target, rule.updatedProgress(current, context, userId, periodStart))
    }

    private const val MOUNTAIN_RIVER = "Горная река"
    private val zone = ZoneId.of("Europe/Belgrade")

    private val definitions: List<QuestDefinition> = listOf(
        QuestDefinition(
            code = "daily_common_10",
            period = QuestPeriod.DAILY,
            nameRu = "Обычная добыча",
            nameEn = "Common Catch",
            descRu = "Поймайте 10 рыб простой редкости.",
            descEn = "Catch 10 common rarity fish.",
            target = 10,
            rewardCoins = 100,
            rule = RarityCountRule(setOf("common")),
        ),
        QuestDefinition(
            code = "daily_uncommon_3",
            period = QuestPeriod.DAILY,
            nameRu = "Необычный улов",
            nameEn = "Uncommon Catch",
            descRu = "Поймайте 3 рыбы необычной редкости.",
            descEn = "Catch 3 uncommon rarity fish.",
            target = 3,
            rewardCoins = 100,
            rule = RarityCountRule(setOf("uncommon")),
        ),
        QuestDefinition(
            code = "daily_rare_1",
            period = QuestPeriod.DAILY,
            nameRu = "Редкая находка",
            nameEn = "Rare Find",
            descRu = "Поймайте 1 рыбу редкой редкости.",
            descEn = "Catch 1 rare rarity fish.",
            target = 1,
            rewardCoins = 100,
            rule = RarityCountRule(setOf("rare")),
        ),
        QuestDefinition(
            code = "daily_locations_3",
            period = QuestPeriod.DAILY,
            nameRu = "Новые места",
            nameEn = "New Spots",
            descRu = "Поймайте рыбу на 3 разных локациях.",
            descEn = "Catch fish in 3 different locations.",
            target = 3,
            rewardCoins = 100,
            rule = DistinctLocationsRule,
            availability = { it.unlockedLocations >= 3 },
        ),
        QuestDefinition(
            code = "daily_weight_5",
            period = QuestPeriod.DAILY,
            nameRu = "Весомый улов",
            nameEn = "Hefty Catch",
            descRu = "Поймайте рыбу весом не менее 5 кг.",
            descEn = "Catch a fish weighing at least 5 kg.",
            target = 1,
            rewardCoins = 150,
            rule = WeightThresholdRule(5.0, inclusive = true),
        ),
        questForFish(
            code = "daily_bream",
            period = QuestPeriod.DAILY,
            fishName = "Лещ",
            nameRu = "Лещ на крючке",
            nameEn = "Bream on the Hook",
            rewardCoins = 100,
        ),
        questForFish(
            code = "daily_ide",
            period = QuestPeriod.DAILY,
            fishName = "Язь",
            nameRu = "Язь дня",
            nameEn = "Ide of the Day",
            rewardCoins = 100,
        ),
        questForFish(
            code = "daily_peled",
            period = QuestPeriod.DAILY,
            fishName = "Пелядь",
            nameRu = "Пелядь дня",
            nameEn = "Peled of the Day",
            rewardCoins = 100,
        ),
        questForFish(
            code = "daily_tench",
            period = QuestPeriod.DAILY,
            fishName = "Линь",
            nameRu = "Линь на берегу",
            nameEn = "Tench on the Bank",
            rewardCoins = 100,
        ),
        QuestDefinition(
            code = "weekly_epic_plus",
            period = QuestPeriod.WEEKLY,
            nameRu = "Эпическая удача",
            nameEn = "Epic Luck",
            descRu = "Поймайте 1 рыбу эпической редкости или выше.",
            descEn = "Catch 1 epic or higher rarity fish.",
            target = 1,
            rewardCoins = 1000,
            rule = RarityCountRule(setOf("epic", "mythic", "legendary")),
        ),
        QuestDefinition(
            code = "weekly_common_50",
            period = QuestPeriod.WEEKLY,
            nameRu = "Обычный марафон",
            nameEn = "Common Marathon",
            descRu = "Поймайте 50 рыб обычной редкости.",
            descEn = "Catch 50 common rarity fish.",
            target = 50,
            rewardCoins = 1000,
            rule = RarityCountRule(setOf("common")),
        ),
        QuestDefinition(
            code = "weekly_uncommon_10",
            period = QuestPeriod.WEEKLY,
            nameRu = "Необычная неделя",
            nameEn = "Uncommon Week",
            descRu = "Поймайте 10 рыб необычной редкости.",
            descEn = "Catch 10 uncommon rarity fish.",
            target = 10,
            rewardCoins = 1000,
            rule = RarityCountRule(setOf("uncommon")),
        ),
        QuestDefinition(
            code = "weekly_rare_5",
            period = QuestPeriod.WEEKLY,
            nameRu = "Редкая серия",
            nameEn = "Rare Streak",
            descRu = "Поймайте 5 рыб редкой редкости.",
            descEn = "Catch 5 rare rarity fish.",
            target = 5,
            rewardCoins = 1000,
            rule = RarityCountRule(setOf("rare")),
        ),
        QuestDefinition(
            code = "weekly_locations_10",
            period = QuestPeriod.WEEKLY,
            nameRu = "Путешествие по локациям",
            nameEn = "Location Tour",
            descRu = "Поймайте рыбу на 10 разных локациях.",
            descEn = "Catch fish in 10 different locations.",
            target = 10,
            rewardCoins = 1000,
            rule = DistinctLocationsRule,
            availability = { it.unlockedLocations >= 10 },
        ),
        QuestDefinition(
            code = "weekly_weight_30",
            period = QuestPeriod.WEEKLY,
            nameRu = "Тяжеловес недели",
            nameEn = "Weekly Heavyweight",
            descRu = "Поймайте рыбу весом более 30 кг.",
            descEn = "Catch a fish weighing over 30 kg.",
            target = 1,
            rewardCoins = 1500,
            rule = WeightThresholdRule(30.0, inclusive = false),
            availability = { it.hasMountainRiver },
        ),
        questForFish(
            code = "weekly_pike",
            period = QuestPeriod.WEEKLY,
            fishName = "Щука",
            nameRu = "Щука недели",
            nameEn = "Pike of the Week",
            rewardCoins = 500,
        ),
        questForFish(
            code = "weekly_zander",
            period = QuestPeriod.WEEKLY,
            fishName = "Судак",
            nameRu = "Судак недели",
            nameEn = "Zander of the Week",
            rewardCoins = 500,
        ),
        questForFish(
            code = "weekly_carp",
            period = QuestPeriod.WEEKLY,
            fishName = "Карп",
            nameRu = "Карп недели",
            nameEn = "Carp of the Week",
            rewardCoins = 500,
        ),
    )

    private fun questForFish(
        code: String,
        period: QuestPeriod,
        fishName: String,
        nameRu: String,
        nameEn: String,
        rewardCoins: Int,
    ): QuestDefinition {
        val fishNameEn = I18n.fish(fishName, "en")
        return QuestDefinition(
            code = code,
            period = period,
            nameRu = nameRu,
            nameEn = nameEn,
            descRu = "Поймайте рыбу: $fishName.",
            descEn = "Catch a $fishNameEn.",
            target = 1,
            rewardCoins = rewardCoins,
            rule = FishCountRule(fishName),
        )
    }

    private inline fun <T> inTxn(crossinline block: () -> T): T {
        val current = TransactionManager.currentOrNull()
        return if (current == null) transaction { block() } else block()
    }

    private fun definitionFor(code: String): QuestDefinition? = definitions.find { it.code == code }

    private fun availabilityContext(userId: Long): AvailabilityContext {
        val total = totalKg(userId)
        val unlockedLocations = Locations.select { Locations.unlockKg lessEq total }.count().toInt()
        val hasMountainRiver = Locations.select {
            (Locations.name eq MOUNTAIN_RIVER) and (Locations.unlockKg lessEq total)
        }.limit(1).any()
        return AvailabilityContext(unlockedLocations, hasMountainRiver)
    }

    private fun selectQuests(period: QuestPeriod, context: AvailabilityContext): List<QuestDefinition> {
        val available = definitions.filter { it.period == period && it.availability(context) }
        if (available.size <= 3) return available
        return available.shuffled(Rng.fast()).take(3)
    }

    fun ensureCurrentQuests(userId: Long) = inTxn {
        val context = availabilityContext(userId)
        QuestPeriod.values().forEach { period ->
            val start = periodStart(period)
            val exists = QuestProgress
                .slice(QuestProgress.id)
                .select {
                    (QuestProgress.userId eq userId) and
                        (QuestProgress.period eq period.code) and
                        (QuestProgress.periodStart eq start)
                }
                .limit(1)
                .any()
            if (!exists) {
                val selected = selectQuests(period, context)
                selected.forEach { def ->
                    val initial = def.initialProgress(userId, start)
                    QuestProgress.insert {
                        it[QuestProgress.userId] = userId
                        it[QuestProgress.code] = def.code
                        it[QuestProgress.period] = def.period.code
                        it[QuestProgress.periodStart] = start
                        it[QuestProgress.progress] = initial
                        it[QuestProgress.target] = def.target
                        it[QuestProgress.rewardCoins] = def.rewardCoins
                        it[QuestProgress.updatedAt] = Instant.now()
                    }
                }
            }
        }
    }

    fun list(userId: Long, lang: String): QuestListDTO = inTxn {
        ensureCurrentQuests(userId)
        val dailyStart = periodStart(QuestPeriod.DAILY)
        val weeklyStart = periodStart(QuestPeriod.WEEKLY)
        val daily = listForPeriod(userId, QuestPeriod.DAILY, dailyStart, lang)
        val weekly = listForPeriod(userId, QuestPeriod.WEEKLY, weeklyStart, lang)
        QuestListDTO(daily, weekly)
    }

    private fun listForPeriod(
        userId: Long,
        period: QuestPeriod,
        start: LocalDate,
        lang: String,
    ): List<QuestDTO> {
        return QuestProgress
            .select {
                (QuestProgress.userId eq userId) and
                    (QuestProgress.period eq period.code) and
                    (QuestProgress.periodStart eq start)
            }
            .orderBy(QuestProgress.createdAt)
            .mapNotNull { row ->
                val def = definitionFor(row[QuestProgress.code]) ?: return@mapNotNull null
                QuestDTO(
                    code = def.code,
                    period = def.period.code,
                    name = def.name(lang),
                    description = def.description(lang),
                    progress = row[QuestProgress.progress],
                    target = row[QuestProgress.target],
                    rewardCoins = row[QuestProgress.rewardCoins],
                    completed = row[QuestProgress.completedAt] != null,
                )
            }
    }

    fun updateOnCatch(
        userId: Long,
        fishName: String,
        rarity: String,
        locationId: Long,
        weight: Double,
    ): List<QuestUpdate> = inTxn {
        ensureCurrentQuests(userId)
        val context = CatchContext(fishName, rarity, locationId, weight)
        val now = Instant.now()
        val dailyStart = periodStart(QuestPeriod.DAILY)
        val weeklyStart = periodStart(QuestPeriod.WEEKLY)
        val rows = QuestProgress.select {
            (QuestProgress.userId eq userId) and
                (
                    ((QuestProgress.period eq QuestPeriod.DAILY.code) and (QuestProgress.periodStart eq dailyStart)) or
                        ((QuestProgress.period eq QuestPeriod.WEEKLY.code) and (QuestProgress.periodStart eq weeklyStart))
                    )
        }.toList()

        val completions = mutableListOf<QuestUpdate>()

        rows.forEach { row ->
            val code = row[QuestProgress.code]
            val def = definitionFor(code) ?: return@forEach
            val current = row[QuestProgress.progress]
            val updated = def.updatedProgress(current, context, userId, row[QuestProgress.periodStart])
            val completedAt = row[QuestProgress.completedAt]
            if (updated != current) {
                QuestProgress.update({ QuestProgress.id eq row[QuestProgress.id].value }) {
                    it[QuestProgress.progress] = updated
                    it[QuestProgress.updatedAt] = now
                }
            }
            if (updated >= def.target && completedAt == null) {
                QuestProgress.update({ QuestProgress.id eq row[QuestProgress.id].value }) {
                    it[QuestProgress.completedAt] = now
                    it[QuestProgress.updatedAt] = now
                }
                Metrics.counter("quests_complete_total", mapOf("period" to def.period.code))
                completions.add(
                    QuestUpdate(
                        code = def.code,
                        period = def.period.code,
                        rewardCoins = def.rewardCoins,
                    )
                )
            }
        }

        completions
    }

    fun localizeUpdates(updates: List<QuestUpdate>, lang: String): List<QuestUpdate> {
        return updates.map { update ->
            val def = definitionFor(update.code)
            val name = def?.name(lang) ?: update.code
            update.copy(name = name)
        }
    }

    fun unlockMessages(updates: List<QuestUpdate>, lang: String): List<String> {
        if (updates.isEmpty()) return emptyList()
        return updates.map { update ->
            val def = definitionFor(update.code)
            val name = def?.name(lang) ?: update.code
            val coins = update.rewardCoins
            if (lang == "ru") {
                "Квест выполнен: $name (+$coins монет)"
            } else {
                val suffix = if (coins == 1) "" else "s"
                "Quest completed: $name (+$coins coin$suffix)"
            }
        }
    }

    private fun periodStart(period: QuestPeriod): LocalDate {
        val today = LocalDate.now(zone)
        return when (period) {
            QuestPeriod.DAILY -> today
            QuestPeriod.WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }
    }

    private fun periodStartInstant(periodStart: LocalDate): Instant = periodStart.atStartOfDay(zone).toInstant()

    private fun totalKg(userId: Long): Double {
        val sumExpr = Catches.weight.sum()
        if (sumExpr == null) return 0.0
        return Catches
            .slice(sumExpr)
            .select { Catches.userId eq userId }
            .singleOrNull()
            ?.get(sumExpr) ?: 0.0
    }

    private fun baseCatchCondition(userId: Long, periodStart: LocalDate): Op<Boolean> {
        val start = periodStartInstant(periodStart)
        return (Catches.userId eq userId) and (Catches.createdAt greaterEq start)
    }

    private fun countByRarity(userId: Long, periodStart: LocalDate, rarities: Set<String>): Int {
        val countExpr = Catches.id.count()
        return (Catches innerJoin Fish)
            .slice(countExpr)
            .select { baseCatchCondition(userId, periodStart) and (Fish.rarity inList rarities.toList()) }
            .single()[countExpr]
            ?.toInt() ?: 0
    }

    private fun countByFish(userId: Long, periodStart: LocalDate, fishName: String): Int {
        val countExpr = Catches.id.count()
        return (Catches innerJoin Fish)
            .slice(countExpr)
            .select { baseCatchCondition(userId, periodStart) and (Fish.name eq fishName) }
            .single()[countExpr]
            ?.toInt() ?: 0
    }

    private fun countDistinctLocations(userId: Long, periodStart: LocalDate): Int {
        return Catches
            .slice(Catches.locationId)
            .select { baseCatchCondition(userId, periodStart) }
            .withDistinct()
            .count()
            .toInt()
    }

    private fun countByWeight(
        userId: Long,
        periodStart: LocalDate,
        minWeight: Double,
        inclusive: Boolean,
    ): Int {
        val condition = if (inclusive) {
            Catches.weight greaterEq minWeight
        } else {
            Catches.weight greater minWeight
        }
        val countExpr = Catches.id.count()
        return Catches
            .slice(countExpr)
            .select { baseCatchCondition(userId, periodStart) and condition }
            .single()[countExpr]
            ?.toInt() ?: 0
    }
}
