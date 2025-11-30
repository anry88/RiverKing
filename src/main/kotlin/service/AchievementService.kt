package service

import db.AchievementProgress
import db.Achievements
import db.Catches
import db.Fish
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import util.Metrics
import java.time.Instant

@Serializable
data class AchievementDTO(
    val code: String,
    val name: String,
    val description: String,
    val level: String,
    val levelIndex: Int,
    val progress: Int,
    val target: Int,
    val claimable: Boolean,
)

@Serializable
data class AchievementRewardDTO(
    val code: String,
    val rewards: List<PrizeSpec>,
)

@Serializable
data class AchievementUnlock(
    val code: String,
    val newLevelIndex: Int,
)

object AchievementService {
    private data class AchievementDefinition(
        val code: String,
        val nameRu: String,
        val nameEn: String,
        val descRu: String,
        val descEn: String,
        val thresholds: List<Int>,
        val rewards: List<PrizeSpec>,
        val progress: (Long) -> Int,
    )

    private const val KOI_COLLECTOR_CODE = "koi_collector"
    private const val SIMPLE_FISHER_CODE = "simple_fisher"

    private val koiFishNames = listOf(
        "Карп кои (Кохаку)",
        "Карп кои (Тайсё Сансёку)",
        "Карп кои (Сёва Сансёку)",
        "Карп кои (Уцуримоно)",
        "Карп кои (Бэкко)",
        "Карп кои (Тантё)",
        "Карп кои (Асаги)",
        "Карп кои (Сюсуй)",
        "Карп кои (Коромо)",
        "Карп кои (Кингинрин)",
        "Карп кои (Каваримоно)",
        "Карп кои (Огон)",
        "Карп кои (Хикари-моёмоно)",
        "Карп кои (Госики)",
        "Карп кои (Кумонрю)",
        "Карп кои (Дойцу-гои)",
    )

    private val koiRewards = listOf(
        PrizeSpec(pack = "", qty = 0),
        PrizeSpec(pack = "fresh_topup_s", qty = 1),
        PrizeSpec(pack = "fresh_stock_m", qty = 1),
        PrizeSpec(pack = "fresh_crate_l", qty = 1),
        PrizeSpec(pack = "autofish", qty = 1),
    )

    private val koiThresholds = listOf(0, 1, 3, 8, 16)
    private val simpleFisherThresholds = listOf(0, 10, 100, 500, 1000)
    private val simpleFisherRewards = listOf(
        PrizeSpec(pack = "", qty = 0),
        PrizeSpec(pack = "fresh_topup_s", qty = 1),
        PrizeSpec(pack = "fresh_stock_m", qty = 1),
        PrizeSpec(pack = "fresh_crate_l", qty = 1),
        PrizeSpec(pack = "salt_crate_l", qty = 1),
    )

    private val definitions = listOf(
        AchievementDefinition(
            code = KOI_COLLECTOR_CODE,
            nameRu = "Коллекционер Кои",
            nameEn = "Koi Collector",
            descRu = "Поймайте всех 16 видов карпов кои",
            descEn = "Catch all 16 koi varieties",
            thresholds = koiThresholds,
            rewards = koiRewards,
            progress = ::koiCatchCount,
        ),
        AchievementDefinition(
            code = SIMPLE_FISHER_CODE,
            nameRu = "Простой рыбак",
            nameEn = "Simple Fisher",
            descRu = "Ловите рыбу простой редкости",
            descEn = "Catch common rarity fish",
            thresholds = simpleFisherThresholds,
            rewards = simpleFisherRewards,
            progress = ::commonCatchCount,
        ),
    )

    private inline fun <T> inTxn(crossinline block: () -> T): T {
        val current = TransactionManager.currentOrNull()
        return if (current == null) transaction { block() } else block()
    }

    fun seed() = inTxn {
        definitions.forEach { def ->
            Achievements.insertIgnore { row ->
                row[code] = def.code
                row[nameRu] = def.nameRu
                row[nameEn] = def.nameEn
                row[descRu] = def.descRu
                row[descEn] = def.descEn
            }
        }
    }

    private fun definitionFor(code: String): AchievementDefinition? = definitions.find { it.code == code }

    private fun achievementId(definition: AchievementDefinition): Long = inTxn {
        Achievements.select { Achievements.code eq definition.code }
            .single()[Achievements.id].value
    }

    private fun levelIndex(thresholds: List<Int>, progress: Int): Int {
        var level = 0
        thresholds.forEachIndexed { index, threshold ->
            if (progress >= threshold) level = index
        }
        return level
    }

    private fun ensureProgressRow(userId: Long, definition: AchievementDefinition): Long = inTxn {
        val achievementId = achievementId(definition)
        val existing = AchievementProgress.select {
            (AchievementProgress.userId eq userId) and (AchievementProgress.achievementId eq achievementId)
        }.singleOrNull()
        if (existing != null) return@inTxn existing[AchievementProgress.id]
        AchievementProgress.insertIgnore { row ->
            row[AchievementProgress.userId] = userId
            row[AchievementProgress.achievementId] = achievementId
            row[level] = 0
            row[claimedLevel] = 0
            row[updatedAt] = Instant.now()
        } get AchievementProgress.id
    }.value

