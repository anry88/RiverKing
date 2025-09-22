package service

import app.Env
import db.*
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.ZoneOffset

class FishingServiceTest {
    private fun testEnv(name: String) = Env(
        botToken = "",
        telegramWebhookSecret = "",
        publicBaseUrl = "http://localhost",
        dbUrl = "jdbc:sqlite:file:$name?mode=memory&cache=shared",
        dbUser = "",
        dbPass = "",
        port = 0,
        devMode = true,
        adminTgId = 0L,
        providerToken = "",
        botName = "",
    )

    private fun newService(dbName: String): FishingService {
        DB.init(testEnv(dbName))
        return FishingService()
    }

    @Test
    fun baseEscapeChanceProgressesByLocation() {
        val svc = newService("testdb_fish_escape")
        transaction {
            val ids = Locations.selectAll().orderBy(Locations.unlockKg).map { it[Locations.id].value }
            ids.forEachIndexed { idx, id ->
                val expected = (0.05 * idx).coerceAtMost(0.5)
                assertEquals(expected, svc.baseEscapeChance(id), 0.0000001)
            }
        }
    }

    @Test
    fun restoreCastingLuresReturnsLastUsedBait() {
        val svc = newService("testdb_restore_cast")
        val userId = svc.ensureUserByTgId(1L)
        val lureId = transaction {
            Lures.select { Lures.name eq "Пресная мирная" }.single()[Lures.id].value
        }
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.isCasting] = true
                it[Users.castLureId] = lureId
            }
            InventoryLures.update({ (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId) }) {
                it[InventoryLures.qty] = 9
            }
        }

        val restored = svc.restoreCastingLuresOnStartup()
        assertEquals(1, restored)

        transaction {
            val qty = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq lureId)
            }.single()[InventoryLures.qty]
            assertEquals(10, qty)
            val userRow = Users.select { Users.id eq userId }.single()
            assertEquals(false, userRow[Users.isCasting])
            assertEquals(null, userRow[Users.castLureId])
        }
    }

    @Test
    fun restoreCastingLuresFallsBackToDefaultWhenUnknown() {
        val svc = newService("testdb_restore_fallback")
        val userId = svc.ensureUserByTgId(2L)
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.isCasting] = true
                it[Users.castLureId] = null
            }
        }

        val restored = svc.restoreCastingLuresOnStartup()
        assertEquals(1, restored)

        transaction {
            val fallbackId = Lures.select { Lures.name eq "Пресная хищная+" }.single()[Lures.id].value
            val qty = InventoryLures.select {
                (InventoryLures.userId eq userId) and (InventoryLures.lureId eq fallbackId)
            }.single()[InventoryLures.qty]
            assertEquals(1, qty)
            val userRow = Users.select { Users.id eq userId }.single()
            assertEquals(false, userRow[Users.isCasting])
            assertEquals(null, userRow[Users.castLureId])
        }
    }

    @Test
    fun shopDiscountsAppliedAndRemoved() {
        val svc = newService("testdb_shop_discount")
        val now = LocalDate.now(ZoneOffset.UTC)
        val packId = "fresh_topup_s"

        svc.setDiscount(packId, price = 10, start = now, end = now.plusDays(2))

        val discounted = svc.listShop("ru").flatMap { it.packs }.first { it.id == packId }
        assertEquals(10, discounted.price)
        assertEquals(39, discounted.originalPrice)
        assertEquals(now.plusDays(2), discounted.discountEnd)

        val discounts = svc.listDiscounts().associateBy { it.packageId }
        val stored = discounts[packId]
        requireNotNull(stored)
        assertEquals(10, stored.price)
        assertEquals(now, stored.startDate)

        svc.removeDiscount(packId)

        val restored = svc.listShop("ru").flatMap { it.packs }.first { it.id == packId }
        assertEquals(39, restored.price)
        assertEquals(null, restored.originalPrice)
    }
}
