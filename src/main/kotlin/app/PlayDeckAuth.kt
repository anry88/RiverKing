package app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PlayDeckAuth {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class InitDataCheckResponse(
        val valid: Boolean = false,
        val active: Boolean = false,
        val userUid: Long? = null,
    )

    suspend fun verifyAndExtractUser(initData: String): TgWebAppAuth.TgUser = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(initData, StandardCharsets.UTF_8)
        val url = URL("https://api.playdeck.io/api/v1/initdata/check?initData=$encoded")
        val responseText = (url.openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 5000
            val status = responseCode
            val stream = if (status in 200..299) inputStream else errorStream
            val text = stream?.buffered()?.use { it.readBytes().decodeToString() }.orEmpty()
            disconnect()
            if (status !in 200..299) error("playdeck initData check failed")
            text
        }
        val response = json.decodeFromString<InitDataCheckResponse>(responseText)
        require(response.valid && response.active) { "playdeck initData inactive or invalid" }
        val parsedUser = runCatching { TgWebAppAuth.extractUser(initData) }.getOrNull()
        when {
            parsedUser != null -> parsedUser
            response.userUid != null -> TgWebAppAuth.TgUser(id = response.userUid)
            else -> error("playdeck user missing")
        }
    }
}
