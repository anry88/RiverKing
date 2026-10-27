package stub

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.io.Closeable

class ApiError(val status: Int, val code: String) : RuntimeException("Game server returned $status ($code)")
interface GameApi {
    suspend fun get(path: String): JsonElement
    suspend fun post(path: String, body: JsonObject? = null): JsonElement
}

/** Pure HTTP client: no backend classes, JDBC credentials, SQL or local fishing logic. */
class GameServer(private val baseUrl: String, private val sessions: SessionStore,
    private val client: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 90_000; connectTimeoutMillis = 10_000; socketTimeoutMillis = 90_000 }
        followRedirects = false
    },
) : Closeable {
    private var versionCode = 0L
    suspend fun initialize() {
        val response = client.get("${baseUrl.trimEnd('/')}/api/mobile/update")
        check(response.status.isSuccess()) { "Cannot load server compatibility policy" }
        versionCode = Json.parseToJsonElement(response.bodyAsText()).jsonObject.long("latestVersionCode")
    }
    fun forAccount(userId: Long): GameApi = object : GameApi {
        override suspend fun get(path: String) = request(userId, HttpMethod.Get, path)
        override suspend fun post(path: String, body: JsonObject?) = request(userId, HttpMethod.Post, path, body)
    }
    private suspend fun request(userId: Long, method: HttpMethod, path: String, body: JsonObject? = null,
        refreshAllowed: Boolean = true, policyRetry: Boolean = true): JsonElement {
        require(path.startsWith("/api/") && !path.startsWith("/api/admin/"))
        val response = client.request("${baseUrl.trimEnd('/')}$path") {
            this.method = method
            bearerAuth(sessions.get(userId).accessToken)
            header("X-RiverKing-App-Version-Code", versionCode)
            header(HttpHeaders.UserAgent, "RiverKing-Gameplay-Bot")
            if (body != null) { contentType(ContentType.Application.Json); setBody(body.toString()) }
        }
        if (response.status == HttpStatusCode.Unauthorized && refreshAllowed) {
            val renewed = client.post("${baseUrl.trimEnd('/')}/api/auth/refresh") {
                header("X-RiverKing-App-Version-Code", versionCode)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("refreshToken", sessions.get(userId).refreshToken) }.toString())
            }
            if (!renewed.status.isSuccess()) throw ApiError(renewed.status.value, "session_refresh_failed")
            sessions.update(userId, Json.parseToJsonElement(renewed.bodyAsText()).jsonObject)
            return request(userId, method, path, body, refreshAllowed = false, policyRetry = policyRetry)
        }
        if (response.status == HttpStatusCode.UpgradeRequired && policyRetry) {
            initialize()
            return request(userId, method, path, body, refreshAllowed, policyRetry = false)
        }
        val text = response.bodyAsText()
        val json = if (text.isBlank()) JsonNull else runCatching { Json.parseToJsonElement(text) }.getOrElse {
            throw ApiError(response.status.value, "invalid_server_response")
        }
        if (!response.status.isSuccess()) {
            throw ApiError(response.status.value, (json as? JsonObject)?.get("error")?.jsonPrimitive?.contentOrNull ?: "request_failed")
        }
        return json
    }
    override fun close() = client.close()
}
