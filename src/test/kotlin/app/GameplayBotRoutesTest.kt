package app

import db.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import service.*
import support.testEnv
import util.Metrics
import kotlin.test.*

class GameplayBotRoutesTest {
    @Test
    fun onlyProvisionedBotSessionsCanPerformServerSideCast() = testApplication {
        val env = testEnv("bot-http-routes").copy(devMode = false)
        val fishing = FishingService()
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(DoubleReceive)
            installSessions(env)
            DB.init(env)
            apiRoutes(env, botCasts = BotCastService(fishing, waitSeconds = { 5 }, awaitBite = {}))
            adminApiRoutes(env)
        }
        suspend fun provision(token: String) = client.post("/api/admin/gameplay-bots/sessions") {
            header("X-RiverKing-App-Version-Code", "14")
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.Unauthorized, provision("not-admin").status)
        val response = provision(env.adminApiToken)
        assertEquals(HttpStatusCode.OK, response.status)
        val accounts = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(50, accounts.size)
        val ids = accounts.map { it.jsonObject["userId"]!!.jsonPrimitive.long }
        assertEquals(ids, Json.parseToJsonElement(provision(env.adminApiToken).bodyAsText()).jsonArray
            .map { it.jsonObject["userId"]!!.jsonPrimitive.long })
        transaction {
            assertEquals(50L, Users.selectAll().count())
            assertEquals(25L, Users.select { Users.language eq "ru" }.count())
            assertEquals(25L, Users.select { Users.language eq "en" }.count())
            assertTrue(Users.selectAll().all { it[Users.registrationSource] == "bot" })
        }
        val fakeBot = fishing.createUser(registrationSource = "bot")
        val fakeToken = AuthService(env, fishing).issueTokens(fakeBot).accessToken
        suspend fun cast(token: String? = null) = client.post("/api/bot/cast") {
            header("X-RiverKing-App-Version-Code", "14")
            header("X-RiverKing-App-Platform", "bot") // Spoofed client headers grant no privilege.
            header(HttpHeaders.UserAgent, "RiverKing-Gameplay-Bot")
            if (token != null) bearerAuth(token)
        }
        assertEquals(HttpStatusCode.Unauthorized, cast().status)
        assertEquals(HttpStatusCode.Unauthorized, cast(env.adminApiToken).status)
        assertEquals(HttpStatusCode.Forbidden, cast(fakeToken).status)
        val token = accounts.first().jsonObject["accessToken"]!!.jsonPrimitive.content
        val caught = cast(token)
        assertEquals(HttpStatusCode.OK, caught.status)
        assertTrue(Json.parseToJsonElement(caught.bodyAsText()).jsonObject["caught"]!!.jsonPrimitive.boolean)
        transaction {
            assertEquals(1L, Catches.select { Catches.userId eq ids.first() }.count())
            Users.update({ Users.id eq ids.first() }) { it[registrationSource] = "direct" }
        }
        assertEquals(HttpStatusCode.Forbidden, cast(token).status)
        // Exercise the actual standalone HTTP client against the real server routes and DTOs.
        val file = java.nio.file.Files.createTempFile("riverking-bot-sessions-", ".json")
        try {
            java.nio.file.Files.writeString(file, accounts.toString())
            val store = stub.SessionStore(file)
            stub.GameServer("http://localhost", store, client).use { game ->
                game.initialize()
                stub.BotNicknames.synchronize(game, store)
                assertEquals(25, stub.BotNicknames.russian.size)
                assertEquals(25, stub.BotNicknames.english.size)
                assertEquals(50, store.accounts().map { it.nickname }.distinct().size)
                for (account in store.accounts()) {
                    assertEquals(stub.BotNicknames.forSlot(account.slot), fishing.displayName(account.userId))
                }
                val nicknameMetric = Metrics.dump().lineSequence().first {
                    it.startsWith("api_call_total") && it.contains("path=\"/api/nickname\"")
                }
                stub.BotNicknames.synchronize(game, store)
                assertTrue(Metrics.dump().lineSequence().any { it == nicknameMetric }, "Unchanged names must not be written again")
                assertEquals(stub.GameCycle.Result.CAUGHT, stub.GameCycle(game.forAccount(ids[1])).run())
                // Unlock the test profile, then cross the actual fresh/salt boundary through HTTP.
                val uid = ids[1]
                transaction { Users.update({ Users.id eq uid }) { it[totalCaughtKg] = 1_000_000.0 } }
                val regular = fishing.locations(uid).filterNot { it.isEvent }
                val saltLocations = transaction {
                    (LocationFishWeights innerJoin Fish).selectAll()
                        .groupBy { it[LocationFishWeights.locationId].value }
                        .filterValues { fish -> fish.all { it[Fish.water] == "salt" } }.keys
                }
                val saltIndex = regular.indexOfFirst { it.id in saltLocations }
                assertTrue(saltIndex > 0)
                val saltLocation = regular[saltIndex].id
                val saltLure = transaction {
                    val fish = (LocationFishWeights innerJoin Fish).select {
                        (LocationFishWeights.locationId eq saltLocation) and (Fish.water eq "salt")
                    }.first()
                    Lures.select { (Lures.water eq "salt") and (Lures.predator eq fish[Fish.predator]) }.first()[Lures.id].value
                }
                fishing.addLures(uid, listOf(saltLure to 3))
                fishing.setLocation(uid, regular[saltIndex - 1].id)
                fishing.setLure(uid, fishing.listLures(uid).first { it.water == "fresh" && it.qty > 0 }.id)
                assertEquals(stub.GameCycle.Result.CAUGHT, stub.GameCycle(game.forAccount(uid)).run())
                fun currentWater() = transaction {
                    val lure = Users.select { Users.id eq uid }.single()[Users.currentLureId]!!.value
                    Lures.select { Lures.id eq lure }.single()[Lures.water]
                }
                assertEquals("salt", currentWater())
                fishing.setLocation(uid, regular.last().id)
                assertEquals(stub.GameCycle.Result.CAUGHT, stub.GameCycle(game.forAccount(uid)).run())
                assertEquals("fresh", currentWater())
            }
            transaction { assertEquals(3L, Catches.select { Catches.userId eq ids[1] }.count()) }
        } finally {
            java.nio.file.Files.deleteIfExists(file)
        }
        assertTrue(Metrics.dump().contains("bot_cast_total{caught=\"true\"}"))
        assertTrue(Metrics.dump().contains("path=\"/api/bot/cast\""))
    }
}
