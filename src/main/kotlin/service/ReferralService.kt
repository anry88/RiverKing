package service

import db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import kotlin.math.ceil

object ReferralService {
    fun generateLink(userId: Long): String = transaction {
        val token = java.util.UUID.randomUUID().toString().replace("-", "")
        ReferralLinks.insert {
            it[ReferralLinks.userId] = userId
            it[ReferralLinks.token] = token
            it[ReferralLinks.createdAt] = Instant.now()
        }
        token
    }

    fun currentLink(userId: Long): String? = transaction {
        ReferralLinks.select { ReferralLinks.userId eq userId }
            .orderBy(ReferralLinks.createdAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull()?.get(ReferralLinks.token)
    }

    fun invited(userId: Long): List<Long> = transaction {
        Users.select { Users.referredBy eq userId }.map { it[Users.id].value }
    }

    fun setReferrer(newUserId: Long, token: String) = transaction {
        val link = ReferralLinks.select { ReferralLinks.token eq token }.singleOrNull() ?: return@transaction
        val inviter = link[ReferralLinks.userId].value
        if (inviter != newUserId) {
            Users.update({ Users.id eq newUserId }) { it[referredBy] = inviter }
            ReferralRewards.insert {
                it[userId] = inviter
                it[packageId] = "bundle_starter"
                it[qty] = 1
            }
            ReferralRewards.insert {
                it[userId] = newUserId
                it[packageId] = "bundle_starter"
                it[qty] = 1
            }
        }
    }

    fun onPurchase(buyerId: Long, pack: FishingService.ShopPackage) = transaction {
        val ref = Users.select { Users.id eq buyerId }.single()[Users.referredBy]?.value ?: return@transaction
        for ((name, qty) in pack.items) {
            if (qty > 0) {
                val rewardQty = ceil(qty / 4.0).toInt().coerceAtLeast(1)
                val lure = Lures.select { Lures.name eq name }.single()[Lures.id]
                ReferralRewards.insert {
                    it[userId] = ref
                    it[lureId] = lure
                    it[ReferralRewards.qty] = rewardQty
                }
            }
        }
        if (pack.id == "autofish") {
            ReferralRewards.insert {
                it[userId] = ref
                it[packageId] = "autofish_week"
                it[ReferralRewards.qty] = 1
            }
        }
    }

    data class Reward(val id: Long, val packageId: String, val qty: Int)

    data class RewardSimple(val packageId: String, val qty: Int)

    fun pendingRewards(userId: Long): List<Reward> = transaction {
        ReferralRewards.select { (ReferralRewards.userId eq userId) and (ReferralRewards.claimed eq false) }
            .map {
                val pkg = it[ReferralRewards.packageId] ?: Lures.select { Lures.id eq it[ReferralRewards.lureId]!!.value }
                    .single()[Lures.name]
                Reward(it[ReferralRewards.id].value, pkg, it[ReferralRewards.qty])
            }
    }

    fun pendingRewardsSimple(userId: Long): List<RewardSimple> = transaction {
        ReferralRewards.select { (ReferralRewards.userId eq userId) and (ReferralRewards.claimed eq false) }
            .map {
                val pkg = it[ReferralRewards.packageId] ?: Lures.select { Lures.id eq it[ReferralRewards.lureId]!!.value }
                    .single()[Lures.name]
                RewardSimple(pkg, it[ReferralRewards.qty])
            }
            .groupBy { it.packageId }
            .map { RewardSimple(it.key, it.value.sumOf { r -> r.qty }) }
    }

    fun claimAllRewards(userId: Long, fishing: FishingService): Pair<List<FishingService.LureDTO>, Long?> {
        data class Raw(val lureId: Long?, val packageId: String?, val qty: Int)
        val rewards = transaction {
            val rows = ReferralRewards.select {
                (ReferralRewards.userId eq userId) and (ReferralRewards.claimed eq false)
            }.map {
                Raw(it[ReferralRewards.lureId]?.value, it[ReferralRewards.packageId], it[ReferralRewards.qty])
            }
            ReferralRewards.update({ (ReferralRewards.userId eq userId) and (ReferralRewards.claimed eq false) }) {
                it[claimed] = true
            }
            rows
        }
        if (rewards.isEmpty()) return Pair(emptyList(), null)
        val lureItems = mutableListOf<Pair<Long, Int>>()
        val packages = mutableListOf<Pair<String, Int>>()
        for (r in rewards) {
            if (r.lureId != null) lureItems.add(r.lureId to r.qty)
            else if (r.packageId != null) packages.add(r.packageId to r.qty)
        }
        var res: Pair<List<FishingService.LureDTO>, Long?>? = null
        if (lureItems.isNotEmpty()) {
            res = fishing.addLures(userId, lureItems)
        }
        for ((pkg, qty) in packages) {
            repeat(qty) { res = fishing.buyPackage(userId, pkg) }
        }
        return res ?: Pair(emptyList(), null)
    }
}

