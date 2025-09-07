package app

import kotlinx.serialization.json.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TgWebAppAuth {
    fun verifyAndExtractTgId(initData: String, botToken: String): Long {
        // Telegram sends initData as a query-string-style payload where values are URL-encoded.
        // For signature verification we must decode each key/value before building the
        // data check string. Using encoded values leads to "bad hash" errors even for
        // valid initData.
        val params = initData.split("&").associate {
            val i = it.indexOf('=')
            val key = URLDecoder.decode(it.substring(0, i), StandardCharsets.UTF_8)
            val value = URLDecoder.decode(it.substring(i + 1), StandardCharsets.UTF_8)
            key to value
        }
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
        val userJson = params["user"] ?: error("no user")
        return Json.parseToJsonElement(userJson).jsonObject["id"]?.jsonPrimitive?.long
            ?: error("user.id missing")
    }
}
