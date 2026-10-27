package stub

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.nio.file.Files
import kotlin.test.*

class GameServerTest {
    @Test
    fun usesHttpAndRefreshesOrdinarySessionsWithoutAdminCredentials() = runBlocking {
        val file = Files.createTempFile("bot-sessions-", ".json")
        Files.writeString(file, JsonArray((1..50).map { i -> buildJsonObject {
            put("slot", "v1-$i"); put("userId", i); put("nickname", "test-$i")
            put("language", if (i <= 25) "ru" else "en"); put("accessToken", "old-$i"); put("refreshToken", "refresh-$i")
        } }).toString())
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            val headers = headersOf(HttpHeaders.ContentType, "application/json")
            when (request.url.encodedPath) {
                "/api/mobile/update" -> respond("""{"latestVersionCode":14}""", headers = headers)
                "/api/auth/refresh" -> {
                    assertNull(request.headers[HttpHeaders.Authorization])
                    respond("""{"accessToken":"new","refreshToken":"renewed"}""", headers = headers)
                }
                "/api/bot/cast" -> {
                    assertEquals("14", request.headers["X-RiverKing-App-Version-Code"])
                    if (request.headers[HttpHeaders.Authorization] == "Bearer new") respond("""{"caught":true}""", headers = headers)
                    else respond("{}", HttpStatusCode.Unauthorized, headers)
                }
                else -> error("Unexpected request")
            }
        }
        GameServer("http://game", SessionStore(file), HttpClient(engine)).use { server ->
            server.initialize()
            assertTrue(server.forAccount(1).post("/api/bot/cast").jsonObject.bool("caught"))
        }
        assertEquals(listOf("/api/mobile/update", "/api/bot/cast", "/api/auth/refresh", "/api/bot/cast"), paths)
        assertEquals("new", SessionStore(file).get(1).accessToken)
        assertEquals("old-2", SessionStore(file).get(2).accessToken)
        Files.delete(file)
    }
}
