package app

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
import service.I18n
import service.PrizeSpec
import service.ReferralService
import db.Users
import service.PayService
import service.StarsPaymentService
import util.Metrics
import java.time.Instant

fun Application.apiRoutes(env: Env) {
    val fishing = FishingService()
    val log = LoggerFactory.getLogger("Api")
    val stars = StarsPaymentService(env, fishing)
    val tournaments = TournamentService()
    val bot = TelegramBot(env.botToken)
    val rarityGroups = setOf("common", "uncommon", "rare", "epic", "mythic", "legendary")

    // Use the Plugins phase so that sessions are already available
    // when logging each incoming request. Intercepting earlier can
    // cause SessionNotYetConfiguredException for routes like `/app`
    // that are served before the Sessions plugin runs.
    intercept(ApplicationCallPipeline.Plugins) {
        val path = call.request.path()
        if (path.startsWith("/app")) {
            val qsTgId = call.request.queryParameters["tgId"]?.toLongOrNull()
            val sessionTgId = call.sessions.get<AppSession>()?.tgId
            if (qsTgId != null && sessionTgId != null && qsTgId != sessionTgId) {
                call.sessions.clear<AppSession>()
            }
        }
        if (path != "/metrics") {
            val session = call.sessions.get<AppSession>()
            val tgId = session?.tgId
            val params = call.parameters.entries().associate { it.key to it.value.joinToString(",") }
            val body = if (call.request.httpMethod in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
                try { call.receiveText() } catch (_: Exception) { "" }
            } else ""
            log.info(
                "call {} {} tgId={} params={} body={}",
                call.request.httpMethod.value,
                call.request.uri,
                tgId,
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
    data class PrizeDTO(val id: Long, val packageId: String, val qty: Int, val rank: Int)

    @Serializable
    data class PrizeSpecDTO(val packageId: String, val qty: Int)

    @Serializable
    data class ReferralRewardDTO(val packageId: String, val qty: Int, val name: String)

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

    @Serializable
    data class CurrentTournamentDTO(
        val tournament: TournamentDTO,
        val leaderboard: List<LeaderboardEntryDTO>,
        val mine: LeaderboardEntryDTO? = null,
    )

    routing {
        // Telegram WebApp auth: client sends initData in a header
        post("/api/auth/telegram") {
            val initData = call.request.headers["Telegram-Init-Data"] ?: call.receiveText()
            if (initData.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, "missing initData")
            val tgUser = try { TgWebAppAuth.verifyAndExtractUser(initData, env.botToken) }
            catch (_: Exception) { return@post call.respond(HttpStatusCode.Unauthorized, "bad initData") }
            call.sessions.set(AppSession(tgUser.id))
            val ref = call.request.queryParameters["ref"]
            fishing.ensureUserByTgId(
                tgUser.id,
                tgUser.firstName,
                tgUser.lastName,
                tgUser.username,
                tgUser.languageCode,
                ref,
            )
            call.respond(HttpStatusCode.OK)
        }

        // Profile
        get("/api/me") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            @Serializable data class NickReq(val nickname: String)
            val req = call.receive<NickReq>()
            fishing.setNickname(uid, req.nickname)
            call.respond(HttpStatusCode.OK)
        }

        post("/api/language") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            @Serializable data class LangReq(val language: String)
            val req = call.receive<LangReq>()
            fishing.setLanguage(uid, req.language)
            call.respond(HttpStatusCode.OK)
        }

        get("/api/tournament/current") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
                        prize = prizes.getOrNull(it.rank - 1)?.let { p -> PrizeSpecDTO(p.pack, p.qty) },
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
                        prize = prizes.getOrNull(it.rank - 1)?.let { p -> PrizeSpecDTO(p.pack, p.qty) },
                    )
                },
            )
            call.respond(resp)
        }

        get("/api/tournament/{id}") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
                        prize = prizes.getOrNull(it.rank - 1)?.let { p -> PrizeSpecDTO(p.pack, p.qty) },
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
                        prize = prizes.getOrNull(it.rank - 1)?.let { p -> PrizeSpecDTO(p.pack, p.qty) },
                    )
                },
            )
            call.respond(resp)
        }

        get("/api/tournaments/upcoming") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            tournaments.distributePrizes()
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val prizes = tournaments.pendingPrizes(uid).map { PrizeDTO(it.id, it.packageId, it.qty, it.rank) }
            call.respond(prizes)
        }

        post("/api/prizes/{id}/claim") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val id = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val uid = fishing.ensureUserByTgId(tgId)
            val (lures, current) = try {
                tournaments.claimPrize(uid, id, fishing)
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

        // Daily baits
        post("/api/daily") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val url = try { stars.createInvoiceLink(tgUser.id, req.productId, language) }
                catch (_: Exception) { return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad package")) }
            Metrics.counter("create_invoice_total", mapOf("pack" to req.productId))
            call.respond(InvoiceResp(url))
        }

        // Shop
        get("/api/shop") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val autoUntil = transaction { Users.select { Users.id eq uid }.single()[Users.autoFishUntil] }
            val items = fishing.listShop(language).map { cat ->
                ShopCategoryDTO(
                    cat.id,
                    cat.name,
                    cat.packs.map { p ->
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
                        )
                    }
                )
            }
            call.respond(items)
        }

        post("/api/shop/{id}") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            log.info("shop purchase click tgId={} pack={}", tgId, id)
            Metrics.counter("shop_purchase_click_total", mapOf("pack" to id))
            val uid = fishing.ensureUserByTgId(tgId)
            val paymentReq = if (!env.devMode) {
                try { call.receive<PaymentReq>() } catch (_: Exception) {
                    Metrics.counter("shop_purchase_denied_total", mapOf("pack" to id))
                    return@post call.respond(HttpStatusCode.PaymentRequired)
                }
            } else try { call.receive<PaymentReq>() } catch (_: Exception) { null }
            val res = try { fishing.buyPackage(uid, id) } catch (e: Exception) {
                Metrics.counter("shop_purchase_failed_total", mapOf("pack" to id))
                log.warn("shop purchase failed tgId={} pack={} err={}", tgId, id, e.message)
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
            Metrics.counter("shop_purchase_complete_total", mapOf("pack" to id))
            log.info("shop purchase success tgId={} pack={}", tgId, id)
            call.respond(ShopBuyResp(res.first, res.second))
        }

        post("/api/shop/{id}/coins") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            log.info("coin shop purchase tgId={} pack={}", tgId, id)
            Metrics.counter("coin_shop_purchase_click_total", mapOf("pack" to id))
            val uid = fishing.ensureUserByTgId(tgId)
            val language = fishing.userLanguage(uid)
            val result = try {
                fishing.buyPackageWithCoins(uid, id)
            } catch (e: FishingService.NotEnoughCoinsException) {
                Metrics.counter(
                    "coin_shop_purchase_failed_total",
                    mapOf("pack" to id, "reason" to "insufficient")
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
                    mapOf("pack" to id, "reason" to "unavailable")
                )
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "coin_purchase_unavailable")
                )
            } catch (e: Exception) {
                Metrics.counter(
                    "coin_shop_purchase_failed_total",
                    mapOf("pack" to id, "reason" to "error")
                )
                log.warn("coin shop purchase failed tgId={} pack={} err={}", tgId, id, e.message)
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad package"))
            }
            val localized = result.first.map {
                it.copy(
                    displayName = I18n.lure(it.name, language),
                    description = I18n.lureDescription(it.name, language),
                )
            }
            Metrics.counter("coin_shop_purchase_complete_total", mapOf("pack" to id))
            log.info("coin shop purchase success tgId={} pack={}", tgId, id)
            call.respond(ShopBuyResp(localized, result.second))
        }

        get("/api/referrals") {
            Metrics.counter("referrals_get_total")
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val token = ReferralService.currentLink(uid) ?: ReferralService.generateLink(uid)
            val invited = ReferralService.invited(uid).mapNotNull { fishing.displayName(it) }
            val link = "https://t.me/${env.botName}?startapp=$token"
            @Serializable
            data class ReferralsResp(val token: String, val invited: List<String>, val link: String)
            call.respond(ReferralsResp(token, invited, link))
        }

        post("/api/referrals") {
            Metrics.counter("referrals_post_total")
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val token = ReferralService.generateLink(uid)
            val link = "https://t.me/${env.botName}?startapp=$token"
            @Serializable
            data class ReferralLinkResp(val token: String, val link: String)
            call.respond(ReferralLinkResp(token, link))
        }

        get("/api/referrals/rewards") {
            Metrics.counter("referral_rewards_get_total")
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            fishing.disableAutoFish(uid)
            call.respond(HttpStatusCode.NoContent)
        }

        // Guide data
        get("/api/guide") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val language = transaction { Users.select { Users.id eq uid }.single()[Users.language] }
            val data = fishing.guide(language)
            call.respond(data)
        }

        // Change location
        post("/api/location/{id}") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            try {
                val newLure = fishing.startCast(uid)
                call.respond(mapOf("currentLureId" to newLure))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "bad lure")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to (e.message ?: "rate limit")))
            }
        }

        // Hook result: determine if fish can be caught
        post("/api/hook") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val req = call.receive<HookReq>()
            val res = try {
                fishing.hook(uid, req.wait, req.reaction, applyBeginnerProtection = false)
            } catch (e: Exception) {
                log.warn(
                    "hook failed tgId={} wait={} reaction={} err={}",
                    tgId,
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
                    "cast failed tgId={} wait={} reaction={} err={}",
                    tgId,
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
                "cast tgId={} wait={} reaction={} caught={} fish={} weight={} location={} rarity={}",
                tgId,
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
            call.respond(
                res.copy(
                    catch = localizedCatch,
                    unlockedLocations = localizedUnlocked,
                    unlockedRods = localizedRods,
                )
            )
        }

        post("/api/catches/{id}/send") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
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
