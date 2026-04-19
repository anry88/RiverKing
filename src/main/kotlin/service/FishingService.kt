package service

import db.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import util.CoinCalculator
import util.Rng
import util.sanitizeName
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.LinkedHashMap
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class LocationDTO(
    val id: Long,
    val name: String,
    val unlockKg: Double,
    val unlocked: Boolean,
)

@Serializable
data class RodDTO(
    val id: Long,
    val code: String,
    val name: String,
    val unlockKg: Double,
    val unlocked: Boolean,
    val bonusWater: String? = null,
    val bonusPredator: Boolean? = null,
    val priceStars: Int? = null,
    val packId: String? = null,
)

@Serializable
data class RecentDTO(
    val id: Long,
    val fish: String,
    val weight: Double,
    val location: String,
    val rarity: String,
    val at: String,
)

class FishingService(private val clock: Clock = Clock.systemUTC()) {
    companion object {
        private const val DEFAULT_ROD_CODE = "spark"
        private const val BEGINNER_CATCH_THRESHOLD = 6
    }
    data class DailyReward(val name: String, val qty: Int)

    private val ratingZone: ZoneId = ZoneId.of("Europe/Belgrade")
    private val clubs = ClubService()
    private val clubQuests = ClubQuestService()

    @Serializable
    data class StartCastResult(
        val currentLureId: Long?,
        val lureChanged: Boolean = false,
        val newLureName: String? = null,
        val recommendedRodId: Long? = null,
        val recommendedRodCode: String? = null,
        val recommendedRodName: String? = null,
        val recommendedRodUnlocked: Boolean? = null,
        val recommendedRodPriceStars: Int? = null,
        val recommendedRodPackId: String? = null,
    )

    private val freshDailyRewards: List<List<DailyReward>> = listOf(
        listOf(DailyReward("Пресная мирная", 8), DailyReward("Пресная хищная", 4)),
        listOf(DailyReward("Пресная мирная", 10), DailyReward("Пресная хищная", 6)),
        listOf(DailyReward("Пресная мирная", 12), DailyReward("Пресная хищная", 6)),
        listOf(DailyReward("Пресная мирная", 12), DailyReward("Пресная хищная", 8)),
        listOf(
            DailyReward("Пресная мирная", 12),
            DailyReward("Пресная хищная", 8),
            DailyReward("Пресная мирная+", 1),
        ),
        listOf(DailyReward("Пресная мирная", 12), DailyReward("Пресная хищная", 10)),
        listOf(
            DailyReward("Пресная мирная", 12),
            DailyReward("Пресная хищная", 12),
            DailyReward("Пресная мирная+", 1),
            DailyReward("Пресная хищная+", 1),
        ),
    )

    private val saltDailyRewards: List<List<DailyReward>> = listOf(
        listOf(
            DailyReward("Пресная мирная", 6),
            DailyReward("Пресная хищная", 6),
            DailyReward("Морская хищная", 4),
        ),
        listOf(
            DailyReward("Пресная мирная", 8),
            DailyReward("Пресная хищная", 8),
            DailyReward("Морская хищная", 5),
        ),
        listOf(
            DailyReward("Пресная мирная", 8),
            DailyReward("Пресная хищная", 8),
            DailyReward("Морская хищная", 6),
        ),
        listOf(
            DailyReward("Пресная мирная", 8),
            DailyReward("Пресная хищная", 10),
            DailyReward("Морская хищная", 6),
        ),
        listOf(
            DailyReward("Пресная мирная", 8),
            DailyReward("Пресная хищная", 10),
            DailyReward("Морская хищная", 6),
            DailyReward("Морская мирная", 2),
        ),
        listOf(
            DailyReward("Пресная мирная", 8),
            DailyReward("Пресная хищная", 10),
            DailyReward("Морская хищная", 8),
            DailyReward("Морская мирная", 2),
        ),
        listOf(
            DailyReward("Пресная мирная", 8),
            DailyReward("Пресная хищная", 10),
            DailyReward("Морская мирная", 2),
            DailyReward("Морская хищная", 8),
            DailyReward("Пресная мирная+", 1),
            DailyReward("Пресная хищная+", 1),
            DailyReward("Морская мирная+", 1),
            DailyReward("Морская хищная+", 1),
        ),
    )

    private val allDailyRewardLureNames: List<String> =
        (freshDailyRewards + saltDailyRewards).flatten().map { it.name }.distinct()

    private fun rewardPlan(hasSaltUnlocked: Boolean) =
        if (hasSaltUnlocked) saltDailyRewards else freshDailyRewards

    private fun hasSaltUnlocked(unlockedKg: Double) =
        (LocationFishWeights innerJoin Locations innerJoin Fish)
            .select { (Locations.unlockKg lessEq unlockedKg) and (Fish.water eq "salt") }
            .limit(1).any()

    fun createUser(
        tgId: Long? = null,
        firstName: String? = null,
        lastName: String? = null,
        username: String? = null,
        language: String? = null,
        refToken: String? = null,
    ): Long = transaction {
        createUserInternal(
            tgId = tgId,
            firstName = firstName,
            lastName = lastName,
            username = username,
            language = language,
            refToken = refToken,
        )
    }

    fun ensureUserById(userId: Long): Long = transaction {
        val existing = Users.select { Users.id eq userId }.singleOrNull()
            ?: error("unknown user")
        touchUser(existing)
    }

    fun ensureUserByTgId(
        tgId: Long,
        firstName: String? = null,
        lastName: String? = null,
        username: String? = null,
        language: String? = null,
        refToken: String? = null,
    ): Long = transaction {
        val existing = Users.selectAll().where { Users.tgId eq tgId }.singleOrNull()
        val userId = if (existing == null) {
            createUserInternal(
                tgId = tgId,
                firstName = firstName,
                lastName = lastName,
                username = username,
                language = language,
                refToken = refToken,
            )
        } else {
            val id = existing[Users.id].value
            val now = clock.instant()
            val lastSeen = existing[Users.lastSeenAt]
            val shouldUpdateLastSeen = lastSeen == null || Duration.between(lastSeen, now) >= Duration.ofMinutes(1)
            if (firstName != null || lastName != null || username != null || shouldUpdateLastSeen) {
                Users.update({ Users.id eq id }) {
                    if (firstName != null) it[Users.firstName] = firstName
                    if (lastName != null) it[Users.lastName] = lastName
                    if (username != null) it[Users.username] = username
                    it[Users.tgId] = tgId
                    if (shouldUpdateLastSeen) it[Users.lastSeenAt] = now
                }
            }
            touchUser(existing)
        }
        upsertTelegramIdentity(userId, tgId)
        userId
    }

    fun setNickname(userId: Long, nickname: String): String = transaction {
        val sanitized = sanitizeName(nickname).replace('\n', ' ').trim()
        Users.update({ Users.id eq userId }) { it[Users.nickname] = sanitized }
        sanitized
    }

    fun setLanguage(userId: Long, language: String) = transaction {
        Users.update({ Users.id eq userId }) { it[Users.language] = language }
    }

    fun userLanguage(userId: Long): String = transaction {
        Users.select { Users.id eq userId }.singleOrNull()?.get(Users.language) ?: "en"
    }

    fun userTgId(userId: Long): Long? = transaction {
        Users.select { Users.id eq userId }.singleOrNull()?.get(Users.tgId)
    }

    private fun createUserInternal(
        tgId: Long? = null,
        firstName: String? = null,
        lastName: String? = null,
        username: String? = null,
        language: String? = null,
        refToken: String? = null,
    ): Long {
        val now = clock.instant()
        val freshId = Lures.select { Lures.name eq "Пресная мирная" }.single()[Lures.id].value
        val predId = Lures.select { Lures.name eq "Пресная хищная" }.single()[Lures.id].value
        val baseRodId = Rods.select { Rods.code eq DEFAULT_ROD_CODE }.single()[Rods.id].value
        val newId = Users.insertAndGetId {
            it[Users.tgId] = tgId
            it[level] = 1
            it[xp] = 0
            it[createdAt] = now
            it[Users.lastSeenAt] = now
            it[Users.firstName] = firstName
            it[Users.lastName] = lastName
            it[Users.username] = username
            it[Users.language] = if (language?.startsWith("ru") == true) "ru" else "en"
            it[currentLureId] = freshId
            it[currentRodId] = baseRodId
        }.value
        InventoryLures.insert {
            it[InventoryLures.userId] = newId
            it[InventoryLures.lureId] = freshId
            it[InventoryLures.qty] = 10
        }
        InventoryLures.insert {
            it[InventoryLures.userId] = newId
            it[InventoryLures.lureId] = predId
            it[InventoryLures.qty] = 5
        }
        InventoryRods.insert {
            it[InventoryRods.userId] = newId
            it[InventoryRods.rodId] = baseRodId
            it[InventoryRods.qty] = 1
        }
        if (refToken != null) {
            ReferralService.setReferrer(newId, refToken)
        }
        return newId
    }

    private fun touchUser(row: ResultRow): Long {
        val id = row[Users.id].value
        val lastSeen = row[Users.lastSeenAt]
        val now = clock.instant()
        if (lastSeen == null || Duration.between(lastSeen, now) >= Duration.ofMinutes(1)) {
            Users.update({ Users.id eq id }) { it[Users.lastSeenAt] = now }
        }
        val total = totalKg(id)
        ensureRodInventory(id, total)
        ensureCurrentRod(id, total)
        return id
    }

    private fun upsertTelegramIdentity(userId: Long, tgId: Long) {
        val subject = tgId.toString()
        val existing = AuthIdentities.select {
            (AuthIdentities.provider eq "telegram") and
                (AuthIdentities.subject eq subject)
        }.singleOrNull()
        if (existing == null) {
            AuthIdentities.insert {
                it[AuthIdentities.userId] = userId
                it[provider] = "telegram"
                it[AuthIdentities.subject] = subject
                it[email] = null
                it[emailVerified] = false
                it[createdAt] = clock.instant()
                it[lastLoginAt] = clock.instant()
            }
        } else {
            AuthIdentities.update({ AuthIdentities.id eq existing[AuthIdentities.id].value }) {
                it[AuthIdentities.userId] = userId
                it[lastLoginAt] = clock.instant()
            }
        }
    }

