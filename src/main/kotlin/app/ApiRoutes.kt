package app

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import service.FishingService
import service.LocationDTO
import service.RecentDTO
import service.FishingService.LureDTO
import service.FishingService.CatchDTO
import service.FishingService.FishExtremeDTO
import db.Users
import util.Metrics

fun Application.apiRoutes(env: Env) {
    val fishing = FishingService()
    val log = LoggerFactory.getLogger("Api")

    intercept(ApplicationCallPipeline.Monitoring) {
        val session = call.sessions.get<AppSession>()
        val tgId = session?.tgId
        val params = call.parameters.entries().associate { it.key to it.value.joinToString(",") }
        log.info(
            "call {} {} tgId={} params={}",
            call.request.httpMethod.value,
            call.request.uri,
            tgId,
            params
        )
        Metrics.counter(
            "api_call_total",
            mapOf(
                "path" to call.request.uri.substringBefore('?'),
                "method" to call.request.httpMethod.value
            )
        )
        proceed()
    }

    @Serializable
    data class CastReq(val wait: Int, val reaction: Double)

    @Serializable
    data class ShopPackageDTO(val id: String, val name: String, val desc: String, val price: Int)

    @Serializable
    data class ShopCategoryDTO(val id: String, val name: String, val packs: List<ShopPackageDTO>)

    @Serializable
    data class ShopBuyResp(val lures: List<LureDTO>, val currentLureId: Long?)

    routing {
        // Telegram WebApp auth: client posts initData
        post("/api/auth/telegram") {
            val initData = call.receiveText()
            val tgId = try { TgWebAppAuth.verifyAndExtractTgId(initData, env.botToken) }
            catch (_: Exception) { return@post call.respond(HttpStatusCode.Unauthorized, "bad initData") }
            call.sessions.set(AppSession(tgId))
            fishing.ensureUserByTgId(tgId)
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
            val lures = fishing.listLures(uid)
            val totalWeight = fishing.totalCaughtKg(uid)
            val todayWeight = fishing.todayCaughtKg(uid)
            val locs = fishing.locations(uid)
            val dailyAvailable = fishing.canClaimDaily(uid)
            val storedLoc = transaction {
                Users.selectAll().where { Users.id eq uid }.single()[Users.currentLocationId]?.value
            }
            val currentLocId = storedLoc?.takeIf { id -> locs.any { it.id == id && it.unlocked } }
                ?: locs.first { it.unlocked }.id
            val currentLureId = transaction {
                Users.select { Users.id eq uid }.single()[Users.currentLureId]?.value
            }
            val recent = fishing.recent(uid)

            @Serializable
            data class MeResp(
                val username: String,
                val lures: List<LureDTO>,
                val currentLureId: Long?,
                val totalWeight: Double,
                val todayWeight: Double,
                val locationId: Long,
                val locations: List<LocationDTO>,
                val recent: List<RecentDTO>,
                val dailyAvailable: Boolean,
            )
            call.respond(
                MeResp(
                    "angler",
                    lures,
                    currentLureId,
                    totalWeight,
                    todayWeight,
                    currentLocId,
                    locs,
                    recent,
                    dailyAvailable
                )
            )
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
            val res = fishing.giveDailyBaits(uid)
                ?: return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "already claimed"))

            @Serializable
            data class DailyResp(val lures: List<LureDTO>, val currentLureId: Long?)

            call.respond(DailyResp(res.first, res.second))
        }

        // Shop
        get("/api/shop") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            fishing.ensureUserByTgId(tgId)
            val items = fishing.listShop().map { cat ->
                ShopCategoryDTO(
                    cat.id,
                    cat.name,
                    cat.packs.map { ShopPackageDTO(it.id, it.name, it.desc, it.price) }
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
            if (!env.devMode) {
                Metrics.counter("shop_purchase_denied_total", mapOf("pack" to id))
                return@post call.respond(HttpStatusCode.PaymentRequired)
            }
            val res = try { fishing.buyPackage(uid, id) } catch (e: Exception) {
                Metrics.counter("shop_purchase_failed_total", mapOf("pack" to id))
                log.warn("shop purchase failed tgId={} pack={} err={}", tgId, id, e.message)
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad package"))
            }
            Metrics.counter("shop_purchase_complete_total", mapOf("pack" to id))
            log.info("shop purchase success tgId={} pack={}", tgId, id)
            call.respond(ShopBuyResp(res.first, res.second))
        }

        // Guide data
        get("/api/guide") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            fishing.ensureUserByTgId(tgId)
            val data = fishing.guide()
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

        // Cast
        post("/api/cast") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@post call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val req = call.receive<CastReq>()
            val res = try { fishing.cast(uid, req.wait, req.reaction) } catch (e: Exception) {
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
            res.catch?.let { c ->
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
            }
            call.respond(res)
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

        // Achievements - personal
        get("/api/achievements/personal/location/{id}") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val res: List<CatchDTO> = fishing.personalTopByLocation(uid, id)
            call.respond(res)
        }

        get("/api/achievements/personal/fish") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val res: List<CatchDTO> = fishing.personalTopByFish(uid)
            call.respond(res)
        }

        get("/api/achievements/personal/fish/{id}") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val uid = fishing.ensureUserByTgId(tgId)
            val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val res: FishExtremeDTO = fishing.personalFishExtremes(uid, id)
            call.respond(res)
        }

        // Achievements - global
        get("/api/achievements/global/location/{id}") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            fishing.ensureUserByTgId(tgId)
            val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val res: List<CatchDTO> = fishing.globalTopByLocation(id)
            call.respond(res)
        }

        get("/api/achievements/global/fish") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            fishing.ensureUserByTgId(tgId)
            val res: List<CatchDTO> = fishing.globalTopByFish()
            call.respond(res)
        }

        get("/api/achievements/global/fish/{id}") {
            val session = call.sessions.get<AppSession>()
            val tgId = when {
                session != null -> session.tgId
                env.devMode     -> 1L
                else            -> return@get call.respond(HttpStatusCode.Unauthorized)
            }
            fishing.ensureUserByTgId(tgId)
            val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val res: FishExtremeDTO = fishing.globalFishExtremes(id)
            call.respond(res)
        }
    }
}
