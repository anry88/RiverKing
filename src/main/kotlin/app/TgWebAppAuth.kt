package app

import kotlinx.serialization.json.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TgWebAppAuth {
    @kotlinx.serialization.Serializable
    data class TgUser(
        val id: Long,
        val firstName: String? = null,
        val lastName: String? = null,
        val username: String? = null,
        val languageCode: String? = null,
    )

    fun verifyAndExtractUser(initData: String, botToken: String): TgUser {
        // Telegram sends initData as a query-string-style payload where values are URL-encoded.
        // For signature verification we must decode each key/value before building the
        // data check string. Using encoded values leads to "bad hash" errors even for
        // valid initData.
        val params = parseParams(initData)
        val hash = params["hash"] ?: error("no hash")
        val dataCheckString = params.filterKeys { it != "hash" }.toList().sortedBy { it.first }
            .joinToString("\n") { (k, v) -> "$k=$v" }
        val secretKey = Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec("WebAppData".toByteArray(StandardCharsets.UTF_8), algorithm))
            doFinal(botToken.toByteArray(StandardCharsets.UTF_8))
        }
        val calcHex = Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(secretKey, algorithm))
            doFinal(dataCheckString.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
        require(calcHex.equals(hash, ignoreCase = true)) { "bad hash" }
        return extractUser(params)
    }

    fun extractUser(initData: String): TgUser = extractUser(parseParams(initData))

    fun extractStartParam(initData: String): String? =
        runCatching { parseParams(initData)["start_param"]?.takeIf { it.isNotBlank() } }.getOrNull()

    private fun parseParams(initData: String): Map<String, String> =
        initData.split("&")
            .filter { it.isNotBlank() }
            .associate {
                val i = it.indexOf('=')
                require(i > 0) { "bad initData pair" }
                val key = URLDecoder.decode(it.substring(0, i), StandardCharsets.UTF_8)
                val value = URLDecoder.decode(it.substring(i + 1), StandardCharsets.UTF_8)
                key to value
            }

    private fun extractUser(params: Map<String, String>): TgUser {
        val userJson = params["user"] ?: error("no user")
        val obj = Json.parseToJsonElement(userJson).jsonObject
        return TgUser(
            obj["id"]?.jsonPrimitive?.long ?: error("user.id missing"),
            obj["first_name"]?.jsonPrimitive?.contentOrNull,
            obj["last_name"]?.jsonPrimitive?.contentOrNull,
            obj["username"]?.jsonPrimitive?.contentOrNull,
            obj["language_code"]?.jsonPrimitive?.contentOrNull,
        )
    }
}
