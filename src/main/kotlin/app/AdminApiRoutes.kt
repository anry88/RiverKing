package app

import db.Fish
import db.Locations
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
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import service.COIN_PRIZE_ID
import service.FishingService
import service.TournamentService
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val broadcastScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val adminDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun parseAdminDate(value: String): LocalDate {
    val trimmed = value.trim()
    return runCatching { LocalDate.parse(trimmed) }
        .getOrElse { LocalDate.parse(trimmed, adminDateFormatter) }
}

// ── Serializable DTOs for API responses ──

@Serializable
data class TournamentReq(
    val nameRu: String,
    val nameEn: String,
    val startTime: Long,
    val endTime: Long,
    val fish: String? = null,
    val location: String? = null,
    val metric: String,
    val prizePlaces: Int,
    val prizesJson: String
)

@Serializable
data class TournamentResp(
    val id: Long,
    val nameRu: String,
    val nameEn: String,
    val startTime: Long,
    val endTime: Long,
    val fish: String? = null,
    val location: String? = null,
    val metric: String,
    val prizePlaces: Int,
    val prizesJson: String
)

@Serializable
data class DiscountReq(
    val packageId: String,
    val price: Int,
    val start: String,
    val end: String
)

@Serializable
data class DiscountResp(
    val packageId: String,
    val price: Int,
    val startDate: String,
    val endDate: String
)

@Serializable
data class BroadcastReq(
    val textRu: String,
    val textEn: String
)

@Serializable
data class BroadcastResp(
    val status: String,
    val count: Int
)

@Serializable
data class AdminCatalogOptionResp(
    val id: String,
    val label: String
)

@Serializable
data class AdminPrizeOptionResp(
    val id: String,
    val label: String,
    val defaultQty: Int,
    val coins: Boolean = false
)

@Serializable
data class AdminDiscountPackageResp(
    val id: String,
    val name: String,
    val category: String,
    val basePrice: Int,
    val currentPrice: Int,
    val activeDiscountPrice: Int? = null,
    val activeDiscountStart: String? = null,
    val activeDiscountEnd: String? = null
)

@Serializable
data class AdminCatalogResp(
    val metrics: List<AdminCatalogOptionResp>,
    val fish: List<AdminCatalogOptionResp>,
    val locations: List<AdminCatalogOptionResp>,
    val tournamentPrizes: List<AdminPrizeOptionResp>,
    val discountPackages: List<AdminDiscountPackageResp>
)

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

            // ── Tournaments ──

            get("/tournaments") {
                val offset = call.request.queryParameters["offset"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50)
                val list = tournaments.listTournaments(
                    limit = limit,
                    offset = offset,
                    newestFirst = true
                ).map { t ->
                    TournamentResp(
                        id = t.id,
                        nameRu = t.nameRu,
                        nameEn = t.nameEn,
                        startTime = t.startTime.toEpochMilli(),
                        endTime = t.endTime.toEpochMilli(),
                        fish = t.fish,
                        location = t.location,
                        metric = t.metric,
                        prizePlaces = t.prizePlaces,
                        prizesJson = t.prizesJson
                    )
                }
                call.respond(list)
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

            // ── Discounts ──

            get("/discounts") {
                val list = fishing.listDiscounts().map { d ->
                    DiscountResp(
                        packageId = d.packageId,
                        price = d.price,
                        startDate = d.startDate.toString(),
                        endDate = d.endDate.toString()
                    )
                }
                call.respond(list)
            }

            post("/discounts") {
                val req = call.receive<DiscountReq>()
                val start = parseAdminDate(req.start)
                val end = parseAdminDate(req.end)
                fishing.setDiscount(req.packageId, req.price, start, end)
                call.respond(HttpStatusCode.OK)
            }

            delete("/discounts/{packageId}") {
                val packageId = call.parameters["packageId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                fishing.removeDiscount(packageId)
                call.respond(HttpStatusCode.NoContent)
            }

            // ── Catalogs for admin mobile forms ──

            get("/catalog") {
                val metrics = listOf(
                    AdminCatalogOptionResp("largest", "Самая крупная рыба"),
                    AdminCatalogOptionResp("smallest", "Самая мелкая рыба"),
                    AdminCatalogOptionResp("count", "Количество улова"),
                    AdminCatalogOptionResp("total_weight", "Общий вес"),
                )
                val rarityOptions = listOf(
                    AdminCatalogOptionResp("common", "Редкость: common"),
                    AdminCatalogOptionResp("uncommon", "Редкость: uncommon"),
                    AdminCatalogOptionResp("rare", "Редкость: rare"),
                    AdminCatalogOptionResp("epic", "Редкость: epic"),
                    AdminCatalogOptionResp("mythic", "Редкость: mythic"),
                    AdminCatalogOptionResp("legendary", "Редкость: legendary"),
                )
                val fishOptions = transaction {
                    Fish.selectAll()
                        .orderBy(Fish.name, SortOrder.ASC)
                        .map { row ->
                            val name = row[Fish.name]
                            AdminCatalogOptionResp(name, "$name (${row[Fish.rarity]})")
                        }
                }
                val locationOptions = transaction {
                    Locations.selectAll()
                        .orderBy(Locations.unlockKg, SortOrder.ASC)
                        .map { row ->
                            val name = row[Locations.name]
                            AdminCatalogOptionResp(name, name)
                        }
                }
                val shop = fishing.listShop("ru")
                val activeDiscounts = fishing.listDiscounts().associateBy { it.packageId }
                val discountPackages = shop.flatMap { category ->
                    category.packs.map { pack ->
                        val basePrice = fishing.findPack(pack.id)?.price ?: pack.originalPrice ?: pack.price
                        val activeDiscount = activeDiscounts[pack.id]
                        AdminDiscountPackageResp(
                            id = pack.id,
                            name = pack.name,
                            category = category.name,
                            basePrice = basePrice,
                            currentPrice = pack.price,
                            activeDiscountPrice = activeDiscount?.price,
                            activeDiscountStart = activeDiscount?.startDate?.format(adminDateFormatter),
                            activeDiscountEnd = activeDiscount?.endDate?.format(adminDateFormatter),
                        )
                    }
                }
                val baitPrizeOptions = shop.flatMap { category ->
                    category.packs
                        .filter { it.rodCode == null && it.items.isNotEmpty() }
                        .map { pack -> AdminPrizeOptionResp(pack.id, pack.name, defaultQty = 1) }
                }
                val autofishMonth = fishing.findPack("autofish")?.let {
                    AdminPrizeOptionResp(it.id, "${it.name} (месяц)", defaultQty = 1)
                } ?: AdminPrizeOptionResp("autofish", "Автоловля (месяц)", defaultQty = 1)
                val prizeOptions = listOf(
                    AdminPrizeOptionResp(COIN_PRIZE_ID, "Монеты", defaultQty = 1000, coins = true),
                ) + baitPrizeOptions + listOf(
                    autofishMonth,
                    AdminPrizeOptionResp("autofish_week", "Автоловля (неделя)", defaultQty = 1),
                )
                call.respond(
                    AdminCatalogResp(
                        metrics = metrics,
                        fish = rarityOptions + fishOptions,
                        locations = locationOptions,
                        tournamentPrizes = prizeOptions,
                        discountPackages = discountPackages,
                    )
                )
            }

            // ── Broadcast ──

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
                call.respond(HttpStatusCode.Accepted, BroadcastResp(status = "Broadcast started", count = users.size))
            }
        }
    }
}
