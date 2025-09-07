package app

import kotlinx.serialization.json.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TgWebAppAuth {
    fun verifyAndExtractTgId(initData: String, botToken: String): Long {
        val params = initData.split("&").map {
            val i = it.indexOf('='); it.substring(0, i) to it.substring(i + 1)
        }.toMap()
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
        val userJson = URLDecoder.decode(params["user"] ?: error("no user"), StandardCharsets.UTF_8)
        return Json.parseToJsonElement(userJson).jsonObject["id"]?.jsonPrimitive?.long
            ?: error("user.id missing")
    }
}
