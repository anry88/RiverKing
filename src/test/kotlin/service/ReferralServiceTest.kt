package service

import app.Env
import db.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ReferralServiceTest {
    @Test
    fun rewardsCreatedOnPurchase() {
        val env = Env(
            botToken = "",
            telegramWebhookSecret = "",
            publicBaseUrl = "http://localhost",
            dbUrl = "jdbc:sqlite:file:refdb?mode=memory&cache=shared",
            dbUser = "",
            dbPass = "",
            port = 0,
            devMode = true,
            adminTgId = 0L,
            providerToken = "",
            botName = "",
        )
        DB.init(env)
        val fishing = FishingService()

        val inviterId = fishing.ensureUserByTgId(1)
        val token = ReferralService.generateLink(inviterId)
        val newUserId = fishing.ensureUserByTgId(2, refToken = token)

        val pack = fishing.findPack("fresh_topup_s")!!
        ReferralService.onPurchase(newUserId, pack)

        val rewards = transaction {
            ReferralRewards.select { ReferralRewards.userId eq inviterId }.toList()
        }
        assertEquals(2, rewards.size)
        assertTrue(rewards.all { it[ReferralRewards.qty] == 2 })
    }
}
