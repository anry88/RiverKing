package service

import db.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import util.Rng
import util.sanitizeName
import java.time.*
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class LocationDTO(
    val id: Long,
    val name: String,
    val unlockKg: Double,
    val unlocked: Boolean,
)

@Serializable
data class RecentDTO(val fish: String, val weight: Double, val location: String, val rarity: String, val at: String)

class FishingService {
    fun ensureUserByTgId(
        tgId: Long,
        firstName: String? = null,
        lastName: String? = null,
        username: String? = null,
        language: String? = null,
    ): Long = transaction {
        val existing = Users.selectAll().where { Users.tgId eq tgId }.singleOrNull()
        if (existing == null) {
            val freshId = Lures.select { Lures.name eq "Пресная мирная" }.single()[Lures.id].value
            val predId = Lures.select { Lures.name eq "Пресная хищная" }.single()[Lures.id].value
            val newId = Users.insertAndGetId {
                it[Users.tgId] = tgId
                it[level] = 1; it[xp] = 0; it[createdAt] = Instant.now()
                it[Users.firstName] = firstName
                it[Users.lastName] = lastName
                it[Users.username] = username
                it[Users.language] = if (language?.startsWith("ru") == true) "ru" else "en"
                it[currentLureId] = freshId
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
            newId
        } else {
            val id = existing[Users.id].value
            if (firstName != null || lastName != null || username != null) {
                Users.update({ Users.id eq id }) {
                    if (firstName != null) it[Users.firstName] = firstName
                    if (lastName != null) it[Users.lastName] = lastName
                    if (username != null) it[Users.username] = username
                }
            }
            id
        }
    }

    fun setNickname(userId: Long, nickname: String) = transaction {
        Users.update({ Users.id eq userId }) { it[Users.nickname] = sanitizeName(nickname) }
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

    fun displayName(userId: Long): String? = transaction {
        Users.select { Users.id eq userId }.singleOrNull()?.let { nameFromRow(it) }
    }

    fun resetCasting(userId: Long) = transaction {
        Users.update({ Users.id eq userId }) {
            it[Users.isCasting] = false
            it[Users.castLureId] = null
        }
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

    private fun totalKg(userId: Long) =
        Catches.slice(Catches.weight.sum()).selectAll().where { Catches.userId eq userId }
            .singleOrNull()?.get(Catches.weight.sum()) ?: 0.0

    fun totalCaughtKg(userId: Long): Double = transaction { totalKg(userId) }

    fun todayCaughtKg(userId: Long): Double = transaction {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        Catches.slice(Catches.weight.sum()).selectAll()
            .where { (Catches.userId eq userId) and (Catches.createdAt greaterEq start) }
            .singleOrNull()?.get(Catches.weight.sum()) ?: 0.0
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

    fun giveDailyBaits(userId: Long): Triple<List<LureDTO>, Long?, Int>? = transaction {
        val tz = ZoneId.of("Europe/Belgrade")
        val today = LocalDate.now(tz)
        val row = Users.selectAll().where { Users.id eq userId }.forUpdate().single()
        val last = row[Users.lastDailyAt]?.atZone(tz)?.toLocalDate()
        var streak = row[Users.dailyStreak]
        if (last == today) return@transaction null

        streak = if (last == today.minusDays(1)) streak + 1 else 1

        val freshId = Lures.select { Lures.name eq "Пресная мирная" }.single()[Lures.id].value
        val predId = Lures.select { Lures.name eq "Пресная хищная" }.single()[Lures.id].value
        val saltFreshId = Lures.select { Lures.name eq "Морская мирная" }.single()[Lures.id].value
        val saltPredId = Lures.select { Lures.name eq "Морская хищная" }.single()[Lures.id].value
        val freshPlusId = Lures.select { Lures.name eq "Пресная мирная+" }.single()[Lures.id].value
        val predPlusId = Lures.select { Lures.name eq "Пресная хищная+" }.single()[Lures.id].value

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

        val rewardDay = streak.coerceAtMost(7)

        when (rewardDay) {
            1 -> { add(freshId, 10); add(predId, 5) }
            2 -> { add(freshId, 10); add(predId, 10) }
            3 -> { add(freshId, 15); add(predId, 10) }
            4 -> { add(freshId, 15); add(predId, 15) }
            5 -> { add(freshId, 15); add(predId, 15); add(saltFreshId, 5) }
            6 -> { add(freshId, 15); add(predId, 15); add(saltFreshId, 5); add(saltPredId, 5) }
            7 -> {
                add(freshId, 15); add(predId, 15); add(saltFreshId, 5); add(saltPredId, 5)
                add(freshPlusId, 1); add(predPlusId, 1)
            }
        }

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

    fun canClaimDaily(userId: Long): Boolean = transaction {
        val tz = ZoneId.of("Europe/Belgrade")
        val today = LocalDate.now(tz)
        val last = Users.selectAll().where { Users.id eq userId }.singleOrNull()?.get(Users.lastDailyAt)
            ?.atZone(tz)?.toLocalDate()
        last != today
    }

    @Serializable
    data class LureDTO(val id: Long, val name: String, val qty: Int, val predator: Boolean, val water: String, val rarityBonus: Double)

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
    data class GuideDTO(
        val locations: List<GuideLocationDTO>,
        val fish: List<GuideFishDTO>,
        val lures: List<GuideLureDTO>,
    )

    fun guide(lang: String): GuideDTO = transaction {
        fun rarityRank(r: String) = when(r) {
            "common" -> 0
            "uncommon" -> 1
            "rare" -> 2
            "epic" -> 3
            "legendary" -> 4
            else -> 5
        }

        // Locations with fish and possible lures, ordered by unlock requirement
        val locationDtos = Locations.selectAll().orderBy(Locations.unlockKg).map { locRow ->
            val locId = locRow[Locations.id].value
            val locName = I18n.location(locRow[Locations.name], lang)
            val fishRows = (LocationFishWeights innerJoin Fish)
                .select { LocationFishWeights.locationId eq locId }
                .map { FishBriefDTO(I18n.fish(it[Fish.name], lang), it[Fish.rarity]) }
                .sortedBy { rarityRank(it.rarity) }
            val waters = (LocationFishWeights innerJoin Fish)
                .slice(Fish.water)
                .select { LocationFishWeights.locationId eq locId }
                .map { it[Fish.water] }
                .distinct()
            val waterFilter = with(SqlExpressionBuilder) {
                if (waters.isNotEmpty()) Lures.water inList waters else Lures.water eq "fresh"
            }
            val lureNames = Lures.select { waterFilter }.map { I18n.lure(it[Lures.name], lang) }
            GuideLocationDTO(locId, locName, fishRows, lureNames)
        }

        // Fish with locations and matching lures
        val fishDtos = Fish.selectAll().map { fRow ->
            val fid = fRow[Fish.id].value
            val name = I18n.fish(fRow[Fish.name], lang)
            val rarity = fRow[Fish.rarity]
            val pred = fRow[Fish.predator]
            val water = fRow[Fish.water]
            val locations = (LocationFishWeights innerJoin Locations)
                .select { LocationFishWeights.fishId eq fid }
                .map { I18n.location(it[Locations.name], lang) }
            val lures = Lures.select { (Lures.predator eq pred) and (Lures.water eq water) }
                .map { I18n.lure(it[Lures.name], lang) }
            GuideFishDTO(fid, name, rarity, locations, lures)
        }.sortedBy { rarityRank(it.rarity) }

        // Lures with fish and locations
        val lureDtos = Lures.selectAll().map { lRow ->
            val name = I18n.lure(lRow[Lures.name], lang)
            val pred = lRow[Lures.predator]
            val water = lRow[Lures.water]
            val fishList = Fish.select { (Fish.predator eq pred) and (Fish.water eq water) }
                .map { FishBriefDTO(I18n.fish(it[Fish.name], lang), it[Fish.rarity]) }
                .sortedBy { rarityRank(it.rarity) }
            val locations = (LocationFishWeights innerJoin Fish innerJoin Locations)
                .slice(Locations.name)
                .select { (Fish.predator eq pred) and (Fish.water eq water) }
                .map { I18n.location(it[Locations.name], lang) }
                .distinct()
            GuideLureDTO(name, fishList, locations)
        }

        GuideDTO(locationDtos, fishDtos, lureDtos)
    }

    @Serializable
    data class ShopPackage(
        val id: String,
        val name: String,
        val desc: String,
        val price: Int,
        val items: List<Pair<String, Int>>,
    )

    @Serializable
    data class ShopCategory(
        val id: String,
        val name: String,
        val packs: List<ShopPackage>,
    )

    private val shopCategories = listOf(
        ShopCategory(
            "fresh_basic",
            "Пресные простые",
            listOf(
                ShopPackage(
                    "fresh_topup_s",
                    "Пополнение S",
                    "20 пресных простых: 10 мирных и 10 хищных",
                    1,
//                    39,
                    listOf("Пресная мирная" to 10, "Пресная хищная" to 10)
                ),
                ShopPackage(
                    "fresh_stock_m",
                    "Запас M",
                    "50 пресных простых: 25 мирных и 25 хищных",
                    89,
                    listOf("Пресная мирная" to 25, "Пресная хищная" to 25)
                ),
                ShopPackage(
                    "fresh_crate_l",
                    "Ящик L",
                    "120 пресных простых: 60 мирных и 60 хищных",
                    199,
                    listOf("Пресная мирная" to 60, "Пресная хищная" to 60)
                ),
            )
        ),
        ShopCategory(
            "salt_basic",
            "Морские простые",
            listOf(
                ShopPackage(
                    "salt_topup_s",
                    "Пополнение S",
                    "20 морских простых: 10 мирных и 10 хищных",
                    55,
                    listOf("Морская мирная" to 10, "Морская хищная" to 10)
                ),
                ShopPackage(
                    "salt_stock_m",
                    "Запас M",
                    "50 морских простых: 25 мирных и 25 хищных",
                    129,
                    listOf("Морская мирная" to 25, "Морская хищная" to 25)
                ),
                ShopPackage(
                    "salt_crate_l",
                    "Ящик L",
                    "120 морских простых: 60 мирных и 60 хищных",
                    299,
                    listOf("Морская мирная" to 60, "Морская хищная" to 60)
                ),
            )
        ),
        ShopCategory(
            "fresh_boost",
            "Пресные улучшенные",
            listOf(
                ShopPackage(
                    "fresh_boost_s",
                    "Буст S",
                    "10 пресных улучшенных: 5 мирных и 5 хищных",
                    69,
                    listOf("Пресная мирная+" to 5, "Пресная хищная+" to 5)
                ),
                ShopPackage(
                    "fresh_boost_m",
                    "Буст M",
                    "25 пресных улучшенных: 12 мирных и 13 хищных",
                    159,
                    listOf("Пресная мирная+" to 12, "Пресная хищная+" to 13)
                ),
                ShopPackage(
                    "fresh_boost_l",
                    "Буст L",
                    "60 пресных улучшенных: 30 мирных и 30 хищных",
                    349,
                    listOf("Пресная мирная+" to 30, "Пресная хищная+" to 30)
                ),
            )
        ),
        ShopCategory(
            "salt_boost",
            "Морские улучшенные",
            listOf(
                ShopPackage(
                    "salt_boost_s",
                    "Буст S",
                    "10 морских улучшенных: 5 мирных и 5 хищных",
                    99,
                    listOf("Морская мирная+" to 5, "Морская хищная+" to 5)
                ),
                ShopPackage(
                    "salt_boost_m",
                    "Буст M",
                    "25 морских улучшенных: 12 мирных и 13 хищных",
                    239,
                    listOf("Морская мирная+" to 12, "Морская хищная+" to 13)
                ),
                ShopPackage(
                    "salt_boost_l",
                    "Буст L",
                    "60 морских улучшенных: 30 мирных и 30 хищных",
                    549,
                    listOf("Морская мирная+" to 30, "Морская хищная+" to 30)
                ),
            )
        ),
        ShopCategory(
            "mixed",
            "Смешанные",
            listOf(
                ShopPackage(
                    "bundle_starter",
                    "Стартовый набор",
                    "40 пресных простых (20 мирных и 20 хищных), 20 морских простых (10 мирных и 10 хищных) и 5 пресных улучшенных (3 мирные+ и 2 хищные+)",
                    129,
                    listOf(
                        "Пресная мирная" to 20,
                        "Пресная хищная" to 20,
                        "Морская мирная" to 10,
                        "Морская хищная" to 10,
                        "Пресная мирная+" to 3,
                        "Пресная хищная+" to 2,
                    )
                ),
                ShopPackage(
                    "bundle_pro",
                    "Профи рыболов",
                    "80 пресных простых (40 мирных и 40 хищных), 40 морских простых (20 мирных и 20 хищных), 15 пресных улучшенных (8 мирных+ и 7 хищных+) и 5 морских улучшенных (3 мирные+ и 2 хищные+)",
                    319,
                    listOf(
                        "Пресная мирная" to 40,
                        "Пресная хищная" to 40,
                        "Морская мирная" to 20,
                        "Морская хищная" to 20,
                        "Пресная мирная+" to 8,
                        "Пресная хищная+" to 7,
                        "Морская мирная+" to 3,
                        "Морская хищная+" to 2,
                    )
                ),
                ShopPackage(
                    "bundle_whale",
                    "Китовый ящик",
                    "200 пресных простых (100 мирных и 100 хищных), 120 морских простых (60 мирных и 60 хищных), 40 пресных улучшенных (20 мирных+ и 20 хищных+) и 20 морских улучшенных (10 мирных+ и 10 хищных+)",
                    869,
                    listOf(
                        "Пресная мирная" to 100,
                        "Пресная хищная" to 100,
                        "Морская мирная" to 60,
                        "Морская хищная" to 60,
                        "Пресная мирная+" to 20,
                        "Пресная хищная+" to 20,
                        "Морская мирная+" to 10,
                        "Морская хищная+" to 10,
                    )
                ),
            )
        ),
        ShopCategory(
            "starter",
            "Стартовые",
            listOf(
                ShopPackage(
                    "micro_pred_fresh",
                    "Пополнение пресных хищных",
                    "15 пресных хищных",
                    29,
                    listOf("Пресная хищная" to 15)
                ),
                ShopPackage(
                    "micro_salt_starter",
                    "Морской старт",
                    "10 морских простых: 5 мирных и 5 хищных",
                    25,
                    listOf("Морская мирная" to 5, "Морская хищная" to 5)
                ),
            )
        ),
        ShopCategory(
            "subscriptions",
            "Подписки",
            listOf(
                ShopPackage(
                    "autofish",
                    "Автоловля",
                    "Робот ловит за вас целый месяц и не упустит ни одной рыбы",
                    499,
                    emptyList()
                ),
            )
        ),
    )

    fun listShop(lang: String): List<ShopCategory> = shopCategories.map { cat ->
        cat.copy(
            name = I18n.text(cat.name, lang),
            packs = cat.packs.map { p ->
                p.copy(
                    name = I18n.text(p.name, lang),
                    desc = I18n.text(p.desc, lang),
                    items = p.items.map { I18n.lure(it.first, lang) to it.second }
                )
            }
        )
    }

    fun buyPackage(userId: Long, packageId: String): Pair<List<LureDTO>, Long?> = transaction {
        val pack = shopCategories.flatMap { it.packs }.find { it.id == packageId }
            ?: error("bad package")

        if (packageId == "autofish") {
            val row = Users.select { Users.id eq userId }.forUpdate().single()
            val cur = row[Users.autoFishUntil]
            val base = if (cur != null && cur.isAfter(Instant.now())) cur else Instant.now()
            Users.update({ Users.id eq userId }) {
                it[autoFishUntil] = base.atZone(ZoneId.systemDefault()).plusMonths(1).toInstant()
            }
            return@transaction Pair(emptyList(), null)
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
    fun startCast(userId: Long): Long? = transaction {
        val userRow = Users.select { Users.id eq userId }.single()
        require(!userRow[Users.isCasting]) { "casting" }
        val lureId = userRow[Users.currentLureId]?.value
            ?: error("No lure selected")
        val lureRow = (InventoryLures innerJoin Lures)
            .select { (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }
            .forUpdate().singleOrNull() ?: error("No baits")
        val q = lureRow[InventoryLures.qty]; require(q > 0) { "No baits" }

        val lurePred = lureRow[Lures.predator]
        val lureWater = lureRow[Lures.water]

        val total = totalKg(userId)
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
        newLure
    }

    @Serializable
    data class CatchDTO(
        val fish: String,
        val weight: Double,
        val location: String,
        val rarity: String,
        val userId: Long? = null,
        val fishId: Long? = null,
        val user: String? = null,
        val at: String? = null,
    )

    @Serializable
    data class CastResultDTO(val caught: Boolean, val catch: CatchDTO? = null, val autoFish: Boolean = false)

    private fun rarityModifier(rarity: String, factor: Double): Double = when (rarity) {
        "common" -> 1.0 - 0.7 * factor
        "uncommon" -> 0.6 + 0.3 * factor
        "rare" -> 0.3 + 0.4 * factor
        "epic" -> 0.2 + 0.3 * factor
        "legendary" -> 0.1 + 0.2 * factor
        else -> 1.0
    }

    fun cast(userId: Long, waitSeconds: Int, reactionTime: Double): CastResultDTO = transaction {
        val userRow = Users.select { Users.id eq userId }.single()
        require(userRow[Users.isCasting]) { "no cast" }
        val lureId = userRow[Users.castLureId]?.value
            ?: error("No lure selected")
        val lureRow = Lures.select { Lures.id eq lureId }.single()

        val lurePred = lureRow[Lures.predator]
        val lureWater = lureRow[Lures.water]
        val rarityBonus = lureRow[Lures.rarityBonus]

        val total = totalKg(userId)
        val locId = Users.select { Users.id eq userId }.single()[Users.currentLocationId]?.value
            ?: Locations.select { Locations.unlockKg lessEq total }.orderBy(Locations.unlockKg).first()[Locations.id].value
        val locRow = Locations.select { Locations.id eq locId }.single()
        require(locRow[Locations.unlockKg] <= total) { "locked" }
        val pool = (LocationFishWeights innerJoin Fish)
            .slice(Fish.id, Fish.name, Fish.meanKg, Fish.varKg, Fish.rarity, LocationFishWeights.weight)
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

        fun finish(res: CastResultDTO): CastResultDTO {
            Users.update({ Users.id eq userId }) {
                it[Users.isCasting] = false
                it[Users.castLureId] = null
                it[Users.lastCastAt] = Instant.now()
            }
            return res
        }

        val auto = userRow[Users.autoFishUntil]?.isAfter(Instant.now()) == true
        if (!auto && reactionTime >= 5.0) return@transaction finish(CastResultDTO(false, autoFish = auto))
        val catchChance = if (auto) 1.0 else 1.0 - reactionTime / 5.0
        if (!auto && rnd.nextDouble() > catchChance) return@transaction finish(CastResultDTO(false, autoFish = auto))

        val fishId = picked[Fish.id].value
        val fishName = picked[Fish.name]
        val rarity = picked[Fish.rarity]
        val weight = Rng.logNormalKg(picked[Fish.meanKg], picked[Fish.varKg]) * locRow[Locations.sizeMultiplier]

        Catches.insert {
            it[Catches.userId] = userId
            it[Catches.fishId] = fishId
            it[Catches.weight] = weight
            it[Catches.locationId] = locId
            it[Catches.createdAt] = Instant.now()
        }
        val locName = locRow[Locations.name]
        finish(
            CastResultDTO(
                true,
                CatchDTO(
                    fishName,
                    weight,
                    locName,
                    rarity,
                    userId = null,
                    fishId = fishId,
                ),
                auto
            )
        )
    }

    fun recent(userId: Long, limit: Int = 5): List<RecentDTO> = transaction {
        (Catches innerJoin Fish innerJoin Locations)
            .slice(Fish.name, Fish.rarity, Catches.weight, Catches.createdAt, Locations.name)
            .selectAll().where { Catches.userId eq userId }
            .orderBy(Catches.createdAt, SortOrder.DESC)
            .limit(limit)
            .map {
                RecentDTO(
                    it[Fish.name],
                    it[Catches.weight],
                    it[Locations.name],
                    it[Fish.rarity],
                    it[Catches.createdAt].toString()
                )
            }
    }

    private fun rarityRank(r: String) = when (r) {
        "legendary" -> 5
        "epic" -> 4
        "rare" -> 3
        "uncommon" -> 2
        "common" -> 1
        else -> 0
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
        val zone = ZoneId.systemDefault()
        val start = when (period) {
            "today" -> LocalDate.now().atStartOfDay(zone).toInstant()
            "yesterday" -> LocalDate.now().minusDays(1).atStartOfDay(zone).toInstant()
            "week" -> LocalDate.now().minusWeeks(1).atStartOfDay(zone).toInstant()
            "month" -> LocalDate.now().minusMonths(1).atStartOfDay(zone).toInstant()
            "year" -> LocalDate.now().minusYears(1).atStartOfDay(zone).toInstant()
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
        locationId: Long,
        period: String = "all",
        asc: Boolean = false,
        limit: Int = 50,
    ): List<CatchDTO> {
        val (start, end) = periodRange(period)
        val catches = transaction {
            var cond: Op<Boolean> = (Catches.userId eq userId) and (Catches.locationId eq locationId)
            if (start != null) cond = cond and (Catches.createdAt greaterEq start)
            if (end != null) cond = cond and (Catches.createdAt less end)
            ((Catches leftJoin Users) innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select { cond }
                .map {
                    CatchDTO(
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
        fishId: Long,
        period: String = "all",
        asc: Boolean = false,
        limit: Int = 50,
    ): List<CatchDTO> {
        val (start, end) = periodRange(period)
        val catches = transaction {
            var cond: Op<Boolean> = (Catches.userId eq userId) and (Catches.fishId eq fishId)
            if (start != null) cond = cond and (Catches.createdAt greaterEq start)
            if (end != null) cond = cond and (Catches.createdAt less end)
            ((Catches leftJoin Users) innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select { cond }
                .map {
                    CatchDTO(
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
        locationId: Long,
        period: String = "all",
        asc: Boolean = false,
        limit: Int = 50,
    ): List<CatchDTO> {
        val (start, end) = periodRange(period)
        val catches = transaction {
            var cond: Op<Boolean> = Catches.locationId eq locationId
            if (start != null) cond = cond and (Catches.createdAt greaterEq start)
            if (end != null) cond = cond and (Catches.createdAt less end)
            ((Catches leftJoin Users) innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select { cond }
                .map {
                    CatchDTO(
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

    fun globalTopBySpecies(
        fishId: Long,
        period: String = "all",
        asc: Boolean = false,
        limit: Int = 50,
    ): List<CatchDTO> {
        val (start, end) = periodRange(period)
        val catches = transaction {
            var cond: Op<Boolean> = Catches.fishId eq fishId
            if (start != null) cond = cond and (Catches.createdAt greaterEq start)
            if (end != null) cond = cond and (Catches.createdAt less end)
            ((Catches leftJoin Users) innerJoin Fish)
                .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
                .select { cond }
                .map {
                    CatchDTO(
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
