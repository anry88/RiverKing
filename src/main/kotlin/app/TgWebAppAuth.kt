package app

import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
        val secretKey = MessageDigest.getInstance("SHA-256").digest(botToken.toByteArray(StandardCharsets.UTF_8))
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secretKey, "HmacSHA256")) }
        val calcHex = mac.doFinal(dataCheckString.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
        require(calcHex.equals(hash, ignoreCase = true)) { "bad hash" }
        val userJson = params["user"] ?: error("no user")
        return Json.parseToJsonElement(userJson).jsonObject["id"]?.jsonPrimitive?.long
            ?: error("user.id missing")
    }
}
