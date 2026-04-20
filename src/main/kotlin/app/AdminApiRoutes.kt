package app

import db.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import service.FishingService
import service.Tournament
import service.TournamentService
import java.time.Instant
import java.time.LocalDate

private val broadcastScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun Application.adminApiRoutes(env: Env) {
    val tournaments = TournamentService()
    val fishing = FishingService()
    val bot = TelegramBot(env.botToken)

    routing {
        route("/api/admin") {
            // Authentication Interceptor
            intercept(ApplicationCallPipeline.Call) {
                val authHeader = call.request.headers[HttpHeaders.Authorization]
                val token = authHeader?.removePrefix("Bearer ")?.trim()
                if (env.adminApiToken.isBlank() || token != env.adminApiToken) {
                    call.respond(HttpStatusCode.Unauthorized)
                    finish()
                }
            }

            @Serializable
            data class TournamentReq(
                val nameRu: String,
                val nameEn: String,
                val startTime: Long, // Epoch millis
                val endTime: Long,
                val fish: String?,
                val location: String?,
                val metric: String,
                val prizePlaces: Int,
                val prizesJson: String
            )

            // Tournaments
            get("/tournaments") {
                call.respond(tournaments.listTournaments())
            }

            post("/tournaments") {
                val req = call.receive<TournamentReq>()
                val id = tournaments.createTournament(
                    nameRu = req.nameRu,
                    nameEn = req.nameEn,
                    start = Instant.ofEpochMilli(req.startTime),
                    end = Instant.ofEpochMilli(req.endTime),
                    fish = req.fish,
                    location = req.location,
                    metric = req.metric,
                    prizePlaces = req.prizePlaces,
                    prizes = req.prizesJson
                )
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            put("/tournaments/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<TournamentReq>()
                tournaments.updateTournament(
                    id = id,
                    nameRu = req.nameRu,
                    nameEn = req.nameEn,
                    start = Instant.ofEpochMilli(req.startTime),
                    end = Instant.ofEpochMilli(req.endTime),
                    fish = req.fish,
                    location = req.location,
                    metric = req.metric,
                    prizePlaces = req.prizePlaces,
                    prizes = req.prizesJson
                )
                call.respond(HttpStatusCode.OK)
            }

            delete("/tournaments/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                tournaments.deleteTournament(id)
                call.respond(HttpStatusCode.NoContent)
            }

            @Serializable
            data class DiscountReq(
                val packageId: String,
                val price: Int,
                val start: String, // ISO date string e.g. "2026-04-20"
                val end: String
            )

            // Discounts
            get("/discounts") {
                val discounts = fishing.listDiscounts()
                call.respond(discounts)
            }

            post("/discounts") {
                val req = call.receive<DiscountReq>()
                val start = LocalDate.parse(req.start)
                val end = LocalDate.parse(req.end)
                fishing.setDiscount(req.packageId, req.price, start, end)
                call.respond(HttpStatusCode.OK)
            }

            delete("/discounts/{packageId}") {
                val packageId = call.parameters["packageId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                fishing.removeDiscount(packageId)
                call.respond(HttpStatusCode.NoContent)
            }

            @Serializable
            data class BroadcastReq(
                val textRu: String,
                val textEn: String
            )

            // Broadcast
            post("/broadcast") {
                val req = call.receive<BroadcastReq>()
                val users = transaction {
                    Users.select { Users.tgId.isNotNull() }.map { 
                        it[Users.tgId]!! to (it[Users.language] ?: "en")
                    }
                }
                
                broadcastScope.launch {
                    users.forEach { (tgId, lang) ->
                        val msg = if (lang == "ru" && req.textRu.isNotBlank()) req.textRu else req.textEn
                        if (msg.isNotBlank()) {
                            try {
                                bot.sendMessage(tgId, msg)
                            } catch (e: Exception) {
                                // Ignore errors for blocked users
                            }
                        }
                    }
                }
                call.respond(HttpStatusCode.Accepted, mapOf("status" to "Broadcast started", "count" to users.size))
            }
        }
    }
}
