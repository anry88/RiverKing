package service

import db.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import support.testEnv

class ReferralServiceTest {
    @Test
    fun rewardsCreatedOnPurchase() {
        val env = testEnv("refdb")
        DB.init(env)
        val fishing = FishingService()

        val inviterId = fishing.ensureUserByTgId(1)
        val token = ReferralService.generateLink(inviterId)
        val newUserId = fishing.ensureUserByTgId(2, refToken = token)

        transaction {
            assertEquals(1, ReferralRewards.select { (ReferralRewards.userId eq inviterId) and (ReferralRewards.packageId eq "bundle_starter") }.count())
            assertEquals(1, ReferralRewards.select { (ReferralRewards.userId eq newUserId) and (ReferralRewards.packageId eq "bundle_starter") }.count())
        }

        val pack = fishing.findPack("fresh_topup_s")!!
        ReferralService.onPurchase(newUserId, pack)

        val rewards = transaction {
            ReferralRewards.select {
                (ReferralRewards.userId eq inviterId) and ReferralRewards.lureId.isNotNull()
            }.toList()
        }
        assertEquals(2, rewards.size)
        assertTrue(rewards.all { it[ReferralRewards.qty] == 3 })
    }

    @Test
    fun rewardsRoundedUpMinimumOne() {
        val env = testEnv("refdb_round")
        DB.init(env)
        val fishing = FishingService()

        val inviterId = fishing.ensureUserByTgId(1)
        val token = ReferralService.generateLink(inviterId)
        val newUserId = fishing.ensureUserByTgId(2, refToken = token)

        val smallPack = FishingService.ShopPackage(
            id = "test_pack",
            name = "",
            desc = "",
            price = 0,
            items = listOf("Пресная мирная" to 2)
        )
        ReferralService.onPurchase(newUserId, smallPack)

        val reward = transaction {
            ReferralRewards.select {
                (ReferralRewards.userId eq inviterId) and (ReferralRewards.packageId.isNull())
            }.single()
        }
        assertEquals(1, reward[ReferralRewards.qty])
    }
}
