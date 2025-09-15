package service

import app.Env
import db.*
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class FishingServiceTest {
    @Test
    fun baseEscapeChanceProgressesByLocation() {
        val env = Env(
            botToken = "",
            telegramWebhookSecret = "",
            publicBaseUrl = "http://localhost",
            dbUrl = "jdbc:sqlite:file:testdb_fish?mode=memory&cache=shared",
            dbUser = "",
            dbPass = "",
            port = 0,
            devMode = true,
            adminTgId = 0L,
            providerToken = "",
            botName = "",
        )
        DB.init(env)
        val svc = FishingService()
        transaction {
            val ids = Locations.selectAll().orderBy(Locations.unlockKg).map { it[Locations.id].value }
            ids.forEachIndexed { idx, id ->
                val expected = (0.05 * (idx + 1)).coerceAtMost(0.5)
                assertEquals(expected, svc.baseEscapeChance(id), 0.0000001)
            }
        }
    }
}
