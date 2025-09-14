package service

import db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

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
        }
    }

    fun onPurchase(buyerId: Long, pack: FishingService.ShopPackage) = transaction {
        val ref = Users.select { Users.id eq buyerId }.single()[Users.referredBy]?.value ?: return@transaction
        for ((name, qty) in pack.items) {
            val rewardQty = qty / 4
            if (rewardQty > 0) {
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

    fun pendingRewards(userId: Long): List<Reward> = transaction {
        ReferralRewards.select { (ReferralRewards.userId eq userId) and (ReferralRewards.claimed eq false) }
            .map {
                val pkg = it[ReferralRewards.packageId] ?: Lures.select { Lures.id eq it[ReferralRewards.lureId]!!.value }
                    .single()[Lures.name]
                Reward(it[ReferralRewards.id].value, pkg, it[ReferralRewards.qty])
            }
    }

    fun claimReward(userId: Long, rewardId: Long, fishing: FishingService): Pair<List<FishingService.LureDTO>, Long?> {
        val reward = transaction {
            val row = ReferralRewards.select {
                (ReferralRewards.id eq rewardId) and (ReferralRewards.userId eq userId) and (ReferralRewards.claimed eq false)
            }.singleOrNull() ?: error("not found")
            ReferralRewards.update({ ReferralRewards.id eq rewardId }) { it[claimed] = true }
            Triple(row[ReferralRewards.lureId]?.value, row[ReferralRewards.packageId], row[ReferralRewards.qty])
        }
        return if (reward.first != null) {
            fishing.addLures(userId, listOf(reward.first!! to reward.third))
        } else if (reward.second != null) {
            var res: Pair<List<FishingService.LureDTO>, Long?>? = null
            repeat(reward.third) { res = fishing.buyPackage(userId, reward.second!!) }
            res!!
        } else error("bad reward")
    }
}

