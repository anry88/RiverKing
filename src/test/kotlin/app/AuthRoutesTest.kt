package app

import db.DB
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import service.PlayPurchaseVerificationResult
import service.PlayPurchaseVerifier
import service.VerifiedPlayLineItem
import service.VerifiedPlayPurchase
import support.testEnv
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class AuthResponse(
        val accessToken: String,
        val refreshToken: String,
        val user: AuthUser,
    )

    @Serializable
    private data class AuthUser(
        val id: Long,
        val needsNickname: Boolean,
        val language: String,
    )

    @Serializable
    private data class ReferralResponse(
        val token: String,
        val invited: List<String>,
        val link: String,
        val telegramLink: String,
        val androidShareText: String,
        val webFallbackLink: String,
    )

    @Test
    fun `password auth uses shared me endpoint and refresh logout flow`() = testApplication {
        val env = testEnv("password-auth-routes").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            devMode = false,
        )
        application { installAuthTestModule(env) }

        val register = client.post("/api/auth/password/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"angler.one","password":"password123","language":"ru"}""")
        }
        assertEquals(HttpStatusCode.OK, register.status)
        val registered = json.decodeFromString<AuthResponse>(register.bodyAsText())
        assertEquals(true, registered.user.needsNickname)
        assertEquals("ru", registered.user.language)

        val login = client.post("/api/auth/password/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"angler.one","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val loggedIn = json.decodeFromString<AuthResponse>(login.bodyAsText())
        assertEquals(registered.user.id, loggedIn.user.id)

        val meBeforeNickname = client.get("/api/me") {
            bearerAuth(loggedIn.accessToken)
        }
        assertEquals(HttpStatusCode.OK, meBeforeNickname.status)
        val meBeforeBody = json.parseToJsonElement(meBeforeNickname.bodyAsText()).jsonObject
        assertEquals(true, meBeforeBody.getValue("needsNickname").jsonPrimitive.boolean)

        val nickname = client.post("/api/nickname") {
            bearerAuth(loggedIn.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"nickname":"Captain River"}""")
        }
        assertEquals(HttpStatusCode.OK, nickname.status)

        val meAfterNickname = client.get("/api/me") {
            bearerAuth(loggedIn.accessToken)
        }
        assertEquals(HttpStatusCode.OK, meAfterNickname.status)
        val meAfterBody = json.parseToJsonElement(meAfterNickname.bodyAsText()).jsonObject
        assertEquals("Captain River", meAfterBody.getValue("username").jsonPrimitive.content)
        assertEquals(false, meAfterBody.getValue("needsNickname").jsonPrimitive.boolean)

        val logout = client.post("/api/auth/logout") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"refreshToken":"${loggedIn.refreshToken}"}""")
        }
        assertEquals(HttpStatusCode.NoContent, logout.status)

        val refreshAfterLogout = client.post("/api/auth/refresh") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"refreshToken":"${loggedIn.refreshToken}"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, refreshAfterLogout.status)
    }

    @Test
    fun `account deletion removes auth access and public deletion page accepts requests`() = testApplication {
        val env = testEnv("account-delete").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            publicBaseUrl = "https://riverking.example",
            devMode = false,
        )
        application { installAuthTestModule(env) }

        val registered = registerPasswordUser(client, "angler.delete", "password123")

        val delete = client.post("/api/account/delete") {
            bearerAuth(registered.accessToken)
        }
        assertEquals(HttpStatusCode.NoContent, delete.status)

        val meAfterDelete = client.get("/api/me") {
            bearerAuth(registered.accessToken)
        }
        assertEquals(HttpStatusCode.Unauthorized, meAfterDelete.status)

        val loginAfterDelete = client.post("/api/auth/password/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"angler.delete","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, loginAfterDelete.status)

        val refreshAfterDelete = client.post("/api/auth/refresh") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"refreshToken":"${registered.refreshToken}"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, refreshAfterDelete.status)

        val deletionPage = client.get("/account/delete")
        assertEquals(HttpStatusCode.OK, deletionPage.status)
        assertEquals(true, deletionPage.bodyAsText().contains("Delete a RiverKing account"))

        val request = client.post("/account/delete/request") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(
                "login=${"angler.external".encodeURLParameter()}&" +
                    "provider=password&" +
                    "contact=${"angler@example.com".encodeURLParameter()}&" +
                    "note=${"Please remove my test profile".encodeURLParameter()}"
            )
        }
        assertEquals(HttpStatusCode.OK, request.status)
        assertEquals(true, request.bodyAsText().contains("Deletion request submitted"))
    }

    @Test
    fun `telegram auth stores shared user session cookie`() = testApplication {
        val env = testEnv("telegram-auth-routes").copy(
            botToken = "telegram-test-token",
            botName = "river_king_bot",
            devMode = false,
        )
        application { installAuthTestModule(env) }

        val initData = signedTelegramInitData(
            botToken = env.botToken,
            userJson = """
                {"id":777,"first_name":"River","last_name":"King","username":"riverking","language_code":"ru"}
            """.trimIndent(),
        )

        val authResponse = client.post("/api/auth/telegram") {
            header("Telegram-Init-Data", initData)
        }
        assertEquals(HttpStatusCode.OK, authResponse.status)

        val sessionCookie = authResponse.headers[HttpHeaders.SetCookie]
        assertNotNull(sessionCookie)

        val me = client.get("/api/me") {
            header(HttpHeaders.Cookie, sessionCookie.substringBefore(';'))
        }
        assertEquals(HttpStatusCode.OK, me.status)
        val meBody = json.parseToJsonElement(me.bodyAsText()).jsonObject
        assertEquals("ru", meBody.getValue("language").jsonPrimitive.content)
        assertEquals(false, meBody.getValue("needsNickname").jsonPrimitive.boolean)
    }

    @Test
    fun `android referral and play purchase contracts expose mobile fields`() = testApplication {
        val env = testEnv("android-referral-play").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            publicBaseUrl = "https://riverking.example",
            devMode = false,
        )
        application {
            installAuthTestModule(
                env,
                playPurchaseVerifier = FakePlayPurchaseVerifier(
                    purchased = mapOf(
                        "play-token-1" to verifiedPurchase(
                            productId = "fresh_topup_s",
                            accountId = "1",
                            orderId = "gpa.1-1-1-1",
                        )
                    )
                )
            )
        }

        val registered = registerPasswordUser(client, "angler.mobile", "password123")

        val referrals = client.get("/api/referrals") {
            bearerAuth(registered.accessToken)
        }
        assertEquals(HttpStatusCode.OK, referrals.status)
        val referralBody = json.decodeFromString<ReferralResponse>(referrals.bodyAsText())
        assertEquals(true, referralBody.link.startsWith("https://t.me/"))
        assertEquals(referralBody.link, referralBody.telegramLink)
        assertEquals(true, referralBody.androidShareText.isNotBlank())
        assertEquals("https://riverking.example", referralBody.webFallbackLink)

        val purchase = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-1","orderId":"play-order-1"}""")
        }
        assertEquals(HttpStatusCode.OK, purchase.status)

        val duplicate = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-1","orderId":"play-order-1"}""")
        }
        assertEquals(HttpStatusCode.Conflict, duplicate.status)
    }

    @Test
    fun `android play purchase completion rejects pending cancelled and mismatched store states`() = testApplication {
        val env = testEnv("android-referral-play-hardening").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            devMode = false,
        )
        application {
            installAuthTestModule(
                env,
                playPurchaseVerifier = FakePlayPurchaseVerifier(
                    purchased = mapOf(
                        "play-token-mismatch" to verifiedPurchase(
                            productId = "salt_boost_s",
                            accountId = "1",
                            orderId = "gpa.2-2-2-2",
                        ),
                        "play-token-wrong-user" to verifiedPurchase(
                            productId = "fresh_topup_s",
                            accountId = "999",
                            orderId = "gpa.3-3-3-3",
                        ),
                    ),
                    pending = setOf("play-token-pending"),
                    cancelled = setOf("play-token-cancelled"),
                )
            )
        }

        val registered = registerPasswordUser(client, "angler.hardened", "password123")

        val pending = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-pending"}""")
        }
        assertEquals(HttpStatusCode.Conflict, pending.status)

        val cancelled = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-cancelled"}""")
        }
        assertEquals(HttpStatusCode.Conflict, cancelled.status)

        val mismatch = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-mismatch"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, mismatch.status)

        val wrongUser = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-wrong-user"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, wrongUser.status)
    }

    @Test
    fun `android catch details and share card endpoints return localized catch payloads`() = testApplication {
        val env = testEnv("android-catch-card").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            devMode = false,
        )
        application { installAuthTestModule(env) }

        val registered = registerPasswordUser(client, "angler.catch", "password123")

        val start = client.post("/api/start-cast") {
            bearerAuth(registered.accessToken)
        }
        assertEquals(HttpStatusCode.OK, start.status)

        val hook = client.post("/api/hook") {
            bearerAuth(registered.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"wait":10,"reaction":0.6}""")
        }
        assertEquals(HttpStatusCode.OK, hook.status)
        val hookBody = json.parseToJsonElement(hook.bodyAsText()).jsonObject
        assertEquals(true, hookBody.getValue("success").jsonPrimitive.boolean)

        val cast = client.post("/api/cast") {
            bearerAuth(registered.accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"wait":10,"reaction":0.6,"success":true}""")
        }
        assertEquals(HttpStatusCode.OK, cast.status)
        val castBody = json.parseToJsonElement(cast.bodyAsText()).jsonObject
        val catchId = castBody.getValue("catch").jsonObject.getValue("id").jsonPrimitive.content.toLong()

        val details = client.get("/api/catches/$catchId") {
            bearerAuth(registered.accessToken)
        }
        assertEquals(HttpStatusCode.OK, details.status)
        val detailsBody = json.parseToJsonElement(details.bodyAsText()).jsonObject
        assertEquals(true, detailsBody.getValue("fish").jsonPrimitive.content.isNotBlank())
        assertEquals(true, detailsBody.getValue("location").jsonPrimitive.content.isNotBlank())

        val card = client.get("/api/catches/$catchId/card") {
            bearerAuth(registered.accessToken)
        }
        assertEquals(HttpStatusCode.OK, card.status)
        assertEquals(ContentType.Image.PNG.toString(), card.headers[HttpHeaders.ContentType])
        val bytes = card.body<ByteArray>()
        assertEquals(true, bytes.isNotEmpty())
    }

    private fun Application.installAuthTestModule(
        env: Env,
        playPurchaseVerifier: PlayPurchaseVerifier? = null,
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(DoubleReceive)
        installSessions(env)
        DB.init(env)
        apiRoutes(env, playPurchaseVerifier)
        publicPagesRoutes(env)
    }

    private suspend fun registerPasswordUser(
        client: io.ktor.client.HttpClient,
        login: String,
        password: String,
        language: String = "en",
    ): AuthResponse {
        val register = client.post("/api/auth/password/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"$login","password":"$password","language":"$language"}""")
        }
        assertEquals(HttpStatusCode.OK, register.status)
        return json.decodeFromString(register.bodyAsText())
    }

    private fun signedTelegramInitData(botToken: String, userJson: String): String {
        val params = linkedMapOf(
            "auth_date" to "1710000000",
            "query_id" to "AAEAAAE",
            "user" to userJson,
        )
        val dataCheckString = params.toList()
            .sortedBy { it.first }
            .joinToString("\n") { (key, value) -> "$key=$value" }
        val secretKey = hmacSha256(
            key = "WebAppData".toByteArray(StandardCharsets.UTF_8),
            data = botToken.toByteArray(StandardCharsets.UTF_8),
        )
        val hash = hmacSha256(secretKey, dataCheckString.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val encodedParams = params.entries.joinToString("&") { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }
        return "$encodedParams&hash=$hash"
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun verifiedPurchase(
        productId: String,
        accountId: String,
        orderId: String,
    ): VerifiedPlayPurchase = VerifiedPlayPurchase(
        lineItems = listOf(
            VerifiedPlayLineItem(
                productId = productId,
                quantity = 1,
                acknowledgementState = "ACKNOWLEDGEMENT_STATE_PENDING",
                consumptionState = "CONSUMPTION_STATE_YET_TO_BE_CONSUMED",
            )
        ),
        orderId = orderId,
        obfuscatedAccountId = accountId,
        purchaseCompletionTime = Instant.parse("2026-04-09T10:15:30Z"),
        isTestPurchase = true,
    )

    private class FakePlayPurchaseVerifier(
        private val purchased: Map<String, VerifiedPlayPurchase> = emptyMap(),
        private val pending: Set<String> = emptySet(),
        private val cancelled: Set<String> = emptySet(),
    ) : PlayPurchaseVerifier {
        override suspend fun verifyPurchase(purchaseToken: String): PlayPurchaseVerificationResult {
            purchased[purchaseToken]?.let { return PlayPurchaseVerificationResult.Purchased(it) }
            if (purchaseToken in pending) return PlayPurchaseVerificationResult.Pending
            if (purchaseToken in cancelled) return PlayPurchaseVerificationResult.Cancelled
            return PlayPurchaseVerificationResult.NotFound
        }
    }
}
