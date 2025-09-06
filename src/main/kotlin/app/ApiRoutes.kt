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
import service.FishingService
import service.LocationDTO
import service.RecentDTO
import service.FishingService.LureDTO
import db.Users

fun Application.apiRoutes(env: Env) {
    val fishing = FishingService()

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
            val uid = fishing.ensureUserByTgId(tgId)
            if (!env.devMode) return@post call.respond(HttpStatusCode.PaymentRequired)
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val res = try { fishing.buyPackage(uid, id) } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad package"))
            }
            call.respond(ShopBuyResp(res.first, res.second))
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
                return@post call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to (e.message ?: "rate limit")))
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
    }
}
