package service

import db.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import util.Rng
import java.time.*

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
    fun ensureUserByTgId(tgId: Long): Long = transaction {
        Users.selectAll().where { Users.tgId eq tgId }.singleOrNull()?.get(Users.id)?.value ?: run {
            Users.insertAndGetId {
                it[Users.tgId] = tgId
                it[level] = 1; it[xp] = 0; it[createdAt] = Instant.now()
            }.value
        }
    }

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

    fun giveDailyBaits(userId: Long, freshQty: Int = 10, predQty: Int = 5): Pair<List<LureDTO>, Long?>? = transaction {
        val today = LocalDate.now()
        val row = Users.selectAll().where { Users.id eq userId }.forUpdate().single()
        val last = row[Users.lastDailyAt]?.atZone(ZoneId.systemDefault())?.toLocalDate()
        if (last == today) return@transaction null
        val freshId = Lures.select { Lures.name eq "Пресная мирная" }.single()[Lures.id].value
        val predId = Lures.select { Lures.name eq "Пресная хищная" }.single()[Lures.id].value
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
        add(freshId, freshQty)
        add(predId, predQty)
        Users.update({ Users.id eq userId }) { it[lastDailyAt] = Instant.now() }
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

    fun canClaimDaily(userId: Long): Boolean = transaction {
        val today = LocalDate.now()
        val last = Users.selectAll().where { Users.id eq userId }.singleOrNull()?.get(Users.lastDailyAt)
            ?.atZone(ZoneId.systemDefault())?.toLocalDate()
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
                    39,
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
    )

    fun listShop(): List<ShopCategory> = shopCategories

    fun buyPackage(userId: Long, packageId: String): Pair<List<LureDTO>, Long?> = transaction {
        val pack = shopCategories.flatMap { it.packs }.find { it.id == packageId }
            ?: error("bad package")
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

    fun setLure(userId: Long, lureId: Long) = transaction {
        val has = InventoryLures.select { (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }
            .singleOrNull()?.get(InventoryLures.qty) ?: 0
        require(has > 0) { "no lure" }
        Users.update({ Users.id eq userId }) { it[currentLureId] = lureId }
    }

    fun setLocation(userId: Long, locationId: Long) = transaction {
        val loc = Locations.selectAll().where { Locations.id eq locationId }.singleOrNull()
            ?: error("bad location")
        val total = totalKg(userId)
        require(loc[Locations.unlockKg] <= total) { "locked" }
        Users.update({ Users.id eq userId }) { it[currentLocationId] = locationId }
    }

    @Serializable
    data class CatchDTO(val fish: String, val weight: Double, val location: String, val rarity: String)

    @Serializable
    data class CastResultDTO(val caught: Boolean, val catch: CatchDTO? = null)

    private fun rarityModifier(rarity: String, factor: Double): Double = when (rarity) {
        "common" -> 1.0 - 0.8 * factor
        "uncommon" -> 0.6 + 0.4 * factor
        "rare" -> 0.3 + 0.7 * factor
        "epic" -> 0.2 + 0.8 * factor
        "legendary" -> 0.1 + 0.9 * factor
        else -> 1.0
    }

    fun cast(userId: Long, waitSeconds: Int, reactionTime: Double): CastResultDTO = transaction {
        val lureId = Users.select { Users.id eq userId }.single()[Users.currentLureId]?.value
            ?: error("No lure selected")
        val lureRow = (InventoryLures innerJoin Lures)
            .select { (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }
            .forUpdate().singleOrNull() ?: error("No baits")
        val q = lureRow[InventoryLures.qty]; require(q > 0) { "No baits" }
        InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }) {
            it[InventoryLures.qty] = q - 1
        }

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

        if (reactionTime >= 5.0) return@transaction CastResultDTO(false)
        val catchChance = 1.0 - reactionTime / 5.0
        if (rnd.nextDouble() > catchChance) return@transaction CastResultDTO(false)

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
        CastResultDTO(true, CatchDTO(fishName, weight, locName, rarity))
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
}