    fun displayName(userId: Long): String? = transaction {
        Users.select { Users.id eq userId }.singleOrNull()?.let { nameFromRow(it) }
    }

    fun resetCasting(userId: Long) = transaction {
        PendingCatches.deleteWhere { PendingCatches.userId eq userId }
        Users.update({ Users.id eq userId }) {
            it[Users.isCasting] = false
            it[Users.castLureId] = null
        }
    }

    fun restoreCastingLuresOnStartup(): Int = transaction {
        val fallbackLureId = Lures.select { Lures.name eq "Пресная хищная+" }
            .single()[Lures.id].value
        val castingUsers = Users.select { Users.isCasting eq true }.toList()
        var restored = 0
        castingUsers.forEach { row ->
            val userId = row[Users.id].value
            val lastLureId = row.getOrNull(Users.castLureId)?.value
                ?: PendingCatches.select { PendingCatches.userId eq userId }
                    .singleOrNull()?.get(PendingCatches.lureId)?.value
                ?: fallbackLureId
            val existingQty = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lastLureId)
            }.singleOrNull()?.get(InventoryLures.qty)
            if (existingQty == null) {
                InventoryLures.insert {
                    it[InventoryLures.userId] = userId
                    it[InventoryLures.lureId] = lastLureId
                    it[InventoryLures.qty] = 1
                }
            } else {
                InventoryLures.update({
                    (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lastLureId)
                }) {
                    it[InventoryLures.qty] = existingQty + 1
                }
            }
            PendingCatches.deleteWhere { PendingCatches.userId eq userId }
            Users.update({ Users.id eq userId }) {
                it[Users.isCasting] = false
                it[Users.castLureId] = null
            }
            restored += 1
        }
        restored
    }

    private fun nameFromRow(row: ResultRow): String? {
        val fn = row.getOrNull(Users.firstName)
        val ln = row.getOrNull(Users.lastName)
        val un = row.getOrNull(Users.username)
        val nn = row.getOrNull(Users.nickname)
        val raw = when {
            !nn.isNullOrBlank() -> nn
            !fn.isNullOrBlank() || !ln.isNullOrBlank() -> listOfNotNull(fn, ln).joinToString(" ").trim()
            !un.isNullOrBlank() -> un
            else -> null
        }
        return raw?.let { sanitizeName(it) }
    }

    private fun catchUser(row: ResultRow): String? =
        nameFromRow(row)

    private fun totalCatchCount(userId: Long): Long {
        val countExpr = Catches.id.count()
        return Catches
            .slice(countExpr)
            .select { Catches.userId eq userId }
            .singleOrNull()?.get(countExpr) ?: 0L
    }

    private fun isBeginnerUser(userId: Long): Boolean =
        totalCatchCount(userId) < BEGINNER_CATCH_THRESHOLD

    private fun totalKg(userId: Long) =
        Catches.slice(Catches.weight.sum()).selectAll().where { Catches.userId eq userId }
            .singleOrNull()?.get(Catches.weight.sum()) ?: 0.0

    fun totalCaughtKg(userId: Long): Double = transaction { totalKg(userId) }

    fun todayCaughtKg(userId: Long): Double = transaction {
        val start = LocalDate.now(ratingZone).atStartOfDay(ratingZone).toInstant()
        Catches.slice(Catches.weight.sum()).selectAll()
            .where { (Catches.userId eq userId) and (Catches.createdAt greaterEq start) }
            .singleOrNull()?.get(Catches.weight.sum()) ?: 0.0
    }

    fun totalCaughtCount(userId: Long): Long = transaction { totalCatchCount(userId) }

    fun todayCaughtCount(userId: Long): Long = transaction {
        val start = LocalDate.now(ratingZone).atStartOfDay(ratingZone).toInstant()
        Catches.select { (Catches.userId eq userId) and (Catches.createdAt greaterEq start) }.count()
    }

    data class RarityCatchStats(val rarity: String, val count: Long, val weight: Double)

    fun catchStatsByRarity(userId: Long): List<RarityCatchStats> = catchStatsByRarity(userId, since = null)

    fun catchStatsByRarity(userId: Long, since: Instant?): List<RarityCatchStats> = transaction {
        val countExpr = Catches.id.count()
        val weightExpr = Catches.weight.sum()
        val baseCondition = Catches.userId eq userId
        val condition = if (since != null) baseCondition and (Catches.createdAt greaterEq since) else baseCondition
        (Catches innerJoin Fish)
            .slice(Fish.rarity, countExpr, weightExpr)
            .select { condition }
            .groupBy(Fish.rarity)
            .map { row ->
                val rarity = row[Fish.rarity]
                val count = row[countExpr] ?: 0L
                val weight = row[weightExpr] ?: 0.0
                RarityCatchStats(rarity, count, weight)
            }
            .sortedByDescending { rarityRank(it.rarity) }
    }

    fun catchStatsTotal(userId: Long, since: Instant?): Pair<Double, Long> = transaction {
        val weightExpr = Catches.weight.sum()
        val countExpr = Catches.id.count()
        val baseCondition = Catches.userId eq userId
        val condition = if (since != null) baseCondition and (Catches.createdAt greaterEq since) else baseCondition
        val row = Catches.slice(weightExpr, countExpr).select { condition }.single()
        val weight = row[weightExpr] ?: 0.0
        val count = row[countExpr] ?: 0L
        weight to count
    }

    fun fishRarity(name: String): String? = transaction {
        Fish.select { Fish.name eq name }.singleOrNull()?.get(Fish.rarity)
    }

    fun caughtFishIds(userId: Long): List<Long> = transaction {
        Catches.slice(Catches.fishId).select { Catches.userId eq userId }
            .withDistinct().map { it[Catches.fishId].value }
    }

    fun locations(userId: Long): List<LocationDTO> = transaction {
        val total = totalKg(userId)
        Locations.selectAll().orderBy(Locations.unlockKg).map {
            val unlock = it[Locations.unlockKg]
            LocationDTO(
                it[Locations.id].value,
                it[Locations.name],
                unlock,
                unlock <= total,
            )
        }
    }

    fun listRods(userId: Long): List<RodDTO> = transaction {
        val total = totalKg(userId)
        ensureRodInventory(userId, total)
        ensureCurrentRod(userId, total)
        val owned = InventoryRods
            .slice(InventoryRods.rodId)
            .select { InventoryRods.userId eq userId }
            .map { it[InventoryRods.rodId].value }
            .toSet()
        val nowInstant = Instant.now(clock)
        val zone = ZoneOffset.UTC
        val rodRows = Rods.selectAll().orderBy(Rods.unlockKg).toList()
        val packIds = rodRows.mapNotNull { rodPackId(it[Rods.code]) }.toSet()
        val discounts = loadActiveDiscounts(packIds, nowInstant, zone)
        rodRows.map { row ->
            val id = row[Rods.id].value
            val packId = rodPackId(row[Rods.code])
            val price = packId?.let { discounts[it] } ?: row[Rods.priceStars]
            RodDTO(
                id = id,
                code = row[Rods.code],
                name = row[Rods.name],
                unlockKg = row[Rods.unlockKg],
                unlocked = id in owned,
                bonusWater = row[Rods.bonusWater],
                bonusPredator = row[Rods.bonusPredator],
                priceStars = price,
                packId = packId,
            )
        }
    }

    fun hasRod(userId: Long, rodCode: String): Boolean = transaction {
        val rodId = Rods.select { Rods.code eq rodCode }.singleOrNull()?.get(Rods.id)?.value
            ?: return@transaction false
        InventoryRods.select {
            (InventoryRods.userId eq userId) and (InventoryRods.rodId eq rodId)
        }.any()
    }

    fun rodPackId(code: String): String? = shopCategories
        .flatMap { it.packs }
        .find { it.rodCode == code }
        ?.id

    private fun loadActiveDiscounts(
        packIds: Set<String>,
        nowInstant: Instant,
        zone: ZoneId,
    ): Map<String, Int> {
        if (packIds.isEmpty()) return emptyMap()
        return ShopDiscounts
            .select { ShopDiscounts.packageId inList packIds }
            .mapNotNull { row ->
                val startInstant = row[ShopDiscounts.startDate].atStartOfDay(zone).toInstant()
                val endInstant = row[ShopDiscounts.endDate].atStartOfDay(zone).toInstant()
                if (nowInstant >= startInstant && nowInstant < endInstant) {
                    row[ShopDiscounts.packageId] to row[ShopDiscounts.price]
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun ensureCurrentLure(userId: Long): Long? {
        val current = Users.select { Users.id eq userId }.single()[Users.currentLureId]?.value
        val curQty = current?.let {
            InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq it)
            }.singleOrNull()?.get(InventoryLures.qty) ?: 0
        } ?: 0
        if (curQty > 0) return current
        val first = (InventoryLures innerJoin Lures)
            .slice(Lures.id, InventoryLures.qty)
            .select { (InventoryLures.userId eq userId) and (InventoryLures.qty greater 0) }
            .orderBy(Lures.id)
            .firstOrNull()?.get(Lures.id)?.value
        Users.update({ Users.id eq userId }) { it[currentLureId] = first }
        return first
    }

    fun setRod(userId: Long, rodId: Long) = transaction {
        val casting = Users.select { Users.id eq userId }.single()[Users.isCasting]
        require(!casting) { "casting" }
        val total = totalKg(userId)
        ensureRodInventory(userId, total)
        val rodRow = Rods.select { Rods.id eq rodId }.singleOrNull() ?: error("bad rod")
        val has = InventoryRods.select {
            (InventoryRods.userId eq userId) and (InventoryRods.rodId eq rodId)
        }.any()
        val unlockedByWeight = rodRow[Rods.unlockKg] <= total
        if (!unlockedByWeight && !has) {
            throw IllegalArgumentException("locked")
        }
        require(has) { "no rod" }
        Users.update({ Users.id eq userId }) { it[currentRodId] = rodId }
    }

    private fun ensureRodInventory(userId: Long, total: Double) {
        val unlockedIds = Rods
            .slice(Rods.id)
            .select { Rods.unlockKg lessEq total }
            .map { it[Rods.id].value }
        unlockedIds.forEach { rodId ->
            val has = InventoryRods.select {
                (InventoryRods.userId eq userId) and (InventoryRods.rodId eq rodId)
            }.any()
            if (!has) {
                InventoryRods.insert {
                    it[InventoryRods.userId] = userId
                    it[InventoryRods.rodId] = rodId
                    it[InventoryRods.qty] = 1
                }
            }
        }
    }

    private fun ensureCurrentRod(userId: Long, totalWeight: Double? = null): Long {
        val total = totalWeight ?: totalKg(userId)
        ensureRodInventory(userId, total)
        val ownedIds = InventoryRods
            .slice(InventoryRods.rodId)
            .select { InventoryRods.userId eq userId }
            .map { it[InventoryRods.rodId].value }
        val row = Users.select { Users.id eq userId }.single()
        val current = row[Users.currentRodId]?.value
        if (current != null && ownedIds.contains(current)) {
            return current
        }
        val fallback = if (ownedIds.isNotEmpty()) {
            Rods.select { Rods.id inList ownedIds }
                .orderBy(Rods.unlockKg)
                .first()[Rods.id].value
        } else {
            Rods.select { Rods.code eq DEFAULT_ROD_CODE }.single()[Rods.id].value
        }
        Users.update({ Users.id eq userId }) { it[currentRodId] = fallback }
        return fallback
    }

    private fun rodBonusMultiplier(rodRow: ResultRow?, water: String, predator: Boolean): Double {
        if (rodRow == null) return 1.0
        val bonusWater = rodRow[Rods.bonusWater]
        val bonusPredator = rodRow[Rods.bonusPredator]
        if (bonusWater != null && bonusWater != water) return 1.0
        if (bonusPredator != null && bonusPredator != predator) return 1.0
        if (bonusWater == null && bonusPredator == null) return 1.0
        return 0.5
    }

    private fun rodUnlocksBetween(totalBefore: Double, totalAfter: Double): List<String> {
        if (totalAfter <= totalBefore) return emptyList()
        return Rods.select {
            (Rods.unlockKg greater totalBefore) and (Rods.unlockKg lessEq totalAfter)
        }.orderBy(Rods.unlockKg).map { it[Rods.name] }
    }

    fun giveDailyBaits(userId: Long): Triple<List<LureDTO>, Long?, Int>? = transaction {
        val tz = ZoneId.of("Europe/Belgrade")
        val today = LocalDate.now(tz)
        val row = Users.selectAll().where { Users.id eq userId }.forUpdate().single()
        val last = row[Users.lastDailyAt]?.atZone(tz)?.toLocalDate()
        var streak = row[Users.dailyStreak]
        if (last == today) return@transaction null

        streak = if (last == today.minusDays(1)) streak + 1 else 1

        val nameToId = Lures.select { Lures.name inList allDailyRewardLureNames }
            .associate { it[Lures.name] to it[Lures.id].value }

        fun add(id: Long, qty: Int) {
            val cur = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq id)
            }.singleOrNull()?.get(InventoryLures.qty)
            if (cur == null) {
                InventoryLures.insert {
                    it[InventoryLures.userId] = userId
                    it[InventoryLures.lureId] = id
                    it[InventoryLures.qty] = qty
                }
            } else {
                InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq id) }) {
                    it[InventoryLures.qty] = cur + qty
                }
            }
        }

        fun addByName(name: String, qty: Int) {
            val id = nameToId[name]
            if (id != null) add(id, qty)
        }

        val unlockedKg = totalKg(userId)
        val hasSaltUnlocked = hasSaltUnlocked(unlockedKg)

        val day = streak.coerceAtMost(7)
        val dayRewards = rewardPlan(hasSaltUnlocked)[day - 1]
        dayRewards.forEach { addByName(it.name, it.qty) }

        Users.update({ Users.id eq userId }) {
            it[lastDailyAt] = Instant.now()
            it[dailyStreak] = streak
        }

        val current = ensureCurrentLure(userId)
        val lures = (InventoryLures innerJoin Lures)
            .slice(Lures.id, Lures.name, InventoryLures.qty, Lures.predator, Lures.water, Lures.rarityBonus)
            .select { InventoryLures.userId eq userId }
            .map {
                LureDTO(
                    it[Lures.id].value,
                    it[Lures.name],
                    it[InventoryLures.qty],
                    it[Lures.predator],
                    it[Lures.water],
                    it[Lures.rarityBonus],
                )
            }
        Triple(lures, current, streak)
    }

    fun dailyRewardSchedule(userId: Long): List<List<DailyReward>> = transaction {
        val unlockedKg = totalKg(userId)
        val hasSaltUnlocked = hasSaltUnlocked(unlockedKg)
        rewardPlan(hasSaltUnlocked).map { day -> day.map { it.copy() } }
    }

    fun canClaimDaily(userId: Long): Boolean = transaction {
        val tz = ZoneId.of("Europe/Belgrade")
        val today = LocalDate.now(tz)
        val row = Users.selectAll().where { Users.id eq userId }.singleOrNull()
        val last = row?.get(Users.lastDailyAt)?.atZone(tz)?.toLocalDate()
        if (row != null && (last == null || last.isBefore(today.minusDays(1)))) {
            Users.update({ Users.id eq userId }) { it[Users.dailyStreak] = 0 }
        }
        last != today
    }

    @Serializable
    data class LureDTO(
        val id: Long,
        val name: String,
        val qty: Int,
        val predator: Boolean,
        val water: String,
        val rarityBonus: Double,
        val displayName: String = name,
        val description: String = "",
    )

    fun listLures(userId: Long): List<LureDTO> = transaction {
        ensureCurrentLure(userId)
        (InventoryLures innerJoin Lures)
            .slice(Lures.id, Lures.name, InventoryLures.qty, Lures.predator, Lures.water, Lures.rarityBonus)
            .select { InventoryLures.userId eq userId }
            .map {
                LureDTO(
                    it[Lures.id].value,
                    it[Lures.name],
                    it[InventoryLures.qty],
                    it[Lures.predator],
                    it[Lures.water],
                    it[Lures.rarityBonus],
                )
            }
    }

    @Serializable
    data class FishBriefDTO(val name: String, val rarity: String)

    @Serializable
    data class GuideLocationDTO(
        val id: Long,
        val name: String,
        val fish: List<FishBriefDTO>,
        val lures: List<String>,
    )

    @Serializable
    data class GuideFishDTO(
        val id: Long,
        val name: String,
        val rarity: String,
        val locationIds: List<Long>,
        val locations: List<String>,
        val lures: List<String>,
    )

    @Serializable
    data class GuideLureDTO(
        val name: String,
        val fish: List<FishBriefDTO>,
        val locations: List<String>,
    )

    @Serializable
    data class GuideRodDTO(
        val code: String,
        val name: String,
        val unlockKg: Double,
        val bonusWater: String?,
        val bonusPredator: Boolean?,
    )

    @Serializable
    data class GuideDTO(
        val locations: List<GuideLocationDTO>,
        val fish: List<GuideFishDTO>,
        val lures: List<GuideLureDTO>,
        val rods: List<GuideRodDTO>,
    )

    fun guide(lang: String): GuideDTO = transaction {
        fun rarityRank(r: String) = when (r) {
            "common" -> 0
            "uncommon" -> 1
            "rare" -> 2
            "epic" -> 3
            "mythic" -> 4
            "legendary" -> 5
            else -> 6
        }

        data class FishData(
            val id: Long,
            val name: String,
            val rarity: String,
            val predator: Boolean,
            val water: String,
        )

        data class LureData(
            val id: Long,
            val localizedName: String,
            val predator: Boolean,
            val water: String,
        )

        data class LocationData(
            val id: Long,
            val name: String,
        )

        val locations = Locations.selectAll()
            .orderBy(Locations.unlockKg)
            .map { LocationData(it[Locations.id].value, I18n.location(it[Locations.name], lang)) }
        val locationOrder = locations.mapIndexed { index, loc -> loc.id to index }.toMap()
        val locationNameById = locations.associate { it.id to it.name }

        val fish = Fish.selectAll()
            .map {
                FishData(
                    it[Fish.id].value,
                    I18n.fish(it[Fish.name], lang),
                    it[Fish.rarity],
                    it[Fish.predator],
                    it[Fish.water],
                )
            }
        val fishById = fish.associateBy { it.id }
        val fishBriefById = fish.associate { it.id to FishBriefDTO(it.name, it.rarity) }
        val fishByKey = fish.groupBy { it.predator to it.water }

        val lures = Lures.selectAll()
            .orderBy(Lures.id)
            .map {
                LureData(
                    it[Lures.id].value,
                    I18n.lure(it[Lures.name], lang),
                    it[Lures.predator],
                    it[Lures.water],
                )
            }
        val luresByWater = lures.groupBy { it.water }
        val luresByKey = lures.groupBy { it.predator to it.water }

        val locationFishPairs = LocationFishWeights
            .slice(LocationFishWeights.locationId, LocationFishWeights.fishId)
            .selectAll()
            .map {
                it[LocationFishWeights.locationId].value to it[LocationFishWeights.fishId].value
            }
        val locationFish = locationFishPairs.groupBy({ it.first }, { it.second })
        val fishLocations = locationFishPairs.groupBy({ it.second }, { it.first })

        val locationDtos = locations.map { loc ->
            val fishIds = locationFish[loc.id].orEmpty()
            val fishRows = fishIds.mapNotNull(fishBriefById::get)
                .sortedBy { rarityRank(it.rarity) }
            val waters = fishIds.mapNotNull { fishById[it]?.water }.toSet()
            val lureNames = (if (waters.isEmpty()) setOf("fresh") else waters)
                .flatMap { water -> luresByWater[water].orEmpty() }
                .distinctBy { it.id }
                .map { it.localizedName }
                .sorted()
            GuideLocationDTO(loc.id, loc.name, fishRows, lureNames)
        }

        val fishDtos = fish
            .map { fishData ->
                val locationsForFish = fishLocations[fishData.id]
                    .orEmpty()
                    .distinct()
                    .sortedBy { locationOrder[it] ?: Int.MAX_VALUE }
                    .mapNotNull(locationNameById::get)
                val luresForFish = luresByKey[fishData.predator to fishData.water]
                    .orEmpty()
                    .distinctBy { it.id }
                    .map { it.localizedName }
                    .sorted()
                GuideFishDTO(
                    fishData.id,
                    fishData.name,
                    fishData.rarity,
                    fishLocations[fishData.id]
                        .orEmpty()
                        .distinct()
                        .sortedBy { locationOrder[it] ?: Int.MAX_VALUE },
                    locationsForFish,
                    luresForFish,
                )
            }
            .sortedBy { rarityRank(it.rarity) }

        val lureDtos = lures.map { lure ->
            val fishList = fishByKey[lure.predator to lure.water].orEmpty()
                .sortedBy { rarityRank(it.rarity) }
            val fishBriefs = fishList.map { FishBriefDTO(it.name, it.rarity) }
            val locationsForLure = fishList
                .flatMap { fishData -> fishLocations[fishData.id].orEmpty() }
                .distinct()
                .sortedBy { locationOrder[it] ?: Int.MAX_VALUE }
                .mapNotNull(locationNameById::get)
            GuideLureDTO(lure.localizedName, fishBriefs, locationsForLure)
        }

        val rodDtos = Rods.selectAll().orderBy(Rods.unlockKg).map { rRow ->
            GuideRodDTO(
                rRow[Rods.code],
                I18n.rod(rRow[Rods.name], lang),
                rRow[Rods.unlockKg],
                rRow[Rods.bonusWater],
                rRow[Rods.bonusPredator],
            )
        }

        GuideDTO(locationDtos, fishDtos, lureDtos, rodDtos)
    }

    data class ShopPackage(
        val id: String,
        val name: String,
        val desc: String,
        val price: Int,
        val items: List<Pair<String, Int>>,
        val rodCode: String? = null,
        val coinPrice: Int? = null,
        val originalPrice: Int? = null,
        val discountStart: LocalDate? = null,
        val discountEnd: LocalDate? = null,
    )

    data class ShopCategory(
        val id: String,
        val name: String,
        val packs: List<ShopPackage>,
    )

    data class ShopDiscount(
        val packageId: String,
        val price: Int,
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    private val shopCategories = listOf(
        // --- Пресные простые (без изменений) ---
        ShopCategory(
            "fresh_basic",
            "Пресные простые",
            listOf(
                ShopPackage("fresh_topup_s","Пресное пополнение S","20 пресных простых: 10 «Зерновая крошка» и 10 «Ручейный малек»",20,
                    listOf("Пресная мирная" to 10, "Пресная хищная" to 10), coinPrice = 360),
                ShopPackage("fresh_stock_m","Пресный запас M","50 пресных простых: 25 «Зерновая крошка» и 25 «Ручейный малек»",45,
                    listOf("Пресная мирная" to 25, "Пресная хищная" to 25), coinPrice = 825),
                ShopPackage("fresh_crate_l","Пресный ящик L","120 пресных простых: 60 «Зерновая крошка» и 60 «Ручейный малек»",100,
                    listOf("Пресная мирная" to 60, "Пресная хищная" to 60), coinPrice = 1875),
            )
        ),

        // --- Морские простые: перекос в сторону хищных ---
        ShopCategory(
            "salt_basic",
            "Морские простые",
            listOf(
                ShopPackage(
                    "salt_topup_s",
                    "Морское пополнение S",
                    "20 морских простых: 6 «Морская водоросль» и 14 «Кольца кальмара»",
                    25,
                    listOf("Морская мирная" to 6, "Морская хищная" to 14),
                    coinPrice = 675
                ),
                ShopPackage(
                    "salt_stock_m",
                    "Морской запас M",
                    "50 морских простых: 15 «Морская водоросль» и 35 «Кольца кальмара»",
                    65,
                    listOf("Морская мирная" to 15, "Морская хищная" to 35),
                    coinPrice = 1500
                ),
                ShopPackage(
                    "salt_crate_l",
                    "Морской ящик L",
                    "120 морских простых: 40 «Морская водоросль» и 80 «Кольца кальмара»",
                    125,
                    listOf("Морская мирная" to 40, "Морская хищная" to 80),
                    coinPrice = 3300
                ),
            )
        ),

        // --- Пресные улучшенные (без изменений) ---
        ShopCategory(
            "fresh_boost",
            "Пресные улучшенные",
            listOf(
                ShopPackage("fresh_boost_s","Пресный буст S","10 пресных улучшенных: 5 «Луговой червь» и 5 «Серебряный живец»",35,
                    listOf("Пресная мирная+" to 5, "Пресная хищная+" to 5)),
                ShopPackage("fresh_boost_m","Пресный буст M","25 пресных улучшенных: 12 «Луговой червь» и 13 «Серебряный живец»",70,
                    listOf("Пресная мирная+" to 12, "Пресная хищная+" to 13)),
                ShopPackage("fresh_boost_l","Пресный буст L","60 пресных улучшенных: 30 «Луговой червь» и 30 «Серебряный живец»",140,
                    listOf("Пресная мирная+" to 30, "Пресная хищная+" to 30)),
            )
        ),

        // --- Морские улучшенные: больше хищных+ ---
        ShopCategory(
            "salt_boost",
            "Морские улучшенные",
            listOf(
                ShopPackage(
                    "salt_boost_s",
                    "Морской буст S",
                    "10 морских улучшенных: 4 «Неоновый планктон» и 6 «Королевская креветка»",
                    40,
                    listOf("Морская мирная+" to 4, "Морская хищная+" to 6)
                ),
                ShopPackage(
                    "salt_boost_m",
                    "Морской буст M",
                    "25 морских улучшенных: 9 «Неоновый планктон» и 16 «Королевская креветка»",
                    90,
                    listOf("Морская мирная+" to 9, "Морская хищная+" to 16)
                ),
                ShopPackage(
                    "salt_boost_l",
                    "Морской буст L",
                    "60 морских улучшенных: 20 «Неоновый планктон» и 40 «Королевская креветка»",
                    180,
                    listOf("Морская мирная+" to 20, "Морская хищная+" to 40)
                ),
            )
        ),

        // --- Смешанные: морские позиции смещены к хищным ---
        ShopCategory(
            "mixed",
            "Смешанные",
            listOf(
                ShopPackage(
                    "bundle_starter",
                    "Стартовый набор",
                    "40 пресных простых (20 «Зерновая крошка» и 20 «Ручейный малек»), 20 морских простых (6 «Морская водоросль» и 14 «Кольца кальмара») и 5 пресных улучшенных (3 «Луговой червь» и 2 «Серебряный живец»)",
                    60,
                    listOf(
                        "Пресная мирная" to 20,
                        "Пресная хищная" to 20,
                        "Морская мирная" to 6,
                        "Морская хищная" to 14,
                        "Пресная мирная+" to 3,
                        "Пресная хищная+" to 2,
                    )
                ),
                ShopPackage(
                    "bundle_pro",
                    "Профи рыболов",
                    "80 пресных простых (40 «Зерновая крошка» и 40 «Ручейный малек»), 40 морских простых (12 «Морская водоросль» и 28 «Кольца кальмара»), 15 пресных улучшенных (8 «Луговой червь» и 7 «Серебряный живец») и 5 морских улучшенных (1 «Неоновый планктон» и 4 «Королевская креветка»)",
                    150,
                    listOf(
                        "Пресная мирная" to 40,
                        "Пресная хищная" to 40,
                        "Морская мирная" to 12,
                        "Морская хищная" to 28,
                        "Пресная мирная+" to 8,
                        "Пресная хищная+" to 7,
                        "Морская мирная+" to 1,
                        "Морская хищная+" to 4,
                    )
                ),
                ShopPackage(
                    "bundle_whale",
                    "Китовый ящик",
                    "200 пресных простых (100 «Зерновая крошка» и 100 «Ручейный малек»), 120 морских простых (40 «Морская водоросль» и 80 «Кольца кальмара»), 40 пресных улучшенных (20 «Луговой червь» и 20 «Серебряный живец») и 20 морских улучшенных (6 «Неоновый планктон» и 14 «Королевская креветка»)",
                    400,
                    listOf(
                        "Пресная мирная" to 100,
                        "Пресная хищная" to 100,
                        "Морская мирная" to 40,
                        "Морская хищная" to 80,
                        "Пресная мирная+" to 20,
                        "Пресная хищная+" to 20,
                        "Морская мирная+" to 6,
                        "Морская хищная+" to 14,
                    )
                ),
            )
        ),

        // --- Стартовые: морской старт теперь 3/7 ---
        ShopCategory(
            "starter",
            "Стартовые",
            listOf(
                ShopPackage(
                    "micro_pred_fresh",
                    "Пополнение пресных хищных",
                    "15 «Ручейный малек»",
                    15,
                    listOf("Пресная хищная" to 15),
                    coinPrice = 315
                ),
                ShopPackage(
                    "micro_salt_starter",
                    "Морской старт",
                    "10 морских простых: 3 «Морская водоросль» и 7 «Кольца кальмара»",
                    15,
                    listOf("Морская мирная" to 3, "Морская хищная" to 7),
                    coinPrice = 360
                ),
                ShopPackage(
                    "micro_salt_pred_refill",
                    "Морской хищный запас",
                    "20 «Кольца кальмара»",
                    25,
                    listOf("Морская хищная" to 20),
                    coinPrice = 750
                ),
            )
        ),

        // --- Удочки ---
        ShopCategory(
            "rods",
            "Удочки",
            listOf(
                ShopPackage(
                    id = "rod_dew",
                    name = "Удочка «Роса»",
                    desc = "Разблокирует удочку «Роса».\nБонус: −50% шанс побега пресноводных мирных рыб.",
                    price = 10,
                    items = emptyList(),
                    rodCode = "dew",
                ),
                ShopPackage(
                    id = "rod_stream",
                    name = "Удочка «Поток»",
                    desc = "Разблокирует удочку «Поток».\nБонус: −50% шанс побега пресноводных хищных рыб.",
                    price = 40,
                    items = emptyList(),
                    rodCode = "stream",
                ),
                ShopPackage(
                    id = "rod_abyss",
                    name = "Удочка «Глубь»",
                    desc = "Разблокирует удочку «Глубь».\nБонус: −50% шанс побега морских мирных рыб.",
                    price = 70,
                    items = emptyList(),
                    rodCode = "abyss",
                ),
                ShopPackage(
                    id = "rod_storm",
                    name = "Удочка «Шторм»",
                    desc = "Разблокирует удочку «Шторм».\nБонус: −50% шанс побега морских хищных рыб.",
                    price = 100,
                    items = emptyList(),
                    rodCode = "storm",
                ),
            )
        ),

        // --- Подписки (без изменений) ---
        ShopCategory(
            "subscriptions",
            "Подписки",
            listOf(
                ShopPackage(
                    "autofish",
                    "Автоловля",
                    "Робот ловит за вас целый месяц и не упустит ни одной рыбы",
                    250,
                    emptyList()
                ),
            )
        ),
    )

    fun findPack(id: String): ShopPackage? = shopCategories.flatMap { it.packs }.find { it.id == id }

    class CoinPurchaseUnavailableException : RuntimeException()
    class NotEnoughCoinsException(val required: Int, val balance: Long) : RuntimeException()

    fun listShop(lang: String): List<ShopCategory> {
        val nowInstant = Instant.now(clock)
        val zone = ZoneOffset.UTC
        val discounts = transaction {
            ShopDiscounts.selectAll().associateBy(
                { it[ShopDiscounts.packageId] },
                {
                    ShopDiscount(
                        packageId = it[ShopDiscounts.packageId],
                        price = it[ShopDiscounts.price],
                        startDate = it[ShopDiscounts.startDate],
                        endDate = it[ShopDiscounts.endDate],
                    )
                }
            )
        }
        return shopCategories.map { cat ->
            cat.copy(
                name = I18n.text(cat.name, lang),
                packs = cat.packs.map { p ->
                    val discount = discounts[p.id]
                    val active = discount?.let {
                        val startInstant = it.startDate.atStartOfDay(zone).toInstant()
                        val endInstant = it.endDate.atStartOfDay(zone).toInstant()
                        nowInstant >= startInstant && nowInstant < endInstant
                    } == true
                    val finalPrice = if (active) discount!!.price else p.price
                    val originalPrice = if (active) p.price else null
                    val discountStart = if (active) discount!!.startDate else null
                    val discountEnd = if (active) discount!!.endDate else null
                    p.copy(
                        name = I18n.text(p.name, lang),
                        desc = I18n.text(p.desc, lang),
                        price = finalPrice,
                        items = p.items.map { I18n.lure(it.first, lang) to it.second },
                        originalPrice = originalPrice,
                        discountStart = discountStart,
                        discountEnd = discountEnd,
                    )
                }
            )
        }
    }

    fun listDiscounts(): List<ShopDiscount> {
        val nowInstant = Instant.now(clock)
        val zone = ZoneOffset.UTC
        val discounts = transaction {
            ShopDiscounts.selectAll().map {
                ShopDiscount(
                    packageId = it[ShopDiscounts.packageId],
                    price = it[ShopDiscounts.price],
                    startDate = it[ShopDiscounts.startDate],
                    endDate = it[ShopDiscounts.endDate],
                )
            }
        }
        return discounts.filter { discount ->
            discount.endDate.atStartOfDay(zone).toInstant() > nowInstant
        }
    }

    fun getDiscount(packageId: String): ShopDiscount? = transaction {
        ShopDiscounts.select { ShopDiscounts.packageId eq packageId }
            .singleOrNull()
            ?.let {
                ShopDiscount(
                    packageId = it[ShopDiscounts.packageId],
                    price = it[ShopDiscounts.price],
                    startDate = it[ShopDiscounts.startDate],
                    endDate = it[ShopDiscounts.endDate],
                )
            }
    }

    fun setDiscount(packageId: String, price: Int, start: LocalDate, end: LocalDate) = transaction {
        val existing = ShopDiscounts.select { ShopDiscounts.packageId eq packageId }.singleOrNull()
        if (existing == null) {
            ShopDiscounts.insert {
                it[ShopDiscounts.packageId] = packageId
                it[ShopDiscounts.price] = price
                it[ShopDiscounts.startDate] = start
                it[ShopDiscounts.endDate] = end
            }
        } else {
            ShopDiscounts.update({ ShopDiscounts.packageId eq packageId }) {
                it[ShopDiscounts.price] = price
                it[ShopDiscounts.startDate] = start
                it[ShopDiscounts.endDate] = end
            }
        }
    }

    fun removeDiscount(packageId: String): Int = transaction {
        ShopDiscounts.deleteWhere { ShopDiscounts.packageId eq packageId }
    }

    private fun grantPackItems(userId: Long, pack: ShopPackage): Pair<List<LureDTO>, Long?> {
        if (pack.rodCode != null) {
            unlockRod(userId, pack.rodCode)
            return Pair(emptyList(), null)
        }
        fun add(id: Long, qty: Int) {
            val cur = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq id)
            }.singleOrNull()?.get(InventoryLures.qty)
            if (cur == null) {
                InventoryLures.insert {
                    it[InventoryLures.userId] = userId
                    it[InventoryLures.lureId] = id
                    it[InventoryLures.qty] = qty
                }
            } else {
                InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq id) }) {
                    it[InventoryLures.qty] = cur + qty
                }
            }
        }
        for ((name, qty) in pack.items) {
            val id = Lures.select { Lures.name eq name }.single()[Lures.id].value
            add(id, qty)
        }
        val current = ensureCurrentLure(userId)
        val lures = (InventoryLures innerJoin Lures)
            .slice(Lures.id, Lures.name, InventoryLures.qty, Lures.predator, Lures.water, Lures.rarityBonus)
            .select { InventoryLures.userId eq userId }
            .map {
                LureDTO(
                    it[Lures.id].value,
                    it[Lures.name],
                    it[InventoryLures.qty],
                    it[Lures.predator],
                    it[Lures.water],
                    it[Lures.rarityBonus],
                )
            }
        return Pair(lures, current)
    }

    private fun unlockRod(userId: Long, rodCode: String) {
        val rodRow = Rods.select { Rods.code eq rodCode }.singleOrNull() ?: return
        val rodId = rodRow[Rods.id].value
        val has = InventoryRods.select {
            (InventoryRods.userId eq userId) and (InventoryRods.rodId eq rodId)
        }.forUpdate().singleOrNull()
        if (has == null) {
            InventoryRods.insert {
                it[InventoryRods.userId] = userId
                it[InventoryRods.rodId] = rodId
                it[InventoryRods.qty] = 1
            }
        }
        val userRow = Users.select { Users.id eq userId }.forUpdate().single()
        if (userRow[Users.currentRodId] == null) {
            Users.update({ Users.id eq userId }) { it[currentRodId] = rodId }
        }
    }

    fun buyPackage(userId: Long, packageId: String): Pair<List<LureDTO>, Long?> = transaction {
        if (packageId == "autofish" || packageId == "autofish_week") {
            val row = Users.select { Users.id eq userId }.forUpdate().single()
            val cur = row[Users.autoFishUntil]
            val base = if (cur != null && cur.isAfter(Instant.now())) cur else Instant.now()
            val newUntil = if (packageId == "autofish") {
                base.atZone(ZoneId.systemDefault()).plusMonths(1).toInstant()
            } else {
                base.atZone(ZoneId.systemDefault()).plusDays(7).toInstant()
            }
            Users.update({ Users.id eq userId }) { it[autoFishUntil] = newUntil }
            return@transaction Pair(emptyList(), null)
        }

        val pack = shopCategories.flatMap { it.packs }.find { it.id == packageId }
            ?: error("bad package")

        grantPackItems(userId, pack)
    }

    fun buyPackageWithCoins(userId: Long, packageId: String): Pair<List<LureDTO>, Long?> = transaction {
        val pack = shopCategories.flatMap { it.packs }.find { it.id == packageId }
            ?: error("bad package")
        val coinPrice = pack.coinPrice ?: throw CoinPurchaseUnavailableException()
        val userRow = Users.select { Users.id eq userId }.forUpdate().single()
        val balance = userRow[Users.coins]
        if (balance < coinPrice.toLong()) {
            throw NotEnoughCoinsException(coinPrice, balance)
        }
        Users.update({ Users.id eq userId }) {
            it[coins] = balance - coinPrice
        }
        grantPackItems(userId, pack)
    }

    fun addCoins(userId: Long, amount: Int): Long = transaction {
        val delta = amount.coerceAtLeast(0)
        val row = Users.select { Users.id eq userId }.forUpdate().single()
        val current = row[Users.coins]
        if (delta == 0) return@transaction current
        val updated = (current + delta.toLong()).coerceAtMost(Long.MAX_VALUE)
        Users.update({ Users.id eq userId }) { it[coins] = updated }
        updated
    }

    fun addLures(userId: Long, items: List<Pair<Long, Int>>): Pair<List<LureDTO>, Long?> = transaction {
        fun add(id: Long, qty: Int) {
            val cur = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq id)
            }.singleOrNull()?.get(InventoryLures.qty)
            if (cur == null) {
                InventoryLures.insert {
                    it[InventoryLures.userId] = userId
                    it[InventoryLures.lureId] = id
                    it[InventoryLures.qty] = qty
                }
            } else {
                InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq id) }) {
                    it[InventoryLures.qty] = cur + qty
                }
            }
        }
        for ((id, qty) in items) {
            add(id, qty)
        }
        val current = ensureCurrentLure(userId)
        val lures = (InventoryLures innerJoin Lures)
            .slice(Lures.id, Lures.name, InventoryLures.qty, Lures.predator, Lures.water, Lures.rarityBonus)
            .select { InventoryLures.userId eq userId }
            .map {
                LureDTO(
                    it[Lures.id].value,
                    it[Lures.name],
                    it[InventoryLures.qty],
                    it[Lures.predator],
                    it[Lures.water],
                    it[Lures.rarityBonus],
                )
            }
        Pair(lures, current)
    }

    fun disableAutoFish(userId: Long) = transaction {
        Users.update({ Users.id eq userId }) { it[autoFishUntil] = null }
    }

    fun removeAutoFishMonth(userId: Long) = transaction {
        val row = Users.select { Users.id eq userId }.forUpdate().single()
        val cur = row[Users.autoFishUntil]
        val now = Instant.now()
        if (cur != null && cur.isAfter(now)) {
            val newUntil = cur.atZone(ZoneId.systemDefault()).minusMonths(1).toInstant()
            Users.update({ Users.id eq userId }) {
                it[autoFishUntil] = if (newUntil.isAfter(now)) newUntil else null
            }
        } else {
            Users.update({ Users.id eq userId }) { it[autoFishUntil] = null }
        }
    }

    fun setLure(userId: Long, lureId: Long) = transaction {
        val casting = Users.select { Users.id eq userId }.single()[Users.isCasting]
        require(!casting) { "casting" }
        val has = InventoryLures.select { (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }
            .singleOrNull()?.get(InventoryLures.qty) ?: 0
        require(has > 0) { "no lure" }
        Users.update({ Users.id eq userId }) { it[currentLureId] = lureId }
    }

    fun setLocation(userId: Long, locationId: Long) = transaction {
        val casting = Users.select { Users.id eq userId }.single()[Users.isCasting]
        require(!casting) { "casting" }
        val loc = Locations.selectAll().where { Locations.id eq locationId }.singleOrNull()
            ?: error("bad location")
        val total = totalKg(userId)
        require(loc[Locations.unlockKg] <= total) { "locked" }
        Users.update({ Users.id eq userId }) { it[currentLocationId] = locationId }
    }

    /**
     * Consume a lure and validate that it can be used at the current location.
     * Returns the new current lure id if changed after consumption.
     */
    fun startCast(userId: Long): StartCastResult = transaction {
        val userRow = Users.select { Users.id eq userId }.single()
        require(!userRow[Users.isCasting]) { "casting" }
        PendingCatches.deleteWhere { PendingCatches.userId eq userId }
        val lureId = userRow[Users.currentLureId]?.value
            ?: error("No lure selected")
        val lureRow = (InventoryLures innerJoin Lures)
            .select { (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }
            .forUpdate().singleOrNull() ?: error("No baits")
        val q = lureRow[InventoryLures.qty]; require(q > 0) { "No baits" }

        val lurePred = lureRow[Lures.predator]
        val lureWater = lureRow[Lures.water]

        val total = totalKg(userId)
        ensureRodInventory(userId, total)
        ensureCurrentRod(userId, total)
        val locId = Users.select { Users.id eq userId }.single()[Users.currentLocationId]?.value
            ?: Locations.select { Locations.unlockKg lessEq total }.orderBy(Locations.unlockKg).first()[Locations.id].value
        val locRow = Locations.select { Locations.id eq locId }.single()
        require(locRow[Locations.unlockKg] <= total) { "locked" }
        val hasFish = (LocationFishWeights innerJoin Fish)
            .select { (LocationFishWeights.locationId eq locId) and (Fish.predator eq lurePred) and (Fish.water eq lureWater) }
            .limit(1).any()
        require(hasFish) { "No suitable fish" }

        InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }) {
            it[InventoryLures.qty] = q - 1
        }
        val newLure = ensureCurrentLure(userId)
        Users.update({ Users.id eq userId }) {
            it[Users.isCasting] = true
            it[Users.castLureId] = lureId
        }
        val lureChanged = newLure != lureId
        var newLureName: String? = null
        var recommendedRodId: Long? = null
        var recommendedRodCode: String? = null
        var recommendedRodName: String? = null
        var recommendedRodUnlocked: Boolean? = null
        var recommendedRodPrice: Int? = null
        var recommendedRodPackId: String? = null
        val discountZone = ZoneOffset.UTC
        val nowInstant = Instant.now(clock)
        if (lureChanged && newLure != null) {
            val newLureRow = Lures.select { Lures.id eq newLure }.singleOrNull()
            if (newLureRow != null) {
                newLureName = newLureRow[Lures.name]
                val recommended = Rods.select {
                    (Rods.bonusWater eq newLureRow[Lures.water]) and
                        (Rods.bonusPredator eq newLureRow[Lures.predator])
                }.singleOrNull()
                if (recommended != null) {
                    val recId = recommended[Rods.id].value
                    recommendedRodId = recId
                    recommendedRodCode = recommended[Rods.code]
                    recommendedRodName = recommended[Rods.name]
                    val packId = recommendedRodCode?.let { rodPackId(it) }
                    recommendedRodPackId = packId
                    val discountPrice = packId?.let {
                        loadActiveDiscounts(setOf(it), nowInstant, discountZone)[it]
                    }
                    recommendedRodPrice = discountPrice ?: recommended[Rods.priceStars]
                    recommendedRodUnlocked = InventoryRods.select {
                        (InventoryRods.userId eq userId) and
                            (InventoryRods.rodId eq recId)
                    }.any()
                }
            }
        }
        QuestService.ensureCurrentQuests(userId)
        StartCastResult(
            currentLureId = newLure,
            lureChanged = lureChanged,
            newLureName = newLureName,
            recommendedRodId = recommendedRodId,
            recommendedRodCode = recommendedRodCode,
            recommendedRodName = recommendedRodName,
            recommendedRodUnlocked = recommendedRodUnlocked,
            recommendedRodPriceStars = recommendedRodPrice,
            recommendedRodPackId = recommendedRodPackId,
        )
    }

    data class LocationEscapeChance(
        val locationId: Long,
        val escapeChance: Double,
        val rodBonusMultiplier: Double,
    )

    fun locationEscapeChance(userId: Long): LocationEscapeChance = transaction {
        val userRow = Users.select { Users.id eq userId }.single()
        val total = totalKg(userId)
        ensureRodInventory(userId, total)
        val rodId = ensureCurrentRod(userId, total)
        val rodRow = Rods.select { Rods.id eq rodId }.singleOrNull()
        val locId = userRow[Users.currentLocationId]?.value
            ?: Locations.select { Locations.unlockKg lessEq total }
                .orderBy(Locations.unlockKg)
                .first()[Locations.id].value
        val lureId = userRow[Users.castLureId]?.value ?: userRow[Users.currentLureId]?.value
        val lureRow = lureId?.let { Lures.select { Lures.id eq it }.singleOrNull() }
        val bonus = if (lureRow != null) {
            rodBonusMultiplier(rodRow, lureRow[Lures.water], lureRow[Lures.predator])
        } else {
            1.0
        }
        LocationEscapeChance(
            locationId = locId,
            escapeChance = baseEscapeChance(locId) * bonus,
            rodBonusMultiplier = bonus,
        )
    }

    @Serializable
    data class CatchDTO(
        val id: Long,
        val fish: String,
        val weight: Double,
        val location: String,
        val rarity: String,
        val userId: Long? = null,
        val fishId: Long? = null,
        val user: String? = null,
        val at: String? = null,
        val rank: Int? = null,
        val prizeCoins: Int? = null,
    )

    @Serializable
    data class DailyRatingPosition(
        val locationId: Long,
        val location: String,
        val rank: Int,
        val participants: Int,
        val bestFish: String,
        val bestRarity: String,
        val bestWeight: Double,
        val prizeCoins: Int? = null,
    )

    @Serializable
    data class HookResultDTO(val success: Boolean, val autoFish: Boolean)

    @Serializable
    data class CastResultDTO(
        val caught: Boolean,
        val catch: CatchDTO? = null,
        val autoFish: Boolean = false,
        val unlockedLocations: List<String> = emptyList(),
        val unlockedRods: List<String> = emptyList(),
        val coins: Int = 0,
        val totalCoins: Long? = null,
        val todayCoins: Long? = null,
        val achievements: List<AchievementUnlock> = emptyList(),
        val questUpdates: List<QuestService.QuestUpdate> = emptyList(),
        val questProgressChanged: Boolean = false,
    )

    private fun rarityModifier(rarity: String, factor: Double): Double = when (rarity) {
        "common" -> 1.0 - 0.7 * factor
        "uncommon" -> 0.6 + 0.3 * factor
        "rare" -> 0.3 + 0.4 * factor
        "epic" -> 0.2 + 0.3 * factor
        "mythic" -> 0.15 + 0.25 * factor
        "legendary" -> 0.1 + 0.2 * factor
        else -> 1.0
    }

    private fun locationTier(locId: Long): Int {
        val ordered = Locations
            .slice(Locations.id, Locations.unlockKg)
            .selectAll()
            .orderBy(Locations.unlockKg to SortOrder.ASC, Locations.id to SortOrder.ASC)
            .mapIndexed { index, row -> row[Locations.id].value to index }
            .toMap()
        return ordered[locId] ?: 0
    }

    internal fun baseEscapeChance(locId: Long): Double {
        val tier = locationTier(locId)
        return (0.05 * tier).coerceAtMost(0.5)
    }

    private fun coinsEarnedBetween(userId: Long, start: Instant, end: Instant): Long {
        val sumExpr = Catches.coins.sum() ?: return 0L
        val row = Catches
            .slice(sumExpr)
            .select {
                (Catches.userId eq userId) and
                    (Catches.createdAt greaterEq start) and
                    (Catches.createdAt less end) and
                    Catches.coins.isNotNull()
            }
            .singleOrNull()
        val total = row?.get(sumExpr) ?: 0
        return total.toLong()
    }

    private fun coinsEarnedOnDate(userId: Long, date: LocalDate, zone: ZoneId): Long {
        val start = date.atStartOfDay(zone).toInstant()
        val end = start.plus(1, ChronoUnit.DAYS)
        return coinsEarnedBetween(userId, start, end)
    }

    fun todayCoins(userId: Long): Long = transaction {
        val zone = ratingZone
        val today = LocalDate.now(zone)
        coinsEarnedOnDate(userId, today, zone)
    }

    fun hook(
        userId: Long,
        waitSeconds: Int,
        reactionTime: Double,
        extraEscapeChance: Double = 0.0,
        applyBeginnerProtection: Boolean = true,
    ): HookResultDTO = transaction {
        val userRow = Users.select { Users.id eq userId }.single()
        require(userRow[Users.isCasting]) { "no cast" }
        val lureId = userRow[Users.castLureId]?.value ?: error("No lure selected")
        val lureRow = Lures.select { Lures.id eq lureId }.single()

        val lurePred = lureRow[Lures.predator]
        val lureWater = lureRow[Lures.water]
        val rarityBonus = lureRow[Lures.rarityBonus]

        val total = totalKg(userId)
        val locId = userRow[Users.currentLocationId]?.value
            ?: Locations.select { Locations.unlockKg lessEq total }.orderBy(Locations.unlockKg).first()[Locations.id].value
        val locRow = Locations.select { Locations.id eq locId }.single()
        require(locRow[Locations.unlockKg] <= total) { "locked" }
        val pool = (LocationFishWeights innerJoin Fish)
            .slice(
                Fish.id,
                Fish.meanKg,
                Fish.varKg,
                Fish.rarity,
                Fish.predator,
                Fish.water,
                LocationFishWeights.weight,
            )
            .select { (LocationFishWeights.locationId eq locId) and (Fish.predator eq lurePred) and (Fish.water eq lureWater) }
            .toList()
        require(pool.isNotEmpty()) { "No suitable fish" }

        val wait = waitSeconds.coerceIn(5, 30)
        val factor = ((wait - 5).toDouble() / 25.0 + rarityBonus).coerceIn(0.0, 1.0)
        val rnd = Rng.fast()
        val totalWeight = pool.sumOf { it[LocationFishWeights.weight] * rarityModifier(it[Fish.rarity], factor) }
        var roll = rnd.nextDouble() * totalWeight
        val picked = pool.first { row2 ->
            roll -= row2[LocationFishWeights.weight] * rarityModifier(row2[Fish.rarity], factor)
            roll <= 0.0
        }

        val auto = userRow[Users.autoFishUntil]?.isAfter(Instant.now()) == true

        ensureRodInventory(userId, total)
        val rodId = ensureCurrentRod(userId, total)
        val rodRow = Rods.select { Rods.id eq rodId }.singleOrNull()
        val escapeChance = baseEscapeChance(locId) * rodBonusMultiplier(
            rodRow,
            picked[Fish.water],
            picked[Fish.predator],
        ) + extraEscapeChance

        fun escape(): HookResultDTO {
            PendingCatches.deleteWhere { PendingCatches.userId eq userId }
            Users.update({ Users.id eq userId }) {
                it[Users.isCasting] = false
                it[Users.castLureId] = null
                it[Users.lastCastAt] = Instant.now()
            }
            return HookResultDTO(false, auto)
        }

        val isBeginner = applyBeginnerProtection && isBeginnerUser(userId)

        if (!auto && !isBeginner && reactionTime >= 5.0) return@transaction escape()

        val catchChance = if (auto || isBeginner) 1.0 else {
            (1.0 - escapeChance) * (1.0 - reactionTime / 5.0).coerceIn(0.0, 1.0)
        }
        if (!auto && !isBeginner && rnd.nextDouble() > catchChance) return@transaction escape()

        val fishId = picked[Fish.id].value
        val weight = Rng.logNormalKg(picked[Fish.meanKg], picked[Fish.varKg]) * locRow[Locations.sizeMultiplier]

        val now = Instant.now()
        val updated = PendingCatches.update({ PendingCatches.userId eq userId }) {
            it[PendingCatches.fishId] = fishId
            it[PendingCatches.weight] = weight
            it[PendingCatches.locationId] = locId
            it[PendingCatches.lureId] = lureId
            it[PendingCatches.waitSeconds] = wait
            it[PendingCatches.reactionTime] = reactionTime
            it[PendingCatches.autoCatch] = auto
            it[PendingCatches.createdAt] = now
        }
        if (updated == 0) {
            PendingCatches.insert {
                it[PendingCatches.userId] = userId
                it[PendingCatches.fishId] = fishId
                it[PendingCatches.weight] = weight
                it[PendingCatches.locationId] = locId
                it[PendingCatches.lureId] = lureId
                it[PendingCatches.waitSeconds] = wait
                it[PendingCatches.reactionTime] = reactionTime
                it[PendingCatches.autoCatch] = auto
                it[PendingCatches.createdAt] = now
            }
        }

        HookResultDTO(true, auto)
    }

    fun cast(
        userId: Long,
        _waitSeconds: Int,
        _reactionTime: Double,
        success: Boolean,
        applyBeginnerProtection: Boolean = true,
    ): CastResultDTO = transaction {
        val userRow = Users.select { Users.id eq userId }.single()
        require(userRow[Users.isCasting]) { "no cast" }
        val pending = PendingCatches.select { PendingCatches.userId eq userId }.singleOrNull()
        val autoCatch = pending?.get(PendingCatches.autoCatch) ?: false
        val autoActive = userRow[Users.autoFishUntil]?.isAfter(Instant.now()) == true

        fun finish(
            caught: Boolean,
            catch: CatchDTO? = null,
            unlockedLocations: List<String> = emptyList(),
            unlockedRods: List<String> = emptyList(),
            coinsAwarded: Int = 0,
            totalCoins: Long? = null,
            todayCoins: Long? = null,
            achievements: List<AchievementUnlock> = emptyList(),
            questUpdates: List<QuestService.QuestUpdate> = emptyList(),
            questProgressChanged: Boolean = false,
        ): CastResultDTO {
            PendingCatches.deleteWhere { PendingCatches.userId eq userId }
            Users.update({ Users.id eq userId }) {
                it[Users.isCasting] = false
                it[Users.castLureId] = null
                it[Users.lastCastAt] = Instant.now()
            }
            return CastResultDTO(
                caught,
                catch,
                autoActive,
                unlockedLocations,
                unlockedRods,
                coinsAwarded,
                totalCoins,
                todayCoins,
                achievements,
                questUpdates,
                questProgressChanged,
            )
        }

        if (pending == null) return@transaction finish(false)
        val isBeginner = applyBeginnerProtection && isBeginnerUser(userId)
        if (!autoCatch && !success && !isBeginner) return@transaction finish(false)

        val fishId = pending[PendingCatches.fishId].value
        val weight = pending[PendingCatches.weight]
        val locId = pending[PendingCatches.locationId].value

        val fishRow = Fish.select { Fish.id eq fishId }.single()
        val fishName = fishRow[Fish.name]
        val rarity = fishRow[Fish.rarity]
        val locRow = Locations.select { Locations.id eq locId }.single()
        val locName = locRow[Locations.name]

        val totalBefore = totalKg(userId)
        val totalAfter = totalBefore + weight
        val unlockedLocations = if (totalAfter > totalBefore) {
            Locations.select {
                (Locations.unlockKg greater totalBefore) and (Locations.unlockKg lessEq totalAfter)
            }.orderBy(Locations.unlockKg).map { it[Locations.name] }
        } else {
            emptyList()
        }
        val unlockedRods = rodUnlocksBetween(totalBefore, totalAfter)
        ensureRodInventory(userId, totalAfter)
        ensureCurrentRod(userId, totalAfter)

        val caughtAt = Instant.now()
        val zone = ratingZone
        val catchDate = caughtAt.atZone(zone).toLocalDate()
        val coinsEarnedBefore = coinsEarnedOnDate(userId, catchDate, zone)
        val tier = locationTier(locId)
        val coinsAwarded = CoinCalculator.computeCoins(
            weight,
            rarity,
            tier,
            fishRow[Fish.water],
            coinsEarnedBefore.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        )
        val totalCoinsBefore = userRow[Users.coins]
        val totalCoinsAfter = totalCoinsBefore + coinsAwarded
        val todayCoinsAfter = coinsEarnedBefore + coinsAwarded

        val catchId = Catches.insertAndGetId {
            it[Catches.userId] = userId
            it[Catches.fishId] = fishId
            it[Catches.weight] = weight
            it[Catches.locationId] = locId
            it[Catches.createdAt] = caughtAt
            it[Catches.coins] = coinsAwarded
        }

        val achievements = AchievementService.updateOnCatch(userId, fishId, locId)
        val questResult = QuestService.updateOnCatch(
            userId = userId,
            fishName = fishName,
            rarity = rarity,
            locationId = locId,
            weight = weight,
        )
        val questUpdates = questResult.completions
        val questRewardCoins = questUpdates.sumOf { it.rewardCoins }

        if (rarity == "mythic" || rarity == "legendary") {
            clubs.logRareCatchTx(userId, fishName, locName, weight, rarity, caughtAt)
        }

        if (coinsAwarded != 0 || questRewardCoins != 0) {
            Users.update({ Users.id eq userId }) {
                it[Users.coins] = totalCoinsAfter + questRewardCoins
            }
        }

        val clubQuestResult = clubQuests.updateOnCatch(
            userId = userId,
            fishName = fishName,
            rarity = rarity,
            caughtAt = caughtAt,
        )
        val totalCoinsFinal = Users.select { Users.id eq userId }.single()[Users.coins]

        finish(
            true,
            CatchDTO(
                catchId.value,
                fishName,
                weight,
                locName,
                rarity,
                userId = userId,
                fishId = fishId,
                user = nameFromRow(userRow),
                at = caughtAt.toString(),
            ),
            unlockedLocations = unlockedLocations,
            unlockedRods = unlockedRods,
            coinsAwarded = coinsAwarded,
            totalCoins = totalCoinsFinal,
            todayCoins = todayCoinsAfter,
            achievements = achievements,
            questUpdates = questUpdates,
            questProgressChanged = questResult.progressChanged || clubQuestResult.progressChanged,
        )
    }

    fun recent(userId: Long, limit: Int = 5): List<RecentDTO> = transaction {
        (Catches innerJoin Fish innerJoin Locations)
            .slice(Catches.id, Fish.name, Fish.rarity, Catches.weight, Catches.createdAt, Locations.name)
            .selectAll().where { Catches.userId eq userId }
            .orderBy(Catches.createdAt, SortOrder.DESC)
            .limit(limit)
            .map {
                RecentDTO(
                    it[Catches.id].value,
                    it[Fish.name],
                    it[Catches.weight],
                    it[Locations.name],
                    it[Fish.rarity],
                    it[Catches.createdAt].toString()
                )
            }
    }

    fun catchById(userId: Long, catchId: Long): CatchDTO? = transaction {
        ((Catches leftJoin Users) innerJoin Fish)
            .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
            .select { (Catches.id eq catchId) and (Catches.userId eq userId) }
            .limit(1)
            .map {
                CatchDTO(
                    it[Catches.id].value,
                    it[Fish.name],
                    it[Catches.weight],
                    it[Locations.name],
                    it[Fish.rarity],
                    it[Catches.userId].value,
                    it[Fish.id].value,
                    user = catchUser(it),
                    at = it[Catches.createdAt].toString(),
                )
            }
            .singleOrNull()
    }

    private fun rarityRank(r: String) = when (r) {
        "legendary" -> 6
        "mythic" -> 5
        "epic" -> 4
        "rare" -> 3
        "uncommon" -> 2
        "common" -> 1
        else -> 0
    }

    private data class DailyPrizeCatch(
        val catchId: Long,
        val rarity: String,
        val weight: Double,
        val userId: Long,
    )

    private data class DailyRatingCatch(
        val catchId: Long,
        val locationId: Long,
        val location: String,
        val rarity: String,
        val weight: Double,
        val userId: Long,
        val fish: String,
    )

    private fun dailyPrizePreview(locationId: Long, date: LocalDate): Map<Int, Int> {
        val start = date.atStartOfDay(ratingZone).toInstant()
        val end = start.plus(Duration.ofDays(1))
        val rows = transaction {
            (Catches innerJoin Fish)
                .select {
                    (Catches.locationId eq locationId) and
                        (Catches.createdAt greaterEq start) and
                        (Catches.createdAt less end)
                }
                .map {
                    DailyPrizeCatch(
                        catchId = it[Catches.id].value,
                        rarity = it[Fish.rarity],
                        weight = it[Catches.weight],
                        userId = it[Catches.userId].value,
                    )
                }
        }
        if (rows.isEmpty()) return emptyMap()
        val playerCount = rows.map { it.userId }.toSet().size
        if (playerCount == 0) return emptyMap()
        val maxPlaces = minOf(playerCount, 10)
        if (maxPlaces <= 0) return emptyMap()
        return rows
            .sortedWith(
                compareByDescending<DailyPrizeCatch> { rarityRank(it.rarity) }
                    .thenByDescending { it.weight }
                    .thenBy { it.catchId }
            )
            .take(maxPlaces)
            .mapIndexed { index, _ ->
                val rank = index + 1
                val coins = (maxPlaces - index) * 50
                rank to coins
            }
            .toMap()
    }

    fun dailyRatingPositions(userId: Long, date: LocalDate = ZonedDateTime.now(ratingZone).toLocalDate()): List<DailyRatingPosition> {
        val start = date.atStartOfDay(ratingZone).toInstant()
        val end = start.plus(Duration.ofDays(1))
        val catches = transaction {
            (Catches innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select {
                    (Catches.createdAt greaterEq start) and
                        (Catches.createdAt less end)
                }
                .map {
                    DailyRatingCatch(
                        catchId = it[Catches.id].value,
                        locationId = it[Catches.locationId].value,
                        location = it[Locations.name],
                        rarity = it[Fish.rarity],
                        weight = it[Catches.weight],
                        userId = it[Catches.userId].value,
                        fish = it[Fish.name],
                    )
                }
        }
        if (catches.isEmpty()) return emptyList()
        val comparator = compareByDescending<DailyRatingCatch> { rarityRank(it.rarity) }
            .thenByDescending { it.weight }
            .thenBy { it.catchId }
        data class RankedCatch(val catch: DailyRatingCatch, val rank: Int)

        return catches
            .groupBy { it.locationId }
            .flatMap { (locationId, entries) ->
                if (entries.isEmpty()) return@flatMap emptyList<DailyRatingPosition>()

                val sorted = entries.sortedWith(comparator)
                val uniquePlayers = entries.map { it.userId }.toSet().size
                if (uniquePlayers <= 0) return@flatMap emptyList<DailyRatingPosition>()

                val prizePlaces = minOf(sorted.size, uniquePlayers, 10)
                val rankedCatches = sorted.mapIndexedNotNull { index, catch ->
                    if (catch.userId != userId) {
                        null
                    } else {
                        RankedCatch(catch, index + 1)
                    }
                }

                if (rankedCatches.isEmpty()) return@flatMap emptyList<DailyRatingPosition>()

                val prizeCatches = rankedCatches.filter { it.rank <= prizePlaces }
                val topNonPrize = rankedCatches.firstOrNull { it.rank > prizePlaces }
                val selected = when {
                    prizeCatches.isNotEmpty() && topNonPrize != null -> prizeCatches + topNonPrize
                    prizeCatches.isNotEmpty() -> prizeCatches
                    topNonPrize != null -> listOf(topNonPrize)
                    else -> emptyList()
                }

                selected.map { ranked ->
                    val rank = ranked.rank
                    val prizeCoins = if (rank <= prizePlaces) {
                        (prizePlaces - rank + 1) * 50
                    } else {
                        null
                    }
                    DailyRatingPosition(
                        locationId = locationId,
                        location = ranked.catch.location,
                        rank = rank,
                        participants = uniquePlayers,
                        bestFish = ranked.catch.fish,
                        bestRarity = ranked.catch.rarity,
                        bestWeight = ranked.catch.weight,
                        prizeCoins = prizeCoins,
                    )
                }
            }
            .sortedWith(compareBy<DailyRatingPosition> { it.location }.thenBy { it.rank })
    }

    private fun sortCatches(list: List<CatchDTO>, limit: Int, asc: Boolean = false) =
        if (asc) {
            list.sortedWith(
                compareBy<CatchDTO> { rarityRank(it.rarity) }
                    .thenBy { it.weight }
            ).take(limit)
        } else {
            list.sortedWith(
                compareByDescending<CatchDTO> { rarityRank(it.rarity) }
                    .thenByDescending { it.weight }
            ).take(limit)
        }

    private fun periodRange(period: String): Pair<Instant?, Instant?> {
        val zone = ratingZone
        val today = LocalDate.now(zone)
        val start = when (period) {
            "today" -> today.atStartOfDay(zone).toInstant()
            "yesterday" -> today.minusDays(1).atStartOfDay(zone).toInstant()
            "week" -> today.minusWeeks(1).atStartOfDay(zone).toInstant()
            "month" -> today.minusMonths(1).atStartOfDay(zone).toInstant()
            "year" -> today.minusYears(1).atStartOfDay(zone).toInstant()
            else -> null
        }
        val end = when (period) {
            "today", "yesterday" -> start?.plus(Duration.ofDays(1))
            else -> null
        }
        return Pair(start, end)
    }

    fun personalTopByLocation(
        userId: Long,
        locationId: Long?,
        period: String = "all",
        asc: Boolean = false,
        limit: Int = 50,
    ): List<CatchDTO> {
        val (start, end) = periodRange(period)
        val catches = transaction {
            var cond: Op<Boolean> = Catches.userId eq userId
            if (locationId != null) cond = cond and (Catches.locationId eq locationId)
            if (start != null) cond = cond and (Catches.createdAt greaterEq start)
            if (end != null) cond = cond and (Catches.createdAt less end)
            ((Catches leftJoin Users) innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select { cond }
                .map {
                    CatchDTO(
                        it[Catches.id].value,
                        it[Fish.name],
                        it[Catches.weight],
                        it[Locations.name],
                        it[Fish.rarity],
                        it[Catches.userId].value,
                        it[Fish.id].value,
                        user = catchUser(it),
                        at = it[Catches.createdAt].toString(),
                    )
                }
        }
        return sortCatches(catches, limit, asc)
    }

    fun personalTopBySpecies(
        userId: Long,
        fishId: Long?,
        period: String = "all",
        asc: Boolean = false,
        limit: Int = 50,
    ): List<CatchDTO> {
        val (start, end) = periodRange(period)
        val catches = transaction {
            var cond: Op<Boolean> = Catches.userId eq userId
            if (fishId != null) cond = cond and (Catches.fishId eq fishId)
            if (start != null) cond = cond and (Catches.createdAt greaterEq start)
            if (end != null) cond = cond and (Catches.createdAt less end)
            ((Catches leftJoin Users) innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select { cond }
                .map {
                    CatchDTO(
                        it[Catches.id].value,
                        it[Fish.name],
                        it[Catches.weight],
                        it[Locations.name],
                        it[Fish.rarity],
                        it[Catches.userId].value,
                        it[Fish.id].value,
                        user = catchUser(it),
                        at = it[Catches.createdAt].toString(),
                    )
                }
        }
        return sortCatches(catches, limit, asc)
    }

    fun globalTopByLocation(
        locationId: Long?,
        period: String = "all",
        asc: Boolean = false,
        limit: Int = 50,
    ): List<CatchDTO> {
        val (start, end) = periodRange(period)
        val catches = transaction {
            var cond: Op<Boolean> = Op.TRUE
            if (locationId != null) cond = cond and (Catches.locationId eq locationId)
            if (start != null) cond = cond and (Catches.createdAt greaterEq start)
            if (end != null) cond = cond and (Catches.createdAt less end)
            ((Catches leftJoin Users) innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select { cond }
                .map {
                    CatchDTO(
                        it[Catches.id].value,
                        it[Fish.name],
                        it[Catches.weight],
                        it[Locations.name],
                        it[Fish.rarity],
                        it[Catches.userId].value,
                        it[Fish.id].value,
                        user = catchUser(it),
                        at = it[Catches.createdAt].toString(),
                    )
                }
        }
        val sorted = sortCatches(catches, limit, asc)
        if (!asc && locationId != null) {
            val nowInZone = ZonedDateTime.now(ratingZone)
            val prizeDate = when (period) {
                "today" -> nowInZone.toLocalDate()
                "yesterday" -> nowInZone.minusDays(1).toLocalDate()
                else -> null
            }
            if (prizeDate != null) {
                val coinsByRank = dailyPrizePreview(locationId, prizeDate)
                if (coinsByRank.isNotEmpty()) {
                    return sorted.mapIndexed { index, catch ->
                        val rank = index + 1
                        val coins = coinsByRank[rank]
                        if (coins != null) {
                            catch.copy(rank = rank, prizeCoins = coins)
                        } else {
                            catch
                        }
                    }
                }
            }
        }
        return sorted
    }

    fun globalTopBySpecies(
        fishId: Long?,
        period: String = "all",
        asc: Boolean = false,
        limit: Int = 50,
    ): List<CatchDTO> {
        val (start, end) = periodRange(period)
        val catches = transaction {
            var cond: Op<Boolean> = Op.TRUE
            if (fishId != null) cond = cond and (Catches.fishId eq fishId)
            if (start != null) cond = cond and (Catches.createdAt greaterEq start)
            if (end != null) cond = cond and (Catches.createdAt less end)
            ((Catches leftJoin Users) innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select { cond }
                .map {
                    CatchDTO(
                        it[Catches.id].value,
                        it[Fish.name],
                        it[Catches.weight],
                        it[Locations.name],
                        it[Fish.rarity],
                        it[Catches.userId].value,
                        it[Fish.id].value,
                        user = catchUser(it),
                        at = it[Catches.createdAt].toString(),
                    )
                }
        }
        return sortCatches(catches, limit, asc)
    }
}
