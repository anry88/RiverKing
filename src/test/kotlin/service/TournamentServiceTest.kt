package service

import app.Env
import db.*
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class TournamentServiceTest {
    @Test
    fun createAndList() {
        val env = Env(
            botToken = "",
            telegramWebhookSecret = "",
            publicBaseUrl = "http://localhost",
            dbUrl = "jdbc:sqlite:file:testdb?mode=memory&cache=shared",
            dbUser = "",
            dbPass = "",
            port = 0,
            devMode = true,
            adminTgId = 0L,
            providerToken = "",
        )
        DB.init(env)
        val svc = TournamentService()
        val now = Instant.now()
        svc.createTournament(
            nameRu = "Тест",
            nameEn = "Test",
            start = now,
            end = now.plusSeconds(3600),
            fish = "Щука",
            location = "Пруд",
            metric = "largest",
            prizePlaces = 3,
            prizes = "[{\"pack\":\"fresh_topup_s\",\"qty\":1},{\"pack\":\"fresh_topup_s\",\"qty\":1},{\"pack\":\"fresh_topup_s\",\"qty\":1}]",
        )
        val list = svc.listTournaments()
        assertEquals(1, list.size)
        assertEquals("Тест", list[0].nameRu)
    }

    @Test
    fun updateAndDelete() {
        val env = Env(
            botToken = "",
            telegramWebhookSecret = "",
            publicBaseUrl = "http://localhost",
            dbUrl = "jdbc:sqlite:file:testdb2?mode=memory&cache=shared",
            dbUser = "",
            dbPass = "",
            port = 0,
            devMode = true,
            adminTgId = 0L,
            providerToken = "",
        )
        DB.init(env)
        val svc = TournamentService()
        val now = Instant.now()
        val id = svc.createTournament(
            nameRu = "Ориг",
            nameEn = "Orig",
            start = now,
            end = now.plusSeconds(100),
            fish = null,
            location = null,
            metric = "count",
            prizePlaces = 1,
            prizes = "[{\"pack\":\"fresh_topup_s\",\"qty\":1}]",
        )
        var t = svc.getTournament(id)
        assertEquals("Ориг", t?.nameRu)
        svc.updateTournament(
            id,
            nameRu = "Нью",
            nameEn = "New",
            start = now,
            end = now.plusSeconds(200),
            fish = "Щука",
            location = "Река",
            metric = "largest",
            prizePlaces = 2,
            prizes = "[{\"pack\":\"fresh_topup_s\",\"qty\":1},{\"pack\":\"fresh_topup_s\",\"qty\":1}]",
        )
        t = svc.getTournament(id)
        assertEquals("Нью", t?.nameRu)
        svc.deleteTournament(id)
        assertEquals(0, svc.listTournaments().size)
    }

    @Test
    fun pastTournaments() {
        val env = Env(
            botToken = "",
            telegramWebhookSecret = "",
            publicBaseUrl = "http://localhost",
            dbUrl = "jdbc:sqlite:file:testdb3?mode=memory&cache=shared",
            dbUser = "",
            dbPass = "",
            port = 0,
            devMode = true,
            adminTgId = 0L,
            providerToken = "",
        )
        DB.init(env)
        val svc = TournamentService()
        val now = Instant.now()
        svc.createTournament(
            nameRu = "Past",
            nameEn = "Past",
            start = now.minusSeconds(7200),
            end = now.minusSeconds(3600),
            fish = null,
            location = null,
            metric = "largest",
            prizePlaces = 1,
            prizes = "[]",
        )
        svc.createTournament(
            nameRu = "Future",
            nameEn = "Future",
            start = now.plusSeconds(3600),
            end = now.plusSeconds(7200),
            fish = null,
            location = null,
            metric = "largest",
            prizePlaces = 1,
            prizes = "[]",
        )
        val past = svc.pastTournaments()
        assertEquals(1, past.size)
        assertEquals("Past", past[0].nameEn)
    }

    @Test
    fun leaderboardCountShowsFish() {
        val env = Env(
            botToken = "",
            telegramWebhookSecret = "",
            publicBaseUrl = "http://localhost",
            dbUrl = "jdbc:sqlite:file:testdb4?mode=memory&cache=shared",
            dbUser = "",
            dbPass = "",
            port = 0,
            devMode = true,
            adminTgId = 0L,
            providerToken = "",
        )
        DB.init(env)
        val svc = TournamentService()
        val now = Instant.now()
        val tid = svc.createTournament(
            nameRu = "Турнир",
            nameEn = "Tournament",
            start = now.minusSeconds(60),
            end = now.plusSeconds(60),
            fish = null,
            location = null,
            metric = "count",
            prizePlaces = 1,
            prizes = "[]",
        )
        val t = svc.getTournament(tid)!!
        val uid = transaction {
            Users.insertAndGetId {
                it[tgId] = 1L
                it[level] = 1
                it[xp] = 0
                it[createdAt] = now
            }.value
        }
        val locId = transaction { Locations.select { Locations.name eq "Пруд" }.single()[Locations.id].value }
        val fishId = transaction { Fish.select { Fish.name eq "Плотва" }.single()[Fish.id].value }
        transaction {
            Catches.insert {
                it[Catches.userId] = uid
                it[Catches.fishId] = fishId
                it[Catches.weight] = 1.0
                it[Catches.locationId] = locId
                it[Catches.createdAt] = now
            }
        }
        val (top, mine) = svc.leaderboard(t, uid)
        assertEquals(1, top.size)
        assertEquals("Плотва", top[0].fish)
        assertEquals("Плотва", mine?.fish)
    }

    @Test
    fun leaderboardCountAggregatesCatches() {
        val env = Env(
            botToken = "",
            telegramWebhookSecret = "",
            publicBaseUrl = "http://localhost",
            dbUrl = "jdbc:sqlite:file:testdb5?mode=memory&cache=shared",
            dbUser = "",
            dbPass = "",
            port = 0,
            devMode = true,
            adminTgId = 0L,
            providerToken = "",
        )
        DB.init(env)
        val svc = TournamentService()
        val now = Instant.now()
        val tid = svc.createTournament(
            nameRu = "Счёт",
            nameEn = "Count",
            start = now.minusSeconds(60),
            end = now.plusSeconds(60),
            fish = null,
            location = null,
            metric = "count",
            prizePlaces = 1,
            prizes = "[]",
        )
        val t = svc.getTournament(tid)!!
        val uid = transaction {
            Users.insertAndGetId {
                it[tgId] = 1L
                it[level] = 1
                it[xp] = 0
                it[createdAt] = now
            }.value
        }
        val locId = transaction { Locations.select { Locations.name eq "Пруд" }.single()[Locations.id].value }
        val fishId = transaction { Fish.select { Fish.name eq "Плотва" }.single()[Fish.id].value }
        repeat(3) { idx ->
            transaction {
                Catches.insert {
                    it[Catches.userId] = uid
                    it[Catches.fishId] = fishId
                    it[Catches.weight] = 1.0 + idx
                    it[Catches.locationId] = locId
                    it[Catches.createdAt] = now.plusSeconds(idx.toLong())
                }
            }
        }
        val (top, mine) = svc.leaderboard(t, uid)
        assertEquals(1, top.size)
        assertEquals(3.0, top[0].value)
        assertEquals(3.0, mine?.value)
    }
}
