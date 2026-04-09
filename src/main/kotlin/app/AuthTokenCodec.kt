package app

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AuthTokenCodec(
    secret: String,
    private val accessTokenTtl: Duration,
    private val refreshTokenTtl: Duration,
    private val clock: Clock = Clock.systemUTC(),
) {
    data class AccessToken(val value: String, val expiresAt: Instant)
    data class RefreshSession(val sessionId: String, val token: String, val refreshTokenHash: String, val expiresAt: Instant)

    private val secretBytes = secret.toByteArray(StandardCharsets.UTF_8)
    private val random = SecureRandom()
    private val base64 = Base64.getUrlEncoder().withoutPadding()
    private val base64Decoder = Base64.getUrlDecoder()

    fun createAccessToken(userId: Long): AccessToken {
        val expiresAt = clock.instant().plus(accessTokenTtl)
        val payload = "$userId:${expiresAt.epochSecond}"
        val encodedPayload = base64.encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        val signature = sign(encodedPayload)
        return AccessToken("$encodedPayload.$signature", expiresAt)
    }

    fun verifyAccessToken(token: String): Long? {
        val parts = token.split('.')
        if (parts.size != 2) return null
        val payload = parts[0]
        val signature = parts[1]
        if (!constantTimeEquals(sign(payload), signature)) return null
        val decoded = runCatching {
            String(base64Decoder.decode(payload), StandardCharsets.UTF_8)
        }.getOrNull() ?: return null
        val rawParts = decoded.split(':')
        if (rawParts.size != 2) return null
        val userId = rawParts[0].toLongOrNull() ?: return null
        val expiresAt = rawParts[1].toLongOrNull()?.let(Instant::ofEpochSecond) ?: return null
        return if (expiresAt.isAfter(clock.instant())) userId else null
    }

    fun createRefreshSession(): RefreshSession {
        val sessionId = randomToken(24)
        val secret = randomToken(32)
        val token = "$sessionId.$secret"
        return RefreshSession(
            sessionId = sessionId,
            token = token,
            refreshTokenHash = sha256(secret),
            expiresAt = clock.instant().plus(refreshTokenTtl),
        )
    }

    fun parseRefreshToken(token: String): Pair<String, String>? {
        val parts = token.split('.')
        if (parts.size != 2) return null
        return parts[0] to parts[1]
    }

    fun hashRefreshSecret(secret: String): String = sha256(secret)

    private fun randomToken(bytes: Int): String {
        val raw = ByteArray(bytes)
        random.nextBytes(raw)
        return base64.encodeToString(raw)
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
        return base64.encodeToString(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(expected: String, actual: String): Boolean =
        MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            actual.toByteArray(StandardCharsets.UTF_8),
        )
}