    private fun progress(
        userId: Long,
        language: String,
        definition: AchievementDefinition,
    ): AchievementDTO {
        val langIsRu = language.lowercase().startsWith("ru")
        val progressValue = definition.progress(userId)
        val level = levelIndex(definition.thresholds, progressValue)
        val target = definition.thresholds.getOrNull(level + 1) ?: definition.thresholds.last()
        val rowId = ensureProgressRow(userId, definition)
        val progressRow = inTxn {
            AchievementProgress.select { AchievementProgress.id eq rowId }.singleOrNull()
        }
        val claimed = progressRow?.get(AchievementProgress.claimedLevel) ?: 0
        val storedLevel = progressRow?.get(AchievementProgress.level) ?: 0
        if (level > storedLevel) {
            inTxn {
                AchievementProgress.update({ AchievementProgress.id eq rowId }) {
                    it[AchievementProgress.level] = level
                    it[AchievementProgress.updatedAt] = Instant.now()
                }
            }
        }
        return AchievementDTO(
            code = definition.code,
            name = if (langIsRu) definition.nameRu else definition.nameEn,
            description = if (langIsRu) definition.descRu else definition.descEn,
            level = levelLabel(level, langIsRu),
            levelIndex = level,
            progress = progressValue,
            target = target,
            claimable = level > claimed,
        )
    }

    fun list(userId: Long, language: String): List<AchievementDTO> = definitions.map { progress(userId, language, it) }

    fun claim(userId: Long, code: String): AchievementRewardDTO? {
        val definition = definitionFor(code) ?: return null
        val progressValue = definition.progress(userId)
        val levelIndex = levelIndex(definition.thresholds, progressValue)
        val rowId = ensureProgressRow(userId, definition)
        val (claimed, storedLevel) = inTxn {
            AchievementProgress.select { AchievementProgress.id eq rowId }.single().let { row ->
                row[AchievementProgress.claimedLevel] to row[AchievementProgress.level]
            }
        }
        if (levelIndex <= claimed) return null
        inTxn {
            AchievementProgress.update({ AchievementProgress.id eq rowId }) {
                it[AchievementProgress.claimedLevel] = levelIndex
                it[AchievementProgress.level] = maxOf(storedLevel, levelIndex)
                it[AchievementProgress.updatedAt] = Instant.now()
            }
        }
        Metrics.counter("achievement_claim_total", mapOf("code" to code))
        val rewards = ((claimed + 1)..levelIndex).mapNotNull { definition.rewards.getOrNull(it) }
        return AchievementRewardDTO(code, rewards)
    }

    fun updateOnCatch(userId: Long, fishId: Long): List<AchievementUnlock> {
        val fishDetails = inTxn {
            Fish.select { Fish.id eq fishId }.singleOrNull()?.let { row ->
                row[Fish.name] to row[Fish.rarity]
            }
        }
        val unlocks = mutableListOf<AchievementUnlock>()
        definitions.forEach { definition ->
            val relevant = when (definition.code) {
                KOI_COLLECTOR_CODE -> fishDetails?.first?.let { koiFishNames.contains(it) } == true
                SIMPLE_FISHER_CODE -> fishDetails?.second == "common"
                else -> true
            }
            if (!relevant) return@forEach
            val rowId = ensureProgressRow(userId, definition)
            val previous = inTxn {
                AchievementProgress.select { AchievementProgress.id eq rowId }
                    .single()[AchievementProgress.level]
            }
            val progressValue = definition.progress(userId)
            val levelIndex = levelIndex(definition.thresholds, progressValue)
            if (levelIndex > previous) {
                inTxn {
                    AchievementProgress.update({ AchievementProgress.id eq rowId }) {
                        it[AchievementProgress.level] = levelIndex
                        it[AchievementProgress.updatedAt] = Instant.now()
                    }
                }
                Metrics.counter("achievement_unlock_total", mapOf("code" to definition.code))
                unlocks.add(AchievementUnlock(definition.code, levelIndex))
            }
        }
        return unlocks
    }

    private fun koiCatchCount(userId: Long): Int = inTxn {
        val fishIds = Fish
            .slice(Fish.id)
            .select { Fish.name inList koiFishNames }
            .map { it[Fish.id].value }
        if (fishIds.isEmpty()) return@inTxn 0
        Catches
            .slice(Catches.fishId)
            .select { (Catches.userId eq userId) and (Catches.fishId inList fishIds) }
            .groupBy(Catches.fishId)
            .orderBy(Catches.fishId, SortOrder.ASC)
            .count()
            .toInt()
    }

    private fun commonCatchCount(userId: Long): Int = inTxn {
        (Catches innerJoin Fish)
            .slice(Catches.id)
            .select { (Catches.userId eq userId) and (Fish.rarity eq "common") }
            .count()
            .toInt()
    }

    private fun levelLabel(index: Int, ru: Boolean): String = when (index) {
        1 -> if (ru) "Бронза" else "Bronze"
        2 -> if (ru) "Серебро" else "Silver"
        3 -> if (ru) "Золото" else "Gold"
        4 -> if (ru) "Платина" else "Platinum"
        else -> if (ru) "Нет уровня" else "No tier"
    }
}
