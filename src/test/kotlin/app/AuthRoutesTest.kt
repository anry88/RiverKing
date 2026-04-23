package app

import db.Catches
import db.DB
import db.Fish
import db.Locations
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import service.AndroidUpdateService
import service.ClubService
import service.ClubQuestService
import service.EventCastAreaDTO
import service.FishingService
import service.PlayPurchaseVerificationResult
import service.PlayPurchaseVerifier
import service.SpecialEventFishSpec
import service.SpecialEventPrizeConfig
import service.SpecialEventService
import service.TournamentService
import service.VerifiedPlayLineItem
import service.VerifiedPlayPurchase
import support.testEnv
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    @Serializable
    private data class UpdateResponse(
        val status: String,
        val latestVersionCode: Int,
        val latestVersionName: String,
        val minSupportedVersionCode: Int,
        val mandatory: Boolean,
        val releaseNotes: List<String>,
        val installMode: String,
        val installUrl: String,
        val fallbackUrl: String? = null,
        val downloadFileName: String? = null,
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
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"angler.one","password":"password123","language":"ru"}""")
        }
        assertEquals(HttpStatusCode.OK, register.status)
        val registered = json.decodeFromString<AuthResponse>(register.bodyAsText())
        assertEquals(true, registered.user.needsNickname)
        assertEquals("ru", registered.user.language)

        val login = client.post("/api/auth/password/login") {
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"angler.one","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val loggedIn = json.decodeFromString<AuthResponse>(login.bodyAsText())
        assertEquals(registered.user.id, loggedIn.user.id)

        val meBeforeNickname = client.get("/api/me") {
            bearerAuth(loggedIn.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, meBeforeNickname.status)
        val meBeforeBody = json.parseToJsonElement(meBeforeNickname.bodyAsText()).jsonObject
        assertEquals(true, meBeforeBody.getValue("needsNickname").jsonPrimitive.boolean)

        val nickname = client.post("/api/nickname") {
            bearerAuth(loggedIn.accessToken)
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"nickname":"Captain River"}""")
        }
        assertEquals(HttpStatusCode.OK, nickname.status)

        val meAfterNickname = client.get("/api/me") {
            bearerAuth(loggedIn.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, meAfterNickname.status)
        val meAfterBody = json.parseToJsonElement(meAfterNickname.bodyAsText()).jsonObject
        assertEquals("Captain River", meAfterBody.getValue("username").jsonPrimitive.content)
        assertEquals(false, meAfterBody.getValue("needsNickname").jsonPrimitive.boolean)

        val logout = client.post("/api/auth/logout") {
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"refreshToken":"${loggedIn.refreshToken}"}""")
        }
        assertEquals(HttpStatusCode.NoContent, logout.status)

        val refreshAfterLogout = client.post("/api/auth/refresh") {
            androidClientHeaders()
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
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.NoContent, delete.status)

        val meAfterDelete = client.get("/api/me") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.Unauthorized, meAfterDelete.status)

        val loginAfterDelete = client.post("/api/auth/password/login") {
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"angler.delete","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, loginAfterDelete.status)

        val refreshAfterDelete = client.post("/api/auth/refresh") {
            androidClientHeaders()
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
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, referrals.status)
        val referralBody = json.decodeFromString<ReferralResponse>(referrals.bodyAsText())
        assertEquals(true, referralBody.link.startsWith("https://t.me/"))
        assertEquals(referralBody.link, referralBody.telegramLink)
        assertEquals(true, referralBody.androidShareText.isNotBlank())
        assertEquals("https://riverking.example", referralBody.webFallbackLink)

        val purchase = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-1","orderId":"play-order-1"}""")
        }
        assertEquals(HttpStatusCode.OK, purchase.status)

        val duplicate = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-1","orderId":"play-order-1"}""")
        }
        assertEquals(HttpStatusCode.Conflict, duplicate.status)
    }

    @Test
    fun `android update endpoint returns install targets and guard blocks stale mobile builds`() = testApplication {
        val env = testEnv("android-update-policy").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            publicBaseUrl = "https://riverking.example",
            itchProjectUrl = "https://itch.example/riverking",
            playStoreUrl = "https://play.example/riverking",
            androidDirectDownloadUrl = "https://itch.example/downloads/riverking.apk",
            devMode = false,
        )
        application { installAuthTestModule(env) }

        val directUpdate = client.get("/api/mobile/update") {
            androidClientHeaders(versionCode = 0, channel = AndroidUpdateService.CHANNEL_DIRECT)
            header(HttpHeaders.AcceptLanguage, "ru-RU,ru;q=0.9")
        }
        assertEquals(HttpStatusCode.OK, directUpdate.status)
        val directBody = json.decodeFromString<UpdateResponse>(directUpdate.bodyAsText())
        assertEquals(AndroidUpdateService.STATUS_UPDATE_REQUIRED, directBody.status)
        assertEquals(true, directBody.mandatory)
        assertEquals(4, directBody.latestVersionCode)
        assertEquals("0.4.0", directBody.latestVersionName)
        assertEquals(4, directBody.minSupportedVersionCode)
        assertEquals(AndroidUpdateService.INSTALL_MODE_DOWNLOAD_APK, directBody.installMode)
        assertEquals("https://itch.example/downloads/riverking.apk", directBody.installUrl)
        assertEquals("https://itch.example/riverking", directBody.fallbackUrl)
        assertEquals(true, directBody.releaseNotes.any { it.contains("обнов", ignoreCase = true) })

        val playUpdate = client.get("/api/mobile/update") {
            androidClientHeaders(versionCode = 0, channel = AndroidUpdateService.CHANNEL_PLAY)
        }
        val playBody = json.decodeFromString<UpdateResponse>(playUpdate.bodyAsText())
        assertEquals(AndroidUpdateService.INSTALL_MODE_EXTERNAL, playBody.installMode)
        assertEquals("https://play.example/riverking", playBody.installUrl)

        val legacyRegister = client.post("/api/auth/password/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"angler.stale","password":"password123","language":"en"}""")
        }
        assertEquals(HttpStatusCode.UpgradeRequired, legacyRegister.status)

        val registered = registerPasswordUser(client, "angler.current", "password123")
        val staleProfile = client.get("/api/me") {
            bearerAuth(registered.accessToken)
            androidClientHeaders(versionCode = 2, versionName = "0.2.2")
        }
        assertEquals(HttpStatusCode.UpgradeRequired, staleProfile.status)

        val currentProfile = client.get("/api/me") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, currentProfile.status)
    }

    @Test
    fun `current tournament route returns tournament created with admin timestamp payload`() = testApplication {
        val env = testEnv("admin-created-current-tournament").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            devMode = false,
        )
        application { installAuthTestModule(env) }
        val registered = registerPasswordUser(client, "angler.admin.tournament", "password123", language = "ru")
        val now = Instant.now()
        val tournamentId = TournamentService().createTournament(
            nameRu = "тест",
            nameEn = "test",
            start = Instant.ofEpochMilli(now.minusSeconds(3_600).toEpochMilli()),
            end = Instant.ofEpochMilli(now.plusSeconds(3_600).toEpochMilli()),
            fish = "Карась",
            location = null,
            metric = "largest",
            prizePlaces = 5,
            prizes = """[{"pack":"coins","qty":1500,"coins":1500},{"pack":"autofish_week","qty":2},{"pack":"autofish"},{"pack":"fresh_topup_s","qty":2},{"pack":"bundle_pro"}]""",
        )

        val current = client.get("/api/tournament/current") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }

        assertEquals(HttpStatusCode.OK, current.status)
        val body = json.parseToJsonElement(current.bodyAsText()).jsonObject
        val tournament = body.getValue("tournament").jsonObject
        assertEquals(tournamentId, tournament.getValue("id").jsonPrimitive.content.toLong())
        assertEquals("тест", tournament.getValue("name").jsonPrimitive.content)
        assertEquals("Карась", tournament.getValue("fish").jsonPrimitive.content)
    }

    @Test
    fun `me shows event location locked until user joins a club`() = testApplication {
        val env = testEnv("event-location-in-me").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            publicBaseUrl = "https://riverking.example",
            devMode = false,
        )
        application { installAuthTestModule(env) }
        val registered = registerPasswordUser(client, "angler.event.location", "password123")
        val eventId = createCurrentSpecialEvent()

        val withoutClub = client.get("/api/me") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, withoutClub.status)
        val withoutClubBody = json.parseToJsonElement(withoutClub.bodyAsText()).jsonObject
        val lockedEventLocation = withoutClubBody.getValue("locations").jsonArray.first().jsonObject
        assertEquals(eventId, lockedEventLocation.getValue("eventId").jsonPrimitive.content.toLong())
        assertEquals(true, lockedEventLocation.getValue("isEvent").jsonPrimitive.boolean)
        assertEquals(false, lockedEventLocation.getValue("unlocked").jsonPrimitive.boolean)
        assertTrue(lockedEventLocation.getValue("lockedReason").jsonPrimitive.content.contains("Join a club"))

        val fishing = FishingService()
        fishing.addCoins(registered.user.id, ClubService.CREATE_COST_COINS.toInt())
        addProgressWeight(registered.user.id, ClubService.MIN_CREATE_WEIGHT_KG + 25.0)
        ClubService().createClub(registered.user.id, "Event Club")

        val withClub = client.get("/api/me") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, withClub.status)
        val withClubBody = json.parseToJsonElement(withClub.bodyAsText()).jsonObject
        val unlockedEventLocation = withClubBody.getValue("locations").jsonArray.first().jsonObject
        assertEquals(eventId, unlockedEventLocation.getValue("eventId").jsonPrimitive.content.toLong())
        assertEquals(true, unlockedEventLocation.getValue("isEvent").jsonPrimitive.boolean)
        assertEquals(true, unlockedEventLocation.getValue("unlocked").jsonPrimitive.boolean)
        assertEquals(
            unlockedEventLocation.getValue("id").jsonPrimitive.content.toLong(),
            withClubBody.getValue("locationId").jsonPrimitive.content.toLong(),
        )

        val eventGuide = client.get("/api/guide/event-locations?limit=10&offset=0") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, eventGuide.status)
        val eventGuideBody = json.parseToJsonElement(eventGuide.bodyAsText()).jsonObject
        val eventLocation = eventGuideBody.getValue("locations").jsonArray.single().jsonObject
        assertEquals(
            unlockedEventLocation.getValue("id").jsonPrimitive.content.toLong(),
            eventLocation.getValue("id").jsonPrimitive.content.toLong(),
        )
        assertEquals("https://riverking.example/event-assets/event-bay.webp", eventLocation.getValue("imageUrl").jsonPrimitive.content)
        assertEquals(true, eventLocation.getValue("isEvent").jsonPrimitive.boolean)
        assertEquals(false, eventGuideBody["hasMore"]?.jsonPrimitive?.boolean ?: false)
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
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-pending"}""")
        }
        assertEquals(HttpStatusCode.Conflict, pending.status)

        val cancelled = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-cancelled"}""")
        }
        assertEquals(HttpStatusCode.Conflict, cancelled.status)

        val mismatch = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"purchaseToken":"play-token-mismatch"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, mismatch.status)

        val wrongUser = client.post("/api/shop/fresh_topup_s/play/complete") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
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

        var hooked = false
        for (attempt in 1..5) {
            val start = client.post("/api/start-cast") {
                bearerAuth(registered.accessToken)
                androidClientHeaders()
            }
            assertEquals(HttpStatusCode.OK, start.status)

            val hook = client.post("/api/hook") {
                bearerAuth(registered.accessToken)
                androidClientHeaders()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"wait":10,"reaction":0.6}""")
            }
            assertEquals(HttpStatusCode.OK, hook.status)
            val hookBody = json.parseToJsonElement(hook.bodyAsText()).jsonObject
            if (hookBody.getValue("success").jsonPrimitive.boolean) {
                val hookedFish = hookBody.getValue("hookedFish").jsonObject
                assertEquals(true, hookedFish.getValue("fish").jsonPrimitive.content.isNotBlank())
                assertEquals(true, hookedFish.getValue("location").jsonPrimitive.content.isNotBlank())
                assertEquals(true, hookedFish.getValue("rarity").jsonPrimitive.content.isNotBlank())
                assertEquals(true, hookedFish.getValue("weight").jsonPrimitive.content.toDouble() > 0.0)

                val challenge = hookBody.getValue("challenge").jsonObject
                val tapGoal = challenge.getValue("tapGoal").jsonPrimitive.content.toInt()
                val durationMs = challenge.getValue("durationMs").jsonPrimitive.content.toInt()
                val struggleIntensity = challenge.getValue("struggleIntensity").jsonPrimitive.content.toDouble()
                assertEquals(true, tapGoal >= 3)
                assertEquals(true, durationMs in setOf(5_000, 10_000, 15_000))
                assertEquals(true, struggleIntensity in 0.0..1.0)
                hooked = true
                break
            }
        }
        assertTrue(hooked)

        val cast = client.post("/api/cast") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"wait":10,"reaction":0.6,"success":true}""")
        }
        assertEquals(HttpStatusCode.OK, cast.status)
        val castBody = json.parseToJsonElement(cast.bodyAsText()).jsonObject
        val catchId = castBody.getValue("catch").jsonObject.getValue("id").jsonPrimitive.content.toLong()

        val details = client.get("/api/catches/$catchId") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, details.status)
        val detailsBody = json.parseToJsonElement(details.bodyAsText()).jsonObject
        assertEquals(true, detailsBody.getValue("fish").jsonPrimitive.content.isNotBlank())
        assertEquals(true, detailsBody.getValue("location").jsonPrimitive.content.isNotBlank())

        val card = client.get("/api/catches/$catchId/card") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, card.status)
        assertEquals(ContentType.Image.PNG.toString(), card.headers[HttpHeaders.ContentType])
        val bytes = card.body<ByteArray>()
        assertEquals(true, bytes.isNotEmpty())
    }

    @Test
    fun `quests api includes club section for members and non members`() = testApplication {
        val env = testEnv("quests-api-club-section").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            devMode = false,
        )
        application { installAuthTestModule(env) }

        val registered = registerPasswordUser(client, "angler.quests", "password123")

        val withoutClub = client.get("/api/quests") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, withoutClub.status)
        val withoutClubBody = json.parseToJsonElement(withoutClub.bodyAsText()).jsonObject
        val withoutClubSection = withoutClubBody.getValue("club").jsonObject
        assertEquals(false, withoutClubSection.getValue("available").jsonPrimitive.boolean)
        assertTrue(withoutClubSection.getValue("message").jsonPrimitive.content.contains("Join a club"))

        val fishing = FishingService()
        fishing.addCoins(registered.user.id, ClubService.CREATE_COST_COINS.toInt())
        addProgressWeight(registered.user.id, ClubService.MIN_CREATE_WEIGHT_KG + 25.0)
        ClubService().createClub(registered.user.id, "Quest Riders")

        val withClub = client.get("/api/quests") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, withClub.status)
        val withClubBody = json.parseToJsonElement(withClub.bodyAsText()).jsonObject
        val withClubSection = withClubBody.getValue("club").jsonObject
        assertEquals(true, withClubSection.getValue("available").jsonPrimitive.boolean)
        assertEquals(2, withClubSection.getValue("quests").jsonArray.size)
    }

    @Test
    fun `club api exposes weekly quest progress and member contribution fields`() = testApplication {
        val env = testEnv("club-api-quest-analytics").copy(
            botToken = "test-bot-token",
            botName = "river_king_bot",
            devMode = false,
        )
        application { installAuthTestModule(env) }

        val registered = registerPasswordUser(client, "angler.club.page", "password123")
        val fishing = FishingService()
        val clubs = ClubService()
        val clubQuests = ClubQuestService()

        fishing.addCoins(registered.user.id, ClubService.CREATE_COST_COINS.toInt())
        addProgressWeight(registered.user.id, ClubService.MIN_CREATE_WEIGHT_KG + 25.0)
        val clubId = clubs.createClub(registered.user.id, "Weekly View").id
        val partnerId = fishing.ensureUserByTgId(9_901L)
        clubs.joinClub(partnerId, clubId)

        val initial = client.get("/api/club") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, initial.status)
        val initialBody = json.parseToJsonElement(initial.bodyAsText()).jsonObject
        val currentQuestWeek = initialBody.getValue("currentQuestWeek").jsonObject
        val currentQuests = currentQuestWeek.getValue("quests").jsonArray
        assertEquals(2, currentQuests.size)

        val chosenCode = currentQuests.first().jsonObject.getValue("code").jsonPrimitive.content
        val scenario = clubQuestScenario(chosenCode)
        recordClubCatch(clubQuests, registered.user.id, scenario, currentClubWeekInstant(dayOffset = 1))

        val updated = client.get("/api/club") {
            bearerAuth(registered.accessToken)
            androidClientHeaders()
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        val updatedBody = json.parseToJsonElement(updated.bodyAsText()).jsonObject
        val updatedQuest = updatedBody
            .getValue("currentQuestWeek")
            .jsonObject
            .getValue("quests")
            .jsonArray
            .map { it.jsonObject }
            .first { it.getValue("code").jsonPrimitive.content == chosenCode }

        assertEquals(true, updatedBody.containsKey("previousQuestWeek"))
        assertTrue(updatedQuest.getValue("progress").jsonPrimitive.content.toInt() >= 1)

        val members = updatedQuest.getValue("members").jsonArray.map { it.jsonObject }
        assertEquals(2, members.size)
        val currentUserProgress = members
            .first { it.getValue("userId").jsonPrimitive.content.toLong() == registered.user.id }
            .getValue("progress")
            .jsonPrimitive
            .content
            .toInt()
        assertTrue(currentUserProgress >= 1)
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
            androidClientHeaders()
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"login":"$login","password":"$password","language":"$language"}""")
        }
        assertEquals(HttpStatusCode.OK, register.status)
        return json.decodeFromString(register.bodyAsText())
    }

    private fun HttpRequestBuilder.androidClientHeaders(
        versionCode: Int = 4,
        versionName: String = "0.4.0",
        channel: String = AndroidUpdateService.CHANNEL_DIRECT,
    ) {
        header(AndroidUpdateService.HEADER_PLATFORM, AndroidUpdateService.PLATFORM_ANDROID)
        header(AndroidUpdateService.HEADER_CHANNEL, channel)
        header(AndroidUpdateService.HEADER_VERSION_CODE, versionCode.toString())
        header(AndroidUpdateService.HEADER_VERSION_NAME, versionName)
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

    private fun addProgressWeight(userId: Long, weightKg: Double) {
        val (fishId, locationId) = transaction {
            val locationId = Locations.selectAll()
                .orderBy(Locations.id, SortOrder.ASC)
                .limit(1)
                .single()[Locations.id].value
            val fishId = Fish.selectAll()
                .orderBy(Fish.id, SortOrder.ASC)
                .limit(1)
                .single()[Fish.id].value
            fishId to locationId
        }
        transaction {
            Catches.insert {
                it[Catches.userId] = userId
                it[Catches.fishId] = fishId
                it[Catches.weight] = weightKg
                it[Catches.locationId] = locationId
                it[Catches.createdAt] = Instant.now()
                it[Catches.coins] = null
            }
        }
    }

    private fun createCurrentSpecialEvent(): Long {
        val fishId = transaction { Fish.select { Fish.name eq "Плотва" }.single()[Fish.id].value }
        val now = Instant.now()
        return SpecialEventService().createEvent(
            nameRu = "Ивентовая бухта",
            nameEn = "Event Bay",
            start = now.minusSeconds(3_600),
            end = now.plusSeconds(3_600),
            imagePath = "event-bay.webp",
            castArea = EventCastAreaDTO(0.12, 0.88, 0.42, 0.78),
            fish = listOf(SpecialEventFishSpec(fishId, 1.0)),
            weightPrizes = SpecialEventPrizeConfig(0, "[]"),
            countPrizes = SpecialEventPrizeConfig(0, "[]"),
            fishPrizes = SpecialEventPrizeConfig(0, "[]"),
        )
    }

    private data class ClubQuestScenario(
        val fishName: String,
        val rarity: String,
        val locationName: String,
        val weight: Double,
    )

    private fun clubQuestScenario(code: String): ClubQuestScenario = when (code) {
        "club_epic_20" -> ClubQuestScenario("Паку бурый", "epic", "Русло Амазонки", 12.0)
        "club_common_200" -> ClubQuestScenario("Уклейка", "common", "Пруд", 0.2)
        "club_uncommon_100" -> ClubQuestScenario("Пелядь", "uncommon", "Пруд", 1.1)
        "club_ruffe_40" -> ClubQuestScenario("Ёрш", "common", "Пруд", 0.2)
        "club_bream_30" -> ClubQuestScenario("Лещ", "uncommon", "Пруд", 1.2)
        "club_crucian_50" -> ClubQuestScenario("Карась", "common", "Пруд", 0.4)
        "club_roach_50" -> ClubQuestScenario("Плотва", "common", "Пруд", 0.25)
        "club_rare_50" -> ClubQuestScenario("Карп", "rare", "Пруд", 2.6)
        "club_perch_50" -> ClubQuestScenario("Окунь", "common", "Пруд", 0.3)
        "club_herring_50" -> ClubQuestScenario("Сельдь", "common", "Прибрежье моря", 0.4)
        else -> error("Unexpected quest code: $code")
    }

    private fun recordClubCatch(
        clubQuests: ClubQuestService,
        userId: Long,
        scenario: ClubQuestScenario,
        at: Instant,
    ) {
        val (fishId, locationId) = transaction {
            val fishId = Fish.select { Fish.name eq scenario.fishName }.single()[Fish.id].value
            val locationId = Locations.select { Locations.name eq scenario.locationName }.single()[Locations.id].value
            fishId to locationId
        }
        transaction {
            Catches.insert {
                it[Catches.userId] = userId
                it[Catches.fishId] = fishId
                it[Catches.weight] = scenario.weight
                it[Catches.locationId] = locationId
                it[Catches.createdAt] = at
                it[Catches.coins] = null
            }
        }
        clubQuests.updateOnCatch(userId, scenario.fishName, scenario.rarity, at)
    }

    private fun currentClubWeekInstant(dayOffset: Long = 0, secondOffset: Long = 0): Instant {
        val zone = ZoneId.of("Europe/Belgrade")
        val weekStart = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return weekStart
            .plusDays(dayOffset)
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .plusSeconds(secondOffset)
    }

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
