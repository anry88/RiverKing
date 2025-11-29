package service

import db.AchievementProgress
import db.Achievements
import db.Catches
import db.Fish
import db.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
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
    val reward: PrizeSpec,
)

@Serializable
data class AchievementUnlock(
    val code: String,
    val newLevelIndex: Int,
)

object AchievementService {
    private const val KOI_COLLECTOR_CODE = "koi_collector"
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

    private inline fun <T> inTxn(block: () -> T): T {
        val current = TransactionManager.currentOrNull()
        return if (current == null) transaction { block() } else block()
    }

    fun seed() = inTxn {
        Achievements.insertIgnore { row ->
            row[code] = KOI_COLLECTOR_CODE
            row[nameRu] = "Коллекционер Кои"
            row[nameEn] = "Koi Collector"
            row[descRu] = "Поймайте всех 16 видов карпов кои"
            row[descEn] = "Catch all 16 koi varieties"
        }
    }

    private fun levelIndex(progress: Int): Int {
        var level = 0
        koiThresholds.forEachIndexed { index, threshold ->
            if (progress >= threshold) level = index
        }
        return level
    }

    private fun ensureProgressRow(userId: Long): Long = inTxn {
        val achievementId = Achievements.select { Achievements.code eq KOI_COLLECTOR_CODE }
            .single()[Achievements.id].value
        val existing = AchievementProgress.select {
            (AchievementProgress.userId eq userId) and (AchievementProgress.achievementId eq achievementId)
        }.singleOrNull()
        if (existing != null) return@transaction existing[AchievementProgress.id].value
        AchievementProgress.insertIgnore { row ->
            row[AchievementProgress.userId] = userId
            row[AchievementProgress.achievementId] = achievementId
            row[level] = 0
            row[claimedLevel] = 0
            row[updatedAt] = Instant.now()
        } get AchievementProgress.id
    }.value

    fun progress(userId: Long, language: String): AchievementDTO = inTxn {
        val achievement = Achievements.select { Achievements.code eq KOI_COLLECTOR_CODE }.single()
        val langIsRu = language.lowercase().startsWith("ru")
        val progress = koiCatchCount(userId)
        val level = levelIndex(progress)
        val target = koiThresholds.getOrNull(level + 1) ?: koiThresholds.last()
        val progressRow = AchievementProgress.select {
            (AchievementProgress.userId eq userId) and (AchievementProgress.achievementId eq achievement[Achievements.id].value)
        }.singleOrNull()
        val claimed = progressRow?.get(AchievementProgress.claimedLevel) ?: 0
        AchievementDTO(
            code = KOI_COLLECTOR_CODE,
            name = if (langIsRu) achievement[Achievements.nameRu] else achievement[Achievements.nameEn],
            description = if (langIsRu) achievement[Achievements.descRu] else achievement[Achievements.descEn],
            level = levelLabel(level, langIsRu),
            levelIndex = level,
            progress = progress,
            target = target,
            claimable = level > claimed,
        )
    }

    fun list(userId: Long, language: String): List<AchievementDTO> = listOf(progress(userId, language))

    fun claim(userId: Long, code: String): AchievementRewardDTO? {
        if (code != KOI_COLLECTOR_CODE) return null
        val progress = progress(userId, "ru")
        val achievementId = inTxn {
            Achievements.select { Achievements.code eq KOI_COLLECTOR_CODE }
                .single()[Achievements.id].value
        }
        val rowId = ensureProgressRow(userId)
        val claimed = inTxn {
            AchievementProgress.select { AchievementProgress.id eq rowId }
                .single()[AchievementProgress.claimedLevel]
        }
        if (progress.levelIndex <= claimed) return null
        inTxn {
            AchievementProgress.update({ AchievementProgress.id eq rowId }) {
                it[claimedLevel] = progress.levelIndex
                it[updatedAt] = Instant.now()
            }
        }
        Metrics.counter("achievement_claim_total", mapOf("code" to code))
        return AchievementRewardDTO(code, koiRewards[progress.levelIndex])
    }

    fun updateOnCatch(userId: Long, fishId: Long): AchievementUnlock? {
        val isKoi = inTxn {
            Fish.select { Fish.id eq fishId }.singleOrNull()?.get(Fish.name)
        }?.let { koiFishNames.contains(it) } ?: false
        if (!isKoi) return null
        val progress = progress(userId, "ru")
        val rowId = ensureProgressRow(userId)
        val previous = inTxn {
            AchievementProgress.select { AchievementProgress.id eq rowId }
                .single()[AchievementProgress.level]
        }
        if (progress.levelIndex <= previous) return null
        inTxn {
            AchievementProgress.update({ AchievementProgress.id eq rowId }) {
                it[level] = progress.levelIndex
                it[updatedAt] = Instant.now()
            }
        }
        Metrics.counter("achievement_unlock_total", mapOf("code" to KOI_COLLECTOR_CODE))
        return AchievementUnlock(KOI_COLLECTOR_CODE, progress.levelIndex)
    }

    private fun koiCatchCount(userId: Long): Int = inTxn {
        val fishIds = Fish
            .slice(Fish.id)
            .select { Fish.name inList koiFishNames }
            .map { it[Fish.id].value }
        if (fishIds.isEmpty()) return@transaction 0
        Catches
            .slice(Catches.fishId)
            .select { (Catches.userId eq userId) and (Catches.fishId inList fishIds) }
            .groupBy(Catches.fishId)
            .orderBy(Catches.fishId, SortOrder.ASC)
            .count()
    }

    private fun levelLabel(index: Int, ru: Boolean): String = when (index) {
        1 -> if (ru) "Бронза" else "Bronze"
        2 -> if (ru) "Серебро" else "Silver"
        3 -> if (ru) "Золото" else "Gold"
        4 -> if (ru) "Платина" else "Platinum"
        else -> if (ru) "Нет уровня" else "No tier"
    }
}
