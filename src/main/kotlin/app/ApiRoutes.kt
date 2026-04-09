package app

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import service.FishingService
import service.LocationDTO
import service.RecentDTO
import service.RodDTO
import service.FishingService.LureDTO
import service.FishingService.CatchDTO
import service.TournamentService
import service.PrizeService
import service.RatingPrizeService
import service.I18n
import service.PrizeSpec
import service.COIN_PRIZE_ID
import service.ReferralService
import service.AchievementService
import service.AchievementRewardDTO
import service.QuestService
import service.ClubService
import service.PrizeSource
import db.Users
import service.PayService
import service.StarsPaymentService
import util.Metrics
import java.time.Instant

fun Application.apiRoutes(env: Env) {
    val fishing = FishingService()
    val auth = AuthService(env, fishing)
    val log = LoggerFactory.getLogger("Api")
    val stars = StarsPaymentService(env, fishing)
    val tournaments = TournamentService()
    val ratingPrizes = RatingPrizeService()
    val clubs = ClubService()
    val prizeService = PrizeService(tournaments, ratingPrizes, clubs)
    val bot = TelegramBot(env.botToken)
    val rarityGroups = setOf("common", "uncommon", "rare", "epic", "mythic", "legendary")

    // Use the Plugins phase so that sessions are already available
    // when logging each incoming request. Intercepting earlier can
    // cause SessionNotYetConfiguredException for routes like `/app`
    // that are served before the Sessions plugin runs.
    intercept(ApplicationCallPipeline.Plugins) {
        val path = call.request.path()
        if (path != "/metrics") {
            val session = call.sessions.get<AppSession>()
            val userId = session?.userId
            val params = call.parameters.entries().associate { it.key to it.value.joinToString(",") }
            val body = if (call.request.httpMethod in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
                try { call.receiveText() } catch (_: Exception) { "" }
            } else ""
            log.info(
                "call {} {} userId={} params={} body={}",
                call.request.httpMethod.value,
                call.request.uri,
                userId,
                params,
                body
            )
            Metrics.counter(
                "api_call_total",
                mapOf(
                    "path" to path,
                    "method" to call.request.httpMethod.value
                )
            )
        }
        proceed()
    }

    @Serializable
    data class CastReq(val wait: Int, val reaction: Double, val success: Boolean)

    @Serializable
    data class HookReq(val wait: Int, val reaction: Double)

    @Serializable
    data class ShopPackageDTO(
        val id: String,
        val name: String,
        val desc: String,
        val price: Int,
        val until: String? = null,
        val originalPrice: Int? = null,
        val discountStart: String? = null,
        val discountEnd: String? = null,
        val coinPrice: Int? = null,
        val rodCode: String? = null,
    )

    @Serializable
    data class ShopCategoryDTO(val id: String, val name: String, val packs: List<ShopPackageDTO>)

    @Serializable
    data class ShopBuyResp(val lures: List<LureDTO>, val currentLureId: Long?)

    @Serializable
    data class PaymentReq(
        val telegramChargeId: String,
        val providerChargeId: String? = null,
        val amount: Int = 0,
        val currency: String = "XTR",
    )

    @Serializable
    data class InvoiceReq(val productId: String, val initData: String)

    @Serializable
    data class InvoiceResp(val invoice_url: String)

    @Serializable
    data class PrizeDTO(
        val id: Long,
        val packageId: String,
        val qty: Int,
        val rank: Int,
        val coins: Int? = null,
        val source: String = PrizeSource.TOURNAMENT.name.lowercase(),
    )

    @Serializable
    data class PrizeSpecDTO(val packageId: String, val qty: Int, val coins: Int? = null)

    @Serializable
    data class ReferralRewardDTO(val packageId: String, val qty: Int, val name: String)

    @Serializable
    data class ClubMemberDTO(
        val userId: Long,
        val name: String?,
        val role: String,
        val coins: Int,
    )

    @Serializable
    data class ClubWeekDTO(
        val weekStart: String,
        val totalCoins: Int,
        val members: List<ClubMemberDTO>,
    )

    @Serializable
    data class ClubDetailsDTO(
        val id: Long,
        val name: String,
        val role: String,
        val memberCount: Int,
        val capacity: Int,
        val info: String,
        val minJoinWeightKg: Double,
        val recruitingOpen: Boolean,
        val currentWeek: ClubWeekDTO,
        val previousWeek: ClubWeekDTO,
    )

    @Serializable
    data class ClubChatMessageDTO(
        val id: Long,
        val message: String,
        val createdAt: String,
    )

    @Serializable
    data class ClubSummaryDTO(
        val id: Long,
        val name: String,
        val memberCount: Int,
        val capacity: Int,
        val info: String,
        val minJoinWeightKg: Double,
        val recruitingOpen: Boolean,
    )

    fun ClubService.ClubWeekView.toDto(): ClubWeekDTO = ClubWeekDTO(
        weekStart = weekStart.toString(),
        totalCoins = totalCoins,
        members = members.map {
            ClubMemberDTO(
                userId = it.userId,
                name = it.name,
                role = it.role,
                coins = it.coins,
            )
        },
    )

    fun ClubService.ClubDetails.toDto(): ClubDetailsDTO = ClubDetailsDTO(
        id = id,
        name = name,
        role = role,
        memberCount = memberCount,
        capacity = capacity,
        info = info,
        minJoinWeightKg = minJoinWeightKg,
        recruitingOpen = recruitingOpen,
        currentWeek = currentWeek.toDto(),
        previousWeek = previousWeek.toDto(),
    )

    fun ClubService.ClubChatMessage.toDto(): ClubChatMessageDTO = ClubChatMessageDTO(
        id = id,
        message = message,
        createdAt = createdAt.toString(),
    )

    @Serializable
    data class TournamentDTO(
        val id: Long,
        val name: String,
        val startTime: Long,
        val endTime: Long,
        val fish: String? = null,
        val fishRarity: String? = null,
        val location: String? = null,
        val metric: String,
        val prizePlaces: Int,
    )

    @Serializable
    data class LeaderboardEntryDTO(
        val rank: Int,
        val userId: Long? = null,
        val user: String? = null,
        val value: Double,
        val catchId: Long? = null,
        val fish: String? = null,
        val fishId: Long? = null,
        val location: String? = null,
        val at: Long? = null,
        val prize: PrizeSpecDTO? = null,
    )

    fun PrizeSpec.toDtoOrNull(): PrizeSpecDTO? {
        val isCoins = pack == COIN_PRIZE_ID || coins != null
        return when {
            isCoins -> {
                val amount = coins ?: qty
                if (amount <= 0) null else PrizeSpecDTO(COIN_PRIZE_ID, amount, amount)
            }
            pack.isBlank() -> null
            else -> PrizeSpecDTO(pack, qty, coins)
        }
    }

    @Serializable
    data class CurrentTournamentDTO(
        val tournament: TournamentDTO,
        val leaderboard: List<LeaderboardEntryDTO>,
        val mine: LeaderboardEntryDTO? = null,
    )

    @Serializable
    data class AuthUserDTO(
        val id: Long,
        val username: String?,
        val needsNickname: Boolean,
        val language: String,
    )

    @Serializable
    data class AuthResponseDTO(
        val accessToken: String,
        val accessTokenExpiresAt: Long,
        val refreshToken: String,
        val user: AuthUserDTO,
    )

    @Serializable
    data class GoogleAuthReq(val idToken: String, val ref: String? = null)

    @Serializable
    data class PasswordRegisterReq(
        val login: String,
        val password: String,
        val language: String? = null,
        val ref: String? = null,
    )

    @Serializable
    data class PasswordLoginReq(val login: String, val password: String)

    @Serializable
    data class RefreshReq(val refreshToken: String)

    fun ApplicationCall.deviceInfo(): String? =
        request.headers[HttpHeaders.UserAgent]?.takeIf { it.isNotBlank() }

    fun ApplicationCall.bearerToken(): String? {
        val header = request.headers[HttpHeaders.Authorization] ?: return null
        if (!header.startsWith("Bearer ", ignoreCase = true)) return null
        return header.removePrefix("Bearer ").trim().takeIf { it.isNotBlank() }
    }

    fun currentUserSummary(userId: Long): AuthUserDTO {
        val displayName = fishing.displayName(userId)
        val language = fishing.userLanguage(userId)
        return AuthUserDTO(
            id = userId,
            username = displayName,
            needsNickname = displayName == null,
            language = language,
        )
    }

    fun buildAuthResponse(result: AuthService.AuthResult): AuthResponseDTO =
        AuthResponseDTO(
            accessToken = result.accessToken,
            accessTokenExpiresAt = result.accessTokenExpiresAt.epochSecond,
            refreshToken = result.refreshToken,
            user = currentUserSummary(result.userId),
        )

    suspend fun ApplicationCall.requireUserId(): Long? {
        sessions.get<AppSession>()?.userId?.let { return fishing.ensureUserById(it) }
        bearerToken()?.let { token ->
            auth.resolveAccessToken(token)?.let { return it }
        }
        return if (env.devMode) fishing.ensureUserByTgId(1L) else null
    }

    routing {
        get("/api/assets/{path...}") {
            call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val segments = call.parameters.getAll("path")?.takeIf { it.isNotEmpty() }
                ?: return@get call.respond(HttpStatusCode.NotFound)
            val sanitized = segments.map { it.trim() }
            if (sanitized.any { segment ->
                    segment.isEmpty() || segment == "." || segment == ".." ||
                        segment.contains("..") || segment.contains('\\')
                }) {
                return@get call.respond(HttpStatusCode.BadRequest)
            }
            val relativePath = sanitized.joinToString("/")
            val resourcePath = "webapp/assets/$relativePath"
            val resourceStream = call.application.environment.classLoader
                .getResourceAsStream(resourcePath)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            val bytes = withContext(Dispatchers.IO) {
                resourceStream.use { it.readBytes() }
            }
            val contentType = ContentType.fromFilePath(relativePath).firstOrNull()
                ?: ContentType.Application.OctetStream
            call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
            call.respondBytes(bytes, contentType)
        }

        // Telegram WebApp auth: client sends initData in a header
        post("/api/auth/telegram") {
            val initData = call.request.headers["Telegram-Init-Data"] ?: call.receiveText()
            if (initData.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, "missing initData")
            val tgUser = try { TgWebAppAuth.verifyAndExtractUser(initData, env.botToken) }
            catch (_: Exception) { return@post call.respond(HttpStatusCode.Unauthorized, "bad initData") }
            val ref = call.request.queryParameters["ref"]
            val userId = fishing.ensureUserByTgId(
                tgUser.id,
                tgUser.firstName,
                tgUser.lastName,
                tgUser.username,
                tgUser.languageCode,
                ref,
            )
            call.sessions.set(AppSession(userId))
            call.respond(HttpStatusCode.OK)
        }

        post("/api/auth/google") {
            val req = try { call.receive<GoogleAuthReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val result = try {
                auth.loginGoogle(
                    idToken = req.idToken,
                    refToken = req.ref,
                    deviceInfo = call.deviceInfo(),
                )
            } catch (e: AuthService.AuthException) {
                return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            }
            call.respond(buildAuthResponse(result))
        }

        post("/api/auth/password/register") {
            val req = try { call.receive<PasswordRegisterReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val result = try {
                auth.registerPassword(
                    login = req.login,
                    password = req.password,
                    language = req.language,
                    refToken = req.ref,
                    deviceInfo = call.deviceInfo(),
                )
            } catch (e: AuthService.AuthException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
            call.respond(buildAuthResponse(result))
        }

        post("/api/auth/password/login") {
            val req = try { call.receive<PasswordLoginReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val result = try {
                auth.loginPassword(req.login, req.password, call.deviceInfo())
            } catch (e: AuthService.AuthException) {
                return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            }
            call.respond(buildAuthResponse(result))
        }

        post("/api/auth/refresh") {
            val req = try { call.receive<RefreshReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val result = try {
                auth.refresh(req.refreshToken, call.deviceInfo())
            } catch (e: AuthService.AuthException) {
                return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            }
            call.respond(buildAuthResponse(result))
        }

        post("/api/auth/logout") {
            val req = try { call.receive<RefreshReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            auth.logout(req.refreshToken)
            call.respond(HttpStatusCode.NoContent)
        }

        // Profile
        get("/api/me") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            fishing.resetCasting(uid)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val lures = fishing.listLures(uid).map {
                it.copy(
                    displayName = I18n.lure(it.name, language),
                    description = I18n.lureDescription(it.name, language),
                )
            }
            val rods = fishing.listRods(uid).map { it.copy(name = I18n.rod(it.name, language)) }
            val totalWeight = fishing.totalCaughtKg(uid)
            val todayWeight = fishing.todayCaughtKg(uid)
            val locs = fishing.locations(uid).map { it.copy(name = I18n.location(it.name, language)) }
            val dailyAvailable = fishing.canClaimDaily(uid)
            val dailyStreak = transaction { Users.select { Users.id eq uid }.single()[Users.dailyStreak] }

            @Serializable
            data class DailyRewardItemDTO(val name: String, val qty: Int)

            val dailyRewards = fishing.dailyRewardSchedule(uid).map { day ->
                day.map { DailyRewardItemDTO(I18n.lure(it.name, language), it.qty) }
            }
            val displayName = fishing.displayName(uid)
            val storedLoc = transaction {
                Users.selectAll().where { Users.id eq uid }.single()[Users.currentLocationId]?.value
            }
            val currentLocId = storedLoc?.takeIf { id -> locs.any { it.id == id && it.unlocked } }
                ?: locs.first { it.unlocked }.id
            val currentLureId = transaction {
                Users.select { Users.id eq uid }.single()[Users.currentLureId]?.value
            }
            val currentRodId = transaction {
                Users.select { Users.id eq uid }.single()[Users.currentRodId]?.value
            }
            val recent = fishing.recent(uid).map { r ->
                r.copy(
                    fish = I18n.fish(r.fish, language),
                    location = I18n.location(r.location, language)
                )
            }
            val caughtFishIds = fishing.caughtFishIds(uid)
            val autoFish = transaction {
                Users.select { Users.id eq uid }.single()[Users.autoFishUntil]
                    ?.isAfter(Instant.now()) ?: false
            }
            val totalCoins = transaction { Users.select { Users.id eq uid }.single()[Users.coins] }
            val todayCoins = fishing.todayCoins(uid)

            @Serializable
            data class MeResp(
                val id: Long,
                val username: String?,
                val needsNickname: Boolean,
                val lures: List<LureDTO>,
                val currentLureId: Long?,
                val rods: List<RodDTO>,
                val currentRodId: Long?,
                val totalWeight: Double,
                val todayWeight: Double,
                val locationId: Long,
                val locations: List<LocationDTO>,
                val caughtFishIds: List<Long>,
                val recent: List<RecentDTO>,
                val dailyAvailable: Boolean,
                val dailyStreak: Int,
                val dailyRewards: List<List<DailyRewardItemDTO>>,
                val autoFish: Boolean,
                val language: String,
                val coins: Long,
                val todayCoins: Long,
            )
            call.respond(
                MeResp(
                    uid,
                    displayName,
                    displayName == null,
                    lures,
                    currentLureId,
                    rods,
                    currentRodId,
                    totalWeight,
                    todayWeight,
                    currentLocId,
                    locs,
                    caughtFishIds,
                    recent,
                    dailyAvailable,
                    dailyStreak,
                    dailyRewards,
                    autoFish,
                    language,
                    totalCoins,
                    todayCoins,
                )
            )
        }

        post("/api/nickname") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            @Serializable data class NickReq(val nickname: String)
            val req = call.receive<NickReq>()
            val sanitized = fishing.setNickname(uid, req.nickname)
            call.respond(HttpStatusCode.OK, mapOf("nickname" to sanitized))
        }

        post("/api/language") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            @Serializable data class LangReq(val language: String)
            val req = call.receive<LangReq>()
            fishing.setLanguage(uid, req.language)
            call.respond(HttpStatusCode.OK)
        }

        get("/api/tournament/current") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val t = tournaments.currentTournament()
                ?: return@get call.respond(HttpStatusCode.NoContent)
            val (top, mine) = tournaments.leaderboard(t, uid, t.prizePlaces)
            val prizes = try { Json.decodeFromString<List<PrizeSpec>>(t.prizesJson) } catch (_: Exception) { emptyList() }
            val fishRarity = t.fish?.let { f -> if (f in rarityGroups) f else fishing.fishRarity(f) }
            val fishName = t.fish?.takeUnless { it in rarityGroups }?.let { I18n.fish(it, language) }
            val dto = TournamentDTO(
                id = t.id,
                name = if (language == "en") t.nameEn else t.nameRu,
                startTime = t.startTime.epochSecond,
                endTime = t.endTime.epochSecond,
                fish = fishName,
                fishRarity = fishRarity,
                location = t.location?.let { I18n.location(it, language) },
                metric = t.metric.lowercase(),
                prizePlaces = t.prizePlaces,
            )
            val resp = CurrentTournamentDTO(
                tournament = dto,
                leaderboard = top.map {
                    LeaderboardEntryDTO(
                        rank = it.rank,
                        userId = it.userId,
                        user = it.user,
                        value = it.value,
                        catchId = it.catchId,
                        fish = it.fish?.let { f -> I18n.fish(f, language) },
                        fishId = it.fishId,
                        location = it.location?.let { l -> I18n.location(l, language) },
                        at = it.at?.epochSecond,
                        prize = prizes.getOrNull(it.rank - 1)?.toDtoOrNull(),
                    )
                },
                mine = mine?.let {
                    LeaderboardEntryDTO(
                        rank = it.rank,
                        userId = it.userId,
                        user = it.user,
                        value = it.value,
                        catchId = it.catchId,
                        fish = it.fish?.let { f -> I18n.fish(f, language) },
                        fishId = it.fishId,
                        location = it.location?.let { l -> I18n.location(l, language) },
                        at = it.at?.epochSecond,
                        prize = prizes.getOrNull(it.rank - 1)?.toDtoOrNull(),
                    )
                },
            )
            call.respond(resp)
        }

        get("/api/achievements") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val list = AchievementService.list(uid, language)
            Metrics.counter("achievements_view_total", mapOf("source" to "app"))
            call.respond(list)
        }

        get("/api/quests") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val list = QuestService.list(uid, language)
            Metrics.counter("quests_view_total", mapOf("source" to "app"))
            call.respond(list)
        }

        post("/api/achievements/{code}/claim") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val code = call.parameters["code"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val reward = AchievementService.claim(uid, code)
                ?: return@post call.respond(HttpStatusCode.NotFound)
            reward.rewards.forEach { prize ->
                when {
                    prize.pack.equals(COIN_PRIZE_ID, ignoreCase = true) -> {
                        val amount = prize.coins ?: prize.qty
                        fishing.addCoins(uid, amount)
                    }
                    prize.pack.isNotBlank() -> {
                        repeat(prize.qty.coerceAtLeast(1)) { fishing.buyPackage(uid, prize.pack) }
                    }
                }
            }
            Metrics.counter("achievements_claim_total", mapOf("source" to "app", "code" to code))
            call.respond(reward)
        }

        get("/api/tournament/{id}") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val t = tournaments.getTournament(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            val (top, mine) = tournaments.leaderboard(t, uid, t.prizePlaces)
            val prizes = try { Json.decodeFromString<List<PrizeSpec>>(t.prizesJson) } catch (_: Exception) { emptyList() }
            val fishRarity = t.fish?.let { f -> if (f in rarityGroups) f else fishing.fishRarity(f) }
            val fishName = t.fish?.takeUnless { it in rarityGroups }?.let { I18n.fish(it, language) }
            val dto = TournamentDTO(
                id = t.id,
                name = if (language == "en") t.nameEn else t.nameRu,
                startTime = t.startTime.epochSecond,
                endTime = t.endTime.epochSecond,
                fish = fishName,
                fishRarity = fishRarity,
                location = t.location?.let { I18n.location(it, language) },
                metric = t.metric.lowercase(),
                prizePlaces = t.prizePlaces,
            )
            val resp = CurrentTournamentDTO(
                tournament = dto,
                leaderboard = top.map {
                    LeaderboardEntryDTO(
                        rank = it.rank,
                        userId = it.userId,
                        user = it.user,
                        value = it.value,
                        catchId = it.catchId,
                        fish = it.fish?.let { f -> I18n.fish(f, language) },
                        fishId = it.fishId,
                        location = it.location?.let { l -> I18n.location(l, language) },
                        at = it.at?.epochSecond,
                        prize = prizes.getOrNull(it.rank - 1)?.toDtoOrNull(),
                    )
                },
                mine = mine?.let {
                    LeaderboardEntryDTO(
                        rank = it.rank,
                        userId = it.userId,
                        user = it.user,
                        value = it.value,
                        catchId = it.catchId,
                        fish = it.fish?.let { f -> I18n.fish(f, language) },
                        fishId = it.fishId,
                        location = it.location?.let { l -> I18n.location(l, language) },
                        at = it.at?.epochSecond,
                        prize = prizes.getOrNull(it.rank - 1)?.toDtoOrNull(),
                    )
                },
            )
            call.respond(resp)
        }

        get("/api/tournaments/upcoming") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val list = tournaments.upcomingTournaments().map { t ->
                val fishRarity = t.fish?.let { f -> if (f in rarityGroups) f else fishing.fishRarity(f) }
                val fishName = t.fish?.takeUnless { it in rarityGroups }?.let { I18n.fish(it, language) }
                TournamentDTO(
                    id = t.id,
                    name = if (language == "en") t.nameEn else t.nameRu,
                    startTime = t.startTime.epochSecond,
                    endTime = t.endTime.epochSecond,
                    fish = fishName,
                    fishRarity = fishRarity,
                    location = t.location?.let { I18n.location(it, language) },
                    metric = t.metric.lowercase(),
                    prizePlaces = t.prizePlaces,
                )
            }
            call.respond(list)
        }

        get("/api/tournaments/past") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val list = tournaments.pastTournaments().map { t ->
                val fishRarity = t.fish?.let { f -> if (f in rarityGroups) f else fishing.fishRarity(f) }
                val fishName = t.fish?.takeUnless { it in rarityGroups }?.let { I18n.fish(it, language) }
                TournamentDTO(
                    id = t.id,
                    name = if (language == "en") t.nameEn else t.nameRu,
                    startTime = t.startTime.epochSecond,
                    endTime = t.endTime.epochSecond,
                    fish = fishName,
                    fishRarity = fishRarity,
                    location = t.location?.let { I18n.location(it, language) },
                    metric = t.metric.lowercase(),
                    prizePlaces = t.prizePlaces,
                )
            }
            call.respond(list)
        }

        get("/api/prizes") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val pending = prizeService.pendingPrizes(uid).map {
                PrizeDTO(
                    it.id,
                    it.packageId,
                    it.qty,
                    it.rank,
                    it.coins,
                    it.source.name.lowercase(),
                )
            }
            call.respond(pending)
        }

        post("/api/prizes/{id}/claim") {
            val id = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val (lures, current) = try {
                prizeService.claimPrize(uid, id, fishing)
            } catch (_: Exception) { return@post call.respond(HttpStatusCode.BadRequest) }
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val lures2 = lures.map {
                it.copy(
                    displayName = I18n.lure(it.name, language),
                    description = I18n.lureDescription(it.name, language),
                )
            }
            call.respond(ShopBuyResp(lures2, current))
        }

        get("/api/club") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val details = clubs.clubDetails(uid) ?: return@get call.respond(HttpStatusCode.NoContent)
            call.respond(details.toDto())
        }

        get("/api/club/chat") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val messages = try {
                clubs.clubChat(uid)
            } catch (e: ClubService.ClubException) {
                val status = when (e.code) {
                    "not_in_club" -> HttpStatusCode.Conflict
                    else -> HttpStatusCode.BadRequest
                }
                return@get call.respond(status, mapOf("error" to e.code))
            }
            call.respond(messages.map { it.toDto() })
        }

        get("/api/club/search") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val query = call.request.queryParameters["q"]
            val list = clubs.searchClubs(uid, query).map {
                ClubSummaryDTO(
                    it.id,
                    it.name,
                    it.memberCount,
                    it.capacity,
                    it.info,
                    it.minJoinWeightKg,
                    it.recruitingOpen,
                )
            }
            call.respond(list)
        }

        post("/api/club/create") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            @Serializable data class ClubCreateReq(val name: String)
            val req = try { call.receive<ClubCreateReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val details = try {
                clubs.createClub(uid, req.name)
            } catch (e: ClubService.ClubException) {
                val status = when {
                    e.code == "already_in_club" -> HttpStatusCode.Conflict
                    e.code.startsWith("weight_required") -> HttpStatusCode.Conflict
                    e.code == "not_enough_coins" -> HttpStatusCode.Conflict
                    e.code == "name_empty" || e.code == "name_too_long" || e.code == "name_profanity" ->
                        HttpStatusCode.BadRequest
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(details.toDto())
        }

        post("/api/club/{id}/join") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val clubId = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val details = try {
                clubs.joinClub(uid, clubId)
            } catch (e: ClubService.ClubException) {
                val status = when {
                    e.code == "already_in_club" -> HttpStatusCode.Conflict
                    e.code == "club_full" -> HttpStatusCode.Conflict
                    e.code == "recruitment_closed" -> HttpStatusCode.Conflict
                    e.code.startsWith("weight_required") -> HttpStatusCode.Conflict
                    e.code == "not_found" -> HttpStatusCode.NotFound
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(details.toDto())
        }

        post("/api/club/info") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            @Serializable data class ClubInfoReq(val info: String)
            val req = try { call.receive<ClubInfoReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val details = try {
                clubs.updateClubInfo(uid, req.info)
            } catch (e: ClubService.ClubException) {
                val status = when (e.code) {
                    "not_in_club" -> HttpStatusCode.Conflict
                    "forbidden" -> HttpStatusCode.Forbidden
                    "info_too_long" -> HttpStatusCode.BadRequest
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(details.toDto())
        }

        post("/api/club/settings") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            @Serializable
            data class ClubSettingsReq(
                val minJoinWeightKg: Double,
                val recruitingOpen: Boolean,
            )
            val req = try { call.receive<ClubSettingsReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val details = try {
                clubs.updateClubSettings(uid, req.minJoinWeightKg, req.recruitingOpen)
            } catch (e: ClubService.ClubException) {
                val status = when (e.code) {
                    "not_in_club" -> HttpStatusCode.Conflict
                    "forbidden" -> HttpStatusCode.Forbidden
                    "invalid_min_weight" -> HttpStatusCode.BadRequest
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(details.toDto())
        }

        post("/api/club/leave") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            try {
                clubs.leaveClub(uid)
            } catch (e: ClubService.ClubException) {
                val status = when (e.code) {
                    "not_in_club" -> HttpStatusCode.Conflict
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(HttpStatusCode.OK)
        }

        post("/api/club/members/{id}/promote") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val targetId = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val details = try {
                clubs.promoteMember(uid, targetId)
            } catch (e: ClubService.ClubException) {
                val status = when (e.code) {
                    "forbidden" -> HttpStatusCode.Forbidden
                    "not_in_club", "member_not_found" -> HttpStatusCode.NotFound
                    "invalid_role", "invalid_target" -> HttpStatusCode.BadRequest
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(details.toDto())
        }

        post("/api/club/members/{id}/demote") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val targetId = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val details = try {
                clubs.demoteMember(uid, targetId)
            } catch (e: ClubService.ClubException) {
                val status = when (e.code) {
                    "forbidden" -> HttpStatusCode.Forbidden
                    "not_in_club", "member_not_found" -> HttpStatusCode.NotFound
                    "invalid_role", "invalid_target" -> HttpStatusCode.BadRequest
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(details.toDto())
        }

        post("/api/club/members/{id}/kick") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val targetId = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val details = try {
                clubs.kickMember(uid, targetId)
            } catch (e: ClubService.ClubException) {
                val status = when (e.code) {
                    "forbidden" -> HttpStatusCode.Forbidden
                    "not_in_club", "member_not_found" -> HttpStatusCode.NotFound
                    "invalid_target" -> HttpStatusCode.BadRequest
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(details.toDto())
        }

        post("/api/club/members/{id}/appoint-president") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val targetId = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val details = try {
                clubs.appointPresident(uid, targetId)
            } catch (e: ClubService.ClubException) {
                val status = when (e.code) {
                    "forbidden" -> HttpStatusCode.Forbidden
                    "not_in_club", "member_not_found" -> HttpStatusCode.NotFound
                    "invalid_role", "invalid_target" -> HttpStatusCode.BadRequest
                    else -> HttpStatusCode.BadRequest
                }
                return@post call.respond(status, mapOf("error" to e.code))
            }
            call.respond(details.toDto())
        }

        // Daily baits
        post("/api/daily") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val res = fishing.giveDailyBaits(uid)
                ?: return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "already claimed"))

            @Serializable
            data class DailyResp(val lures: List<LureDTO>, val currentLureId: Long?, val dailyStreak: Int)

            val lures = res.first.map {
                it.copy(
                    displayName = I18n.lure(it.name, language),
                    description = I18n.lureDescription(it.name, language),
                )
            }
            call.respond(DailyResp(lures, res.second, res.third))
        }

        post("/api/create-invoice") {
            val req = try { call.receive<InvoiceReq>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val tgUser = try { TgWebAppAuth.verifyAndExtractUser(req.initData, env.botToken) }
                catch (_: Exception) { return@post call.respond(HttpStatusCode.Unauthorized) }
            val uid = fishing.ensureUserByTgId(tgUser.id)
            val language = fishing.userLanguage(uid)
            val packInfo = fishing.findPack(req.productId)
            if (packInfo?.rodCode != null && fishing.hasRod(uid, packInfo.rodCode)) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "rod_unlocked"))
            }
            val url = try { stars.createInvoiceLink(tgUser.id, req.productId, language) }
                catch (_: Exception) { return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad package")) }
            Metrics.counter("create_invoice_total", mapOf("pack" to req.productId))
            call.respond(InvoiceResp(url))
        }

        // Shop
        get("/api/shop") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val autoUntil = transaction { Users.select { Users.id eq uid }.single()[Users.autoFishUntil] }
            val lockedRodCodes = fishing.listRods(uid).filterNot { it.unlocked }.map { it.code }.toSet()
            val items = fishing.listShop(language)
                .map { cat ->
                    val packs = cat.packs.filter { it.rodCode == null || it.rodCode in lockedRodCodes }
                    ShopCategoryDTO(
                        cat.id,
                        cat.name,
                        packs.map { p ->
                            val until = if (p.id == "autofish") {
                                autoUntil?.takeIf { it.isAfter(Instant.now()) }?.toString()
                            } else null
                            ShopPackageDTO(
                                id = p.id,
                                name = p.name,
                                desc = p.desc,
                                price = p.price,
                                until = until,
                                originalPrice = p.originalPrice,
                                discountStart = p.discountStart?.toString(),
                                discountEnd = p.discountEnd?.toString(),
                                coinPrice = p.coinPrice,
                                rodCode = p.rodCode,
                            )
                        }
                    )
                }
                .filter { it.packs.isNotEmpty() }
            call.respond(items)
        }

        post("/api/shop/{id}") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            log.info("shop purchase click userId={} pack={}", uid, id)
            Metrics.counter(
                "shop_purchase_click_total",
                mapOf("pack" to id, "currency" to "stars"),
            )
            val packInfo = fishing.findPack(id)
            if (packInfo?.rodCode != null && fishing.hasRod(uid, packInfo.rodCode)) {
                Metrics.counter(
                    "shop_purchase_denied_total",
                    mapOf("pack" to id, "currency" to "stars", "reason" to "rod_unlocked"),
                )
                return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "rod_unlocked"))
            }
            val paymentReq = if (!env.devMode) {
                try { call.receive<PaymentReq>() } catch (_: Exception) {
                    Metrics.counter(
                        "shop_purchase_denied_total",
                        mapOf("pack" to id, "currency" to "stars"),
                    )
                    return@post call.respond(HttpStatusCode.PaymentRequired)
                }
            } else try { call.receive<PaymentReq>() } catch (_: Exception) { null }
            val res = try { fishing.buyPackage(uid, id) } catch (e: Exception) {
                Metrics.counter(
                    "shop_purchase_failed_total",
                    mapOf("pack" to id, "currency" to "stars"),
                )
                log.warn("shop purchase failed userId={} pack={} err={}", uid, id, e.message)
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad package"))
            }
            paymentReq?.let {
                PayService.recordPayment(
                    uid,
                    id,
                    PayService.PaymentInfo(
                        providerChargeId = it.providerChargeId,
                        telegramChargeId = it.telegramChargeId,
                        amount = it.amount,
                        currency = it.currency,
                    )
                )
            }
            fishing.findPack(id)?.let { pack ->
                ReferralService.onPurchase(uid, pack)
            }
            Metrics.counter(
                "shop_purchase_complete_total",
                mapOf("pack" to id, "currency" to "stars"),
            )
            log.info("shop purchase success userId={} pack={}", uid, id)
            call.respond(ShopBuyResp(res.first, res.second))
        }

        post("/api/shop/{id}/coins") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            log.info("coin shop purchase userId={} pack={}", uid, id)
            Metrics.counter(
                "coin_shop_purchase_click_total",
                mapOf("pack" to id, "currency" to "coins"),
            )
            val language = fishing.userLanguage(uid)
            val result = try {
                fishing.buyPackageWithCoins(uid, id)
            } catch (e: FishingService.NotEnoughCoinsException) {
                Metrics.counter(
                    "coin_shop_purchase_failed_total",
                    mapOf("pack" to id, "currency" to "coins", "reason" to "insufficient")
                )
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "not_enough_coins",
                        "balance" to e.balance,
                        "required" to e.required,
                    )
                )
            } catch (_: FishingService.CoinPurchaseUnavailableException) {
                Metrics.counter(
                    "coin_shop_purchase_failed_total",
                    mapOf("pack" to id, "currency" to "coins", "reason" to "unavailable")
                )
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "coin_purchase_unavailable")
                )
            } catch (e: Exception) {
                Metrics.counter(
                    "coin_shop_purchase_failed_total",
                    mapOf("pack" to id, "currency" to "coins", "reason" to "error")
                )
                log.warn("coin shop purchase failed userId={} pack={} err={}", uid, id, e.message)
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad package"))
            }
            val localized = result.first.map {
                it.copy(
                    displayName = I18n.lure(it.name, language),
                    description = I18n.lureDescription(it.name, language),
                )
            }
            Metrics.counter(
                "coin_shop_purchase_complete_total",
                mapOf("pack" to id, "currency" to "coins"),
            )
            log.info("coin shop purchase success userId={} pack={}", uid, id)
            call.respond(ShopBuyResp(localized, result.second))
        }

        get("/api/referrals") {
            Metrics.counter("referrals_get_total")
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val token = ReferralService.currentLink(uid) ?: ReferralService.generateLink(uid)
            val invited = ReferralService.invited(uid).mapNotNull { fishing.displayName(it) }
            val link = "https://t.me/${env.botName}?startapp=$token"
            @Serializable
            data class ReferralsResp(val token: String, val invited: List<String>, val link: String)
            call.respond(ReferralsResp(token, invited, link))
        }

        post("/api/referrals") {
            Metrics.counter("referrals_post_total")
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val token = ReferralService.generateLink(uid)
            val link = "https://t.me/${env.botName}?startapp=$token"
            @Serializable
            data class ReferralLinkResp(val token: String, val link: String)
            call.respond(ReferralLinkResp(token, link))
        }

        get("/api/referrals/rewards") {
            Metrics.counter("referral_rewards_get_total")
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val shopItems = fishing.listShop(language).flatMap { it.packs }
            val rewards = ReferralService.pendingRewardsSimple(uid).map {
                val name = shopItems.find { p -> p.id == it.packageId }?.name
                    ?: if (it.packageId == "autofish_week") {
                        if (language == "en") "Auto Catch (week)" else "Автоловля (неделя)"
                    } else I18n.lure(it.packageId, language)
                ReferralRewardDTO(it.packageId, it.qty, name)
            }
            call.respond(rewards)
        }

        post("/api/referrals/rewards/claim") {
            Metrics.counter("referral_rewards_claim_total")
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val (lures, current) = ReferralService.claimAllRewards(uid, fishing)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val lures2 = lures.map {
                it.copy(
                    displayName = I18n.lure(it.name, language),
                    description = I18n.lureDescription(it.name, language),
                )
            }
            call.respond(ShopBuyResp(lures2, current))
        }

        post("/api/autofish/disable") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            fishing.disableAutoFish(uid)
            call.respond(HttpStatusCode.NoContent)
        }

        // Guide data
        get("/api/guide") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val data = fishing.guide(language)
            call.respond(data)
        }

        // Change location
        post("/api/location/{id}") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            try {
                fishing.setLocation(uid, id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to (e.message ?: "locked")))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "bad location")))
            }
        }

        // Start cast: consume lure and validate
        post("/api/start-cast") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            try {
                val result = fishing.startCast(uid)
                call.respond(result)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "bad lure")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to (e.message ?: "rate limit")))
            }
        }

        // Hook result: determine if fish can be caught
        post("/api/hook") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val req = call.receive<HookReq>()
            val res = try {
                fishing.hook(uid, req.wait, req.reaction, applyBeginnerProtection = false)
            } catch (e: Exception) {
                log.warn(
                    "hook failed userId={} wait={} reaction={} err={}",
                    uid,
                    req.wait,
                    req.reaction,
                    e.message
                )
                return@post call.respond(
                    HttpStatusCode.TooManyRequests,
                    mapOf("error" to (e.message ?: "rate limit"))
                )
            }
            Metrics.counter(
                "hook_total",
                mapOf("result" to if (res.success) "caught" else "escaped")
            )
            call.respond(res)
        }

        // Cast result / finalize catch
        post("/api/cast") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val req = call.receive<CastReq>()
            val res = try {
                fishing.cast(
                    uid,
                    req.wait,
                    req.reaction,
                    req.success,
                    applyBeginnerProtection = false,
                )
            } catch (e: Exception) {
                log.warn(
                    "cast failed userId={} wait={} reaction={} err={}",
                    uid,
                    req.wait,
                    req.reaction,
                    e.message
                )
                return@post call.respond(
                    HttpStatusCode.TooManyRequests,
                    mapOf("error" to (e.message ?: "rate limit"))
                )
            }
            log.info(
                "cast userId={} wait={} reaction={} caught={} fish={} weight={} location={} rarity={}",
                uid,
                req.wait,
                req.reaction,
                res.caught,
                res.catch?.fish,
                res.catch?.weight,
                res.catch?.location,
                res.catch?.rarity
            )
            Metrics.counter("cast_total", mapOf("caught" to res.caught.toString()))
            val localizedCatch = res.catch?.let { c ->
                Metrics.counter(
                    "fish_caught_total",
                    mapOf(
                        "fish" to c.fish,
                        "location" to c.location,
                        "rarity" to c.rarity
                    )
                )
                Metrics.gauge(
                    "catch_weight_kg",
                    c.weight,
                    mapOf(
                        "fish" to c.fish,
                        "location" to c.location,
                        "rarity" to c.rarity
                    )
                )
                c.copy(
                    fish = I18n.fish(c.fish, language),
                    location = I18n.location(c.location, language)
                )
            }
            val localizedUnlocked = if (res.unlockedLocations.isEmpty()) {
                emptyList()
            } else {
                res.unlockedLocations.map { I18n.location(it, language) }
            }
            val localizedRods = if (res.unlockedRods.isEmpty()) {
                emptyList()
            } else {
                res.unlockedRods.map { I18n.rod(it, language) }
            }
            val localizedQuestUpdates = if (res.questUpdates.isEmpty()) {
                emptyList()
            } else {
                QuestService.localizeUpdates(res.questUpdates, language)
            }
            call.respond(
                res.copy(
                    catch = localizedCatch,
                    unlockedLocations = localizedUnlocked,
                    unlockedRods = localizedRods,
                    questUpdates = localizedQuestUpdates,
                )
            )
        }

        post("/api/catches/{id}/send") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val tgId = fishing.userTgId(uid)
                ?: return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "telegram_only"))
            val catchId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            val catch = fishing.catchById(uid, catchId)
                ?: return@post call.respond(HttpStatusCode.NotFound)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val fishName = I18n.fish(catch.fish, language)
            val locationName = I18n.location(catch.location, language)
            val caughtAt = catch.at?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
            val captionBase = buildCatchCaption(
                lang = language,
                fishName = fishName,
                rarity = catch.rarity,
                weightKg = catch.weight,
                locationName = locationName,
            )
            val caption = appendCatchTags(captionBase, catch)
            val image = generateCatchImage(
                catch.fish,
                catch.location,
                fishName,
                locationName,
                catch.weight,
                catch.rarity,
                language,
                anglerName = catch.user,
                caughtAt = caughtAt,
            )
            try {
                if (image != null) {
                    bot.sendPhoto(tgId, image, caption)
                } else {
                    bot.sendMessage(tgId, caption)
                }
            } catch (e: Exception) {
                log.error("send catch card failed uid={} catchId={}", uid, catchId, e)
                return@post call.respond(HttpStatusCode.BadGateway, mapOf("error" to "send_failed"))
            }
            Metrics.counter("catch_share_total", mapOf("result" to "ok"))
            call.respond(HttpStatusCode.Accepted)
        }

        // Change lure
        post("/api/lure/{id}") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            try {
                fishing.setLure(uid, id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "bad lure")))
            }
        }

        // Change rod
        post("/api/rod/{id}") {
            val uid = call.requireUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            try {
                fishing.setRod(uid, id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "bad rod")))
            }
        }

        // Ratings - personal
        get("/api/ratings/personal/location/{id}") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val idParam = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val locationId = if (idParam.equals("all", ignoreCase = true)) {
                null
            } else {
                idParam.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            }
            val period = call.request.queryParameters["period"] ?: "all"
            val asc = call.request.queryParameters["order"] == "asc"
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val res = fishing.personalTopByLocation(uid, locationId, period, asc).map { c ->
                c.copy(
                    fish = I18n.fish(c.fish, language),
                    location = I18n.location(c.location, language)
                )
            }
            call.respond(res)
        }

        get("/api/ratings/personal/species/{id}") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val idParam = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val fishId = if (idParam.equals("all", ignoreCase = true)) {
                null
            } else {
                idParam.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            }
            val period = call.request.queryParameters["period"] ?: "all"
            val asc = call.request.queryParameters["order"] == "asc"
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val res = fishing.personalTopBySpecies(uid, fishId, period, asc).map { c ->
                c.copy(
                    fish = I18n.fish(c.fish, language),
                    location = I18n.location(c.location, language)
                )
            }
            call.respond(res)
        }

        // Ratings - global
        get("/api/ratings/global/location/{id}") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val idParam = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val locationId = if (idParam.equals("all", ignoreCase = true)) {
                null
            } else {
                idParam.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            }
            val period = call.request.queryParameters["period"] ?: "all"
            val asc = call.request.queryParameters["order"] == "asc"
            val res = fishing.globalTopByLocation(locationId, period, asc).map { c ->
                c.copy(
                    fish = I18n.fish(c.fish, language),
                    location = I18n.location(c.location, language)
                )
            }
            call.respond(res)
        }

        get("/api/ratings/global/species/{id}") {
            val uid = call.requireUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val idParam = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val fishId = if (idParam.equals("all", ignoreCase = true)) {
                null
            } else {
                idParam.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            }
            val period = call.request.queryParameters["period"] ?: "all"
            val asc = call.request.queryParameters["order"] == "asc"
            val res = fishing.globalTopBySpecies(fishId, period, asc).map { c ->
                c.copy(
                    fish = I18n.fish(c.fish, language),
                    location = I18n.location(c.location, language)
                )
            }
            call.respond(res)
        }
    }
}
