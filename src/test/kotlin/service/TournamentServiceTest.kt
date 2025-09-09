package service

import app.Env
import db.DB
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant

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
}
