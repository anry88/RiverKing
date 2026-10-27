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
            stub.GameServer("http://localhost", stub.SessionStore(file), client).use { game ->
                game.initialize()
                assertEquals(stub.GameCycle.Result.CAUGHT, stub.GameCycle(game.forAccount(ids[1])).run())
            }
            transaction { assertEquals(1L, Catches.select { Catches.userId eq ids[1] }.count()) }
        } finally {
            java.nio.file.Files.deleteIfExists(file)
        }
        assertTrue(Metrics.dump().contains("bot_cast_total{caught=\"true\"}"))
        assertTrue(Metrics.dump().contains("path=\"/api/bot/cast\""))
    }
}
