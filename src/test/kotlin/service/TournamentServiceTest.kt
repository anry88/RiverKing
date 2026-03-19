package service

import db.*
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import support.testEnv

class TournamentServiceTest {
    @Test
    fun createAndList() {
        val env = testEnv("testdb")
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
        val env = testEnv("testdb2")
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
        val env = testEnv("testdb3")
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
        val env = testEnv("testdb4")
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
        assertEquals(null, top[0].fish)
        assertEquals(null, mine?.fish)
    }

    @Test
    fun leaderboardCountAggregatesCatches() {
        val env = testEnv("testdb5")
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

    @Test
    fun leaderboardShowsPrizePlacesPlusMine() {
        val env = testEnv("testdb6")
        DB.init(env)
        val svc = TournamentService()
        val now = Instant.now()
        val tid = svc.createTournament(
            nameRu = "Рейтинг",
            nameEn = "Ranks",
            start = now.minusSeconds(60),
            end = now.plusSeconds(60),
            fish = null,
            location = null,
            metric = "largest",
            prizePlaces = 3,
            prizes = "[]",
        )
        val t = svc.getTournament(tid)!!
        val locId = transaction { Locations.select { Locations.name eq "Пруд" }.single()[Locations.id].value }
        val fishId = transaction { Fish.select { Fish.name eq "Плотва" }.single()[Fish.id].value }
        val uids = (1L..4L).map { tg ->
            transaction {
                Users.insertAndGetId {
                    it[Users.tgId] = tg
                    it[level] = 1
                    it[xp] = 0
                    it[createdAt] = now
                }.value
            }
        }
        val weights = listOf(10.0, 9.0, 8.0, 7.0)
        uids.forEachIndexed { idx, uid ->
            transaction {
                Catches.insert {
                    it[Catches.userId] = uid
                    it[Catches.fishId] = fishId
                    it[Catches.weight] = weights[idx]
                    it[Catches.locationId] = locId
                    it[Catches.createdAt] = now
                }
            }
        }
        val (top4, mine4) = svc.leaderboard(t, uids[3], t.prizePlaces)
        assertEquals(3, top4.size)
        assertEquals(4, mine4?.rank)
        assertEquals(false, top4.any { it.userId == uids[3] })

        val (top2, mine2) = svc.leaderboard(t, uids[1], t.prizePlaces)
        assertEquals(3, top2.size)
        assertEquals(2, mine2?.rank)
        assertEquals(true, top2.any { it.userId == uids[1] })
    }
}
