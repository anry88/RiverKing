package service

import db.AchievementProgress
import db.Achievements
import db.Catches
import db.Fish
import db.LocationFishWeights
import db.Locations
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import util.Metrics
import java.time.Instant
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.jetbrains.exposed.sql.sum

@Serializable
data class AchievementDTO(
    val code: String,
    val name: String,
    val description: String,
    val level: String,
    val levelIndex: Int,
    val progress: Double,
    val target: Int,
    val progressLabel: String,
    val targetLabel: String,
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
        val progress: (Long) -> Double,
        val isRelevantCatch: (CatchContext) -> Boolean = { true },
    )

    private data class CatchContext(
        val fishName: String?,
        val rarity: String?,
        val locationId: Long?,
    )

    private data class LocationAchievementData(
        val code: String,
        val locationId: Long,
        val locationName: String,
        val fishCount: Int,
        val waters: Set<String>,
    )

    private const val KOI_COLLECTOR_CODE = "koi_collector"
    private const val SIMPLE_FISHER_CODE = "simple_fisher"
    private const val UNCOMMON_FISHER_CODE = "uncommon_fisher"
    private const val RARE_FISHER_CODE = "rare_fisher"
    private const val EPIC_FISHER_CODE = "epic_fisher"
    private const val MYTHIC_FISHER_CODE = "mythic_fisher"
    private const val LEGENDARY_FISHER_CODE = "legendary_fisher"
    private const val TRAVELER_CODE = "traveler"
    private const val TROPHY_HUNTER_CODE = "trophy_hunter"
    private data class LocationStats(val fishCount: Int, val waters: Set<String>)
    private data class LocationAchievementConfig(val code: String, val name: String, val stats: LocationStats)

    private val locationAchievementConfigs = listOf(
        LocationAchievementConfig("pond_all_fish", "Пруд", LocationStats(fishCount = 35, waters = setOf("fresh"))),
        LocationAchievementConfig("swamp_all_fish", "Болото", LocationStats(fishCount = 17, waters = setOf("fresh"))),
        LocationAchievementConfig("river_all_fish", "Река", LocationStats(fishCount = 32, waters = setOf("fresh"))),
        LocationAchievementConfig("lake_all_fish", "Озеро", LocationStats(fishCount = 30, waters = setOf("fresh"))),
        LocationAchievementConfig("reservoir_all_fish", "Водохранилище", LocationStats(fishCount = 27, waters = setOf("fresh"))),
        LocationAchievementConfig("mountain_river_all_fish", "Горная река", LocationStats(fishCount = 24, waters = setOf("fresh"))),
        LocationAchievementConfig("river_delta_all_fish", "Дельта реки", LocationStats(fishCount = 49, waters = setOf("fresh", "salt"))),
        LocationAchievementConfig("sea_coast_all_fish", "Прибрежье моря", LocationStats(fishCount = 37, waters = setOf("salt"))),
        LocationAchievementConfig("amazon_riverbed_all_fish", "Русло Амазонки", LocationStats(fishCount = 46, waters = setOf("fresh"))),
        LocationAchievementConfig("igapo_all_fish", "Игапо, затопленный лес", LocationStats(fishCount = 47, waters = setOf("fresh"))),
        LocationAchievementConfig("mangroves_all_fish", "Мангровые заросли", LocationStats(fishCount = 43, waters = setOf("salt"))),
        LocationAchievementConfig("coral_flats_all_fish", "Коралловые отмели", LocationStats(fishCount = 42, waters = setOf("salt"))),
        LocationAchievementConfig("fjord_all_fish", "Фьорд", LocationStats(fishCount = 27, waters = setOf("salt"))),
        LocationAchievementConfig("open_ocean_all_fish", "Открытый океан", LocationStats(fishCount = 33, waters = setOf("salt"))),
    )

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

    private val simpleFisherThresholds = listOf(0, 10, 100, 500, 1000)
    private val uncommonFisherThresholds = listOf(0, 5, 50, 250, 500)
    private val rareFisherThresholds = listOf(0, 3, 30, 150, 300)
    private val epicFisherThresholds = listOf(0, 1, 20, 100, 200)
    private val mythicFisherThresholds = listOf(0, 1, 10, 50, 100)
    private val legendaryFisherThresholds = listOf(0, 1, 5, 25, 50)
    private val travelerThresholds = listOf(1, 3, 6, 10, 14)
    private val trophyHunterThresholds = listOf(0, 1, 10, 100, 1000)
    private val koiThresholds = listOf(0, 1, 3, 8, 16)
    private val simpleFisherRewards = listOf(
        PrizeSpec(pack = "", qty = 0),
        PrizeSpec(pack = "fresh_topup_s", qty = 1),
        PrizeSpec(pack = "fresh_stock_m", qty = 1),
        PrizeSpec(pack = "fresh_crate_l", qty = 1),
        PrizeSpec(pack = "salt_crate_l", qty = 1),
    )
    private val travelerRewards = listOf(
        PrizeSpec(pack = "", qty = 0),
        PrizeSpec(pack = "fresh_topup_s", qty = 1),
        PrizeSpec(pack = "fresh_stock_m", qty = 1),
        PrizeSpec(pack = "fresh_crate_l", qty = 1),
        PrizeSpec(pack = "salt_crate_l", qty = 1),
    )

    private val baseDefinitions = listOf(
        AchievementDefinition(
            code = SIMPLE_FISHER_CODE,
            nameRu = "Простой рыбак",
            nameEn = "Simple Fisher",
            descRu = "Наловите рыб простой редкости",
            descEn = "Catch common rarity fish",
            thresholds = simpleFisherThresholds,
            rewards = simpleFisherRewards,
            progress = ::commonCatchCount,
            isRelevantCatch = { it.rarity == "common" },
        ),
        AchievementDefinition(
            code = UNCOMMON_FISHER_CODE,
            nameRu = "Непростой рыбак",
            nameEn = "Uncommon Fisher",
            descRu = "Наловите рыб необычной редкости",
            descEn = "Catch uncommon rarity fish",
            thresholds = uncommonFisherThresholds,
            rewards = simpleFisherRewards,
            progress = ::uncommonCatchCount,
            isRelevantCatch = { it.rarity == "uncommon" },
        ),
        AchievementDefinition(
            code = RARE_FISHER_CODE,
            nameRu = "Редкий рыбак",
            nameEn = "Rare Fisher",
            descRu = "Наловите редких рыб",
            descEn = "Catch rare fish",
            thresholds = rareFisherThresholds,
            rewards = simpleFisherRewards,
            progress = ::rareCatchCount,
            isRelevantCatch = { it.rarity == "rare" },
        ),
        AchievementDefinition(
            code = EPIC_FISHER_CODE,
            nameRu = "Эпичный рыбак",
            nameEn = "Epic Fisher",
            descRu = "Наловите рыб эпической редкости",
            descEn = "Catch epic rarity fish",
            thresholds = epicFisherThresholds,
            rewards = simpleFisherRewards,
            progress = ::epicCatchCount,
            isRelevantCatch = { it.rarity == "epic" },
        ),
        AchievementDefinition(
            code = MYTHIC_FISHER_CODE,
            nameRu = "Мифический рыбак",
            nameEn = "Mythic Fisher",
            descRu = "Наловите рыб мифической редкости",
            descEn = "Catch mythic rarity fish",
            thresholds = mythicFisherThresholds,
            rewards = simpleFisherRewards,
            progress = ::mythicCatchCount,
            isRelevantCatch = { it.rarity == "mythic" },
        ),
        AchievementDefinition(
            code = LEGENDARY_FISHER_CODE,
            nameRu = "Легендарный рыбак",
            nameEn = "Legendary Fisher",
            descRu = "Наловите рыб легендарной редкости",
            descEn = "Catch legendary rarity fish",
            thresholds = legendaryFisherThresholds,
            rewards = simpleFisherRewards,
            progress = ::legendaryCatchCount,
            isRelevantCatch = { it.rarity == "legendary" },
        ),
        AchievementDefinition(
            code = TRAVELER_CODE,
            nameRu = "Путешественник",
            nameEn = "Traveler",
            descRu = "Откройте все локации",
            descEn = "Unlock every location",
            thresholds = travelerThresholds,
            rewards = travelerRewards,
            progress = ::unlockedLocationsCount,
        ),
        AchievementDefinition(
            code = TROPHY_HUNTER_CODE,
            nameRu = "Охотник за трофеями",
            nameEn = "Trophy Hunter",
            descRu = "Поймайте самую крупную рыбу",
            descEn = "Land your heaviest fish yet",
            thresholds = trophyHunterThresholds,
            rewards = simpleFisherRewards,
            progress = ::heaviestCatchKg,
        ),
        AchievementDefinition(
            code = KOI_COLLECTOR_CODE,
            nameRu = "Коллекционер Кои",
            nameEn = "Koi Collector",
            descRu = "Поймайте все 16 видов карпов кои",
            descEn = "Catch all 16 koi varieties",
            thresholds = koiThresholds,
            rewards = koiRewards,
            progress = ::koiCatchCount,
            isRelevantCatch = { koiFishNames.contains(it.fishName) },
        ),
    )

    private val definitions: List<AchievementDefinition> by lazy {
        baseDefinitions + buildLocationAchievements()
    }

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

    private fun locationAchievementData(): List<LocationAchievementData> = inTxn {
        val idsByName = Locations.slice(Locations.id, Locations.name)
            .selectAll()
            .associate { row -> row[Locations.name] to row[Locations.id].value }

        val fishCounts = mutableMapOf<Long, MutableSet<Long>>()
        val watersByLocation = mutableMapOf<Long, MutableSet<String>>()
        (LocationFishWeights innerJoin Fish)
            .slice(LocationFishWeights.locationId, LocationFishWeights.fishId, Fish.water)
            .selectAll()
            .forEach { row ->
                val locId = row[LocationFishWeights.locationId].value
                fishCounts.getOrPut(locId) { mutableSetOf() }.add(row[LocationFishWeights.fishId].value)
                watersByLocation.getOrPut(locId) { mutableSetOf() }.add(row[Fish.water])
            }

        locationAchievementConfigs.mapNotNull { (code, name, stats) ->
            val id = idsByName[name] ?: return@mapNotNull null
            LocationAchievementData(
                code = code,
                locationId = id,
                locationName = name,
                fishCount = fishCounts[id]?.size ?: stats.fishCount,
                waters = watersByLocation[id] ?: stats.waters,
            )
        }
    }

    private fun locationThresholds(fishCount: Int): List<Int> {
        val bronze = 1
        val silver = ceil(fishCount * 0.35).toInt().coerceAtMost(fishCount)
        val gold = ceil(fishCount * 0.7).toInt().coerceAtMost(fishCount)
        return listOf(0, bronze, silver, gold, fishCount)
    }

    private fun locationRewards(waters: Set<String>): List<PrizeSpec> {
        val boosterPack = if (waters.all { it == "fresh" }) "fresh_boost_l" else "salt_boost_l"
        return simpleFisherRewards.dropLast(1) + listOf(PrizeSpec(pack = boosterPack, qty = 1))
    }

    private fun buildLocationAchievements(): List<AchievementDefinition> = locationAchievementData().map { loc ->
        val locNameEn = I18n.location(loc.locationName, "en")
        val thresholds = locationThresholds(loc.fishCount)
        val rewards = locationRewards(loc.waters)
        AchievementDefinition(
            code = loc.code,
            nameRu = "Исследователь: ${loc.locationName}",
            nameEn = "Explorer: $locNameEn",
            descRu = "Откройте всех рыб в локации «${loc.locationName}»",
            descEn = "Discover every fish in $locNameEn",
            thresholds = thresholds,
            rewards = rewards,
            progress = { userId -> uniqueFishCaughtAtLocation(userId, loc.locationId) },
            isRelevantCatch = { ctx -> ctx.locationId == loc.locationId },
        )
    }

    private fun achievementId(definition: AchievementDefinition): Long = inTxn {
        Achievements.select { Achievements.code eq definition.code }
            .single()[Achievements.id].value
    }

    private fun levelIndex(thresholds: List<Int>, progress: Double): Int {
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
        val roundedProgress = roundForDisplay(definition.code, progressValue)
        val level = levelIndex(definition.thresholds, progressValue)
        val uiLevel = minOf(level, 4)
        val target = definition.thresholds.getOrNull(level + 1) ?: definition.thresholds.last()
        val displayTarget = if (definition.code.endsWith("_all_fish")) {
            definition.thresholds.last()
        } else {
            target
        }
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
            level = levelLabel(uiLevel, langIsRu),
            levelIndex = uiLevel,
            progress = roundedProgress,
            target = displayTarget,
            progressLabel = formatAchievementValue(definition.code, roundedProgress, langIsRu),
            targetLabel = formatAchievementValue(definition.code, displayTarget.toDouble(), langIsRu),
            claimable = level > claimed,
        )
    }

    fun list(userId: Long, language: String): List<AchievementDTO> = definitions.map { progress(userId, language, it) }

    fun claim(userId: Long, code: String): AchievementRewardDTO? {
        val definition = definitionFor(code) ?: return null
        val progressValue = definition.progress(userId)
        val currentLevel = levelIndex(definition.thresholds, progressValue)
        val rowId = ensureProgressRow(userId, definition)
        val (claimed, storedLevel) = inTxn {
            AchievementProgress.select { AchievementProgress.id eq rowId }.single().let { row ->
                row[AchievementProgress.claimedLevel] to row[AchievementProgress.level]
            }
        }
        if (currentLevel <= claimed) return null
        inTxn {
            AchievementProgress.update({ AchievementProgress.id eq rowId }) {
                it[AchievementProgress.claimedLevel] = currentLevel
                it[AchievementProgress.level] = maxOf(storedLevel, currentLevel)
                it[AchievementProgress.updatedAt] = Instant.now()
            }
        }
        Metrics.counter("achievement_claim_total", mapOf("code" to code))
        val rewards = ((claimed + 1)..currentLevel).mapNotNull { level ->
            definition.rewards.getOrNull(level)
        }
        return AchievementRewardDTO(code, rewards)
    }

    fun updateOnCatch(userId: Long, fishId: Long, locationId: Long): List<AchievementUnlock> {
        val fishDetails = inTxn {
            Fish.select { Fish.id eq fishId }.singleOrNull()?.let { row ->
                row[Fish.name] to row[Fish.rarity]
            }
        }
        val context = CatchContext(fishDetails?.first, fishDetails?.second, locationId)
        val unlocks = mutableListOf<AchievementUnlock>()
        definitions.forEach { definition ->
            if (!definition.isRelevantCatch(context)) return@forEach
            val rowId = ensureProgressRow(userId, definition)
            val previous = inTxn {
                AchievementProgress.select { AchievementProgress.id eq rowId }
                    .single()[AchievementProgress.level]
            }
            val progressValue = definition.progress(userId)
            val newLevel = levelIndex(definition.thresholds, progressValue)
            val uiLevel = minOf(newLevel, 4)
            if (newLevel > previous) {
                inTxn {
                    AchievementProgress.update({ AchievementProgress.id eq rowId }) {
                        it[AchievementProgress.level] = newLevel
                        it[AchievementProgress.updatedAt] = Instant.now()
                    }
                }
                Metrics.counter("achievement_unlock_total", mapOf("code" to definition.code))
                unlocks.add(AchievementUnlock(definition.code, uiLevel))
            }
        }
        return unlocks
    }

    fun unlockMessages(unlocks: List<AchievementUnlock>, language: String): List<String> {
        if (unlocks.isEmpty()) return emptyList()
        val langIsRu = language.lowercase().startsWith("ru")
        return unlocks.mapNotNull { unlock ->
            val definition = definitionFor(unlock.code) ?: return@mapNotNull null
            val name = if (langIsRu) definition.nameRu else definition.nameEn
            val level = levelLabel(unlock.newLevelIndex, langIsRu)
            if (langIsRu) {
                "Открыто достижение «$name»: $level"
            } else {
                "Achievement unlocked: \"$name\" — $level"
            }
        }
    }

    private fun uniqueFishCaughtAtLocation(userId: Long, locationId: Long): Double = inTxn {
        Catches
            .slice(Catches.fishId)
            .select { (Catches.userId eq userId) and (Catches.locationId eq locationId) }
            .groupBy(Catches.fishId)
            .count()
            .toDouble()
    }

    private fun roundForDisplay(code: String, value: Double): Double =
        if (code == TROPHY_HUNTER_CODE) {
            (value * 100).roundToInt() / 100.0
        } else {
            value
        }

    private fun formatAchievementValue(code: String, value: Double, langIsRu: Boolean): String {
        return if (code == TROPHY_HUNTER_CODE) {
            val rounded = roundForDisplay(code, value)
            val formatted = String.format(Locale.US, "%.2f", rounded)
            if (langIsRu) "$formatted кг" else "$formatted kg"
        } else {
            value.toInt().toString()
        }
    }

    private fun koiCatchCount(userId: Long): Double = inTxn {
        val fishIds = Fish
            .slice(Fish.id)
            .select { Fish.name inList koiFishNames }
            .map { it[Fish.id].value }
        if (fishIds.isEmpty()) return@inTxn 0.0
        Catches
            .slice(Catches.fishId)
            .select { (Catches.userId eq userId) and (Catches.fishId inList fishIds) }
            .groupBy(Catches.fishId)
            .orderBy(Catches.fishId, SortOrder.ASC)
            .count()
            .toDouble()
    }

    private fun rarityCatchCount(userId: Long, rarity: String): Double = inTxn {
        (Catches innerJoin Fish)
            .slice(Catches.id)
            .select { (Catches.userId eq userId) and (Fish.rarity eq rarity) }
            .count()
            .toDouble()
    }

    private fun unlockedLocationsCount(userId: Long): Double = inTxn {
        val totalKg = Catches.slice(Catches.weight.sum())
            .select { Catches.userId eq userId }
            .singleOrNull()?.get(Catches.weight.sum()) ?: 0.0
        Locations.select { Locations.unlockKg lessEq totalKg }.count().toDouble()
    }

    private fun heaviestCatchKg(userId: Long): Double = inTxn {
        val row = Catches
            .slice(Catches.weight)
            .select { Catches.userId eq userId }
            .orderBy(Catches.weight, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
        row?.get(Catches.weight) ?: 0.0
    }

    private fun commonCatchCount(userId: Long): Double = rarityCatchCount(userId, "common")
    private fun uncommonCatchCount(userId: Long): Double = rarityCatchCount(userId, "uncommon")
    private fun rareCatchCount(userId: Long): Double = rarityCatchCount(userId, "rare")
    private fun epicCatchCount(userId: Long): Double = rarityCatchCount(userId, "epic")
    private fun mythicCatchCount(userId: Long): Double = rarityCatchCount(userId, "mythic")
    private fun legendaryCatchCount(userId: Long): Double = rarityCatchCount(userId, "legendary")

    private fun levelLabel(index: Int, ru: Boolean): String =
        when {
            index >= 4 -> if (ru) "Платина" else "Platinum"
            index == 3 -> if (ru) "Золото" else "Gold"
            index == 2 -> if (ru) "Серебро" else "Silver"
            index == 1 -> if (ru) "Бронза" else "Bronze"
            else -> if (ru) "Нет уровня" else "No tier"
        }
}
