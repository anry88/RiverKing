package app

import db.Fish
import db.Locations
import db.SpecialEvents
import db.Users
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import service.CastZoneCodec
import service.CastZoneDTO
import service.COIN_PRIZE_ID
import service.FishingService
import service.SpecialEvent
import service.SpecialEventFishSpec
import service.SpecialEventPrizeConfig
import service.SpecialEventService
import service.TournamentService
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val broadcastScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val adminDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun parseAdminDate(value: String): LocalDate {
    val trimmed = value.trim()
    return runCatching { LocalDate.parse(trimmed) }
        .getOrElse { LocalDate.parse(trimmed, adminDateFormatter) }
}

private val adminLocationBackgrounds = mapOf(
    "Пруд" to "/app/assets/backgrounds/pond.png",
    "Болото" to "/app/assets/backgrounds/swamp.png",
    "Река" to "/app/assets/backgrounds/river.png",
    "Озеро" to "/app/assets/backgrounds/lake.png",
    "Водохранилище" to "/app/assets/backgrounds/reservoir.png",
    "Горная река" to "/app/assets/backgrounds/mountain_river.png",
    "Дельта реки" to "/app/assets/backgrounds/river_delta.png",
    "Прибрежье моря" to "/app/assets/backgrounds/sea_coast.png",
    "Русло Амазонки" to "/app/assets/backgrounds/amazon_riverbed.png",
    "Игапо, затопленный лес" to "/app/assets/backgrounds/flooded_forest.png",
    "Мангровые заросли" to "/app/assets/backgrounds/mangroves.png",
    "Коралловые отмели" to "/app/assets/backgrounds/coral_flats.png",
    "Фьорд" to "/app/assets/backgrounds/fjord.png",
    "Открытый океан" to "/app/assets/backgrounds/open_ocean.png",
)

private fun adminEventAssetPath(imagePath: String?): String? =
    imagePath?.takeIf { it.isNotBlank() }?.let { "/event-assets/$it" }

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
    val eventFish: List<AdminCatalogOptionResp> = emptyList(),
    val locations: List<AdminCatalogOptionResp>,
    val tournamentPrizes: List<AdminPrizeOptionResp>,
    val discountPackages: List<AdminDiscountPackageResp>
)

@Serializable
data class AdminEventFishReq(
    val fishId: Long,
    val weight: Double
)

@Serializable
data class AdminEventPrizeReq(
    val prizePlaces: Int,
    val prizesJson: String
)

@Serializable
data class AdminSpecialEventReq(
    val nameRu: String,
    val nameEn: String,
    val startTime: Long,
    val endTime: Long,
    val imagePath: String? = null,
    val castZone: CastZoneDTO? = null,
    val fish: List<AdminEventFishReq>,
    val weightPrizes: AdminEventPrizeReq,
    val countPrizes: AdminEventPrizeReq,
    val fishPrizes: AdminEventPrizeReq
)

@Serializable
data class AdminSpecialEventResp(
    val id: Long,
    val nameRu: String,
    val nameEn: String,
    val startTime: Long,
    val endTime: Long,
    val imagePath: String? = null,
    val castZone: CastZoneDTO? = null,
    val fish: List<AdminEventFishReq>,
    val weightPrizes: AdminEventPrizeReq,
    val countPrizes: AdminEventPrizeReq,
    val fishPrizes: AdminEventPrizeReq
)

@Serializable
data class AdminImageUploadResp(val imagePath: String)

@Serializable
data class AdminCastZoneLocationResp(
    val id: Long,
    val name: String,
    val kind: String,
    val eventId: Long? = null,
    val imageUrl: String? = null,
    val castZone: CastZoneDTO? = null
)

@Serializable
data class AdminCastZoneUpdateReq(
    val castZone: CastZoneDTO? = null
)

fun Application.adminApiRoutes(env: Env) {
    val tournaments = TournamentService()
    val fishing = FishingService()
    val events = SpecialEventService()
    val bot = TelegramBot(env.botToken)

    fun AdminSpecialEventReq.fishSpecs(): List<SpecialEventFishSpec> =
        fish.map { SpecialEventFishSpec(it.fishId, it.weight) }

    fun AdminEventPrizeReq.toConfig(): SpecialEventPrizeConfig =
        SpecialEventPrizeConfig(prizePlaces, prizesJson)

    fun SpecialEvent.toAdminResp(): AdminSpecialEventResp =
        AdminSpecialEventResp(
            id = id,
            nameRu = nameRu,
            nameEn = nameEn,
            startTime = startTime.toEpochMilli(),
            endTime = endTime.toEpochMilli(),
            imagePath = imagePath,
            castZone = castZone,
            fish = events.fishSpecs(id).map { AdminEventFishReq(it.fishId, it.weight) },
            weightPrizes = AdminEventPrizeReq(weightPrizePlaces, weightPrizesJson),
            countPrizes = AdminEventPrizeReq(countPrizePlaces, countPrizesJson),
            fishPrizes = AdminEventPrizeReq(fishPrizePlaces, fishPrizesJson),
        )

    suspend fun ApplicationCall.respondEventError(e: SpecialEventService.SpecialEventException) {
        val status = if (e.code == "event_overlap") HttpStatusCode.Conflict else HttpStatusCode.BadRequest
        respond(status, mapOf("error" to e.code))
    }

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

            // ── Special Events ──

            get("/events") {
                val offset = call.request.queryParameters["offset"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50)
                call.respond(events.listEvents(limit = limit, offset = offset).map { it.toAdminResp() })
            }

            post("/events") {
                val req = call.receive<AdminSpecialEventReq>()
                try {
                    val id = events.createEvent(
                        nameRu = req.nameRu,
                        nameEn = req.nameEn,
                        start = Instant.ofEpochMilli(req.startTime),
                        end = Instant.ofEpochMilli(req.endTime),
                        imagePath = req.imagePath,
                        castZone = req.castZone,
                        fish = req.fishSpecs(),
                        weightPrizes = req.weightPrizes.toConfig(),
                        countPrizes = req.countPrizes.toConfig(),
                        fishPrizes = req.fishPrizes.toConfig(),
                    )
                    call.respond(HttpStatusCode.Created, mapOf("id" to id))
                } catch (e: SpecialEventService.SpecialEventException) {
                    call.respondEventError(e)
                }
            }

            put("/events/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<AdminSpecialEventReq>()
                try {
                    events.updateEvent(
                        id = id,
                        nameRu = req.nameRu,
                        nameEn = req.nameEn,
                        start = Instant.ofEpochMilli(req.startTime),
                        end = Instant.ofEpochMilli(req.endTime),
                        imagePath = req.imagePath,
                        castZone = req.castZone,
                        fish = req.fishSpecs(),
                        weightPrizes = req.weightPrizes.toConfig(),
                        countPrizes = req.countPrizes.toConfig(),
                        fishPrizes = req.fishPrizes.toConfig(),
                    )
                    call.respond(HttpStatusCode.OK)
                } catch (e: SpecialEventService.SpecialEventException) {
                    call.respondEventError(e)
                }
            }

            delete("/events/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                events.deleteEvent(id)
                call.respond(HttpStatusCode.NoContent)
            }

            post("/events/image") {
                val root = File(env.eventAssetsDir).canonicalFile
                withContext(Dispatchers.IO) { root.mkdirs() }
                var savedName: String? = null
                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    try {
                        if (part is PartData.FileItem && savedName == null) {
                            val original = part.originalFileName.orEmpty()
                            val ext = original.substringAfterLast('.', "")
                                .lowercase()
                                .takeIf { it in setOf("png", "jpg", "jpeg", "webp") }
                                ?.let { ".$it" }
                                ?: ".webp"
                            val fileName = "event-${UUID.randomUUID()}$ext"
                            val target = File(root, fileName).canonicalFile
                            if (!target.path.startsWith(root.path)) {
                                return@forEachPart
                            }
                            withContext(Dispatchers.IO) {
                                part.streamProvider().use { input ->
                                    target.outputStream().use { output -> input.copyTo(output) }
                                }
                            }
                            savedName = fileName
                        }
                    } finally {
                        part.dispose()
                    }
                }
                val imagePath = savedName ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "file_required"))
                call.respond(AdminImageUploadResp(imagePath))
            }

            // ── Cast zones ──

            get("/cast-zones") {
                val locations = transaction {
                    val eventImages = SpecialEvents
                        .slice(SpecialEvents.id, SpecialEvents.imagePath)
                        .selectAll()
                        .associate { row -> row[SpecialEvents.id].value to row[SpecialEvents.imagePath] }

                    val regular = Locations
                        .select { Locations.specialEventId.isNull() }
                        .orderBy(Locations.unlockKg, SortOrder.ASC)
                        .map { row ->
                            val name = row[Locations.name]
                            AdminCastZoneLocationResp(
                                id = row[Locations.id].value,
                                name = name,
                                kind = "regular",
                                imageUrl = adminLocationBackgrounds[name],
                                castZone = CastZoneCodec.decode(row[Locations.castZoneJson]),
                            )
                        }

                    val event = Locations
                        .select { Locations.specialEventId.isNotNull() }
                        .orderBy(Locations.id, SortOrder.DESC)
                        .map { row ->
                            val eventId = row[Locations.specialEventId]
                            AdminCastZoneLocationResp(
                                id = row[Locations.id].value,
                                name = row[Locations.name],
                                kind = "event",
                                eventId = eventId,
                                imageUrl = adminEventAssetPath(eventId?.let(eventImages::get)),
                                castZone = CastZoneCodec.decode(row[Locations.castZoneJson]),
                            )
                        }

                    regular + event
                }
                call.respond(locations)
            }

            put("/locations/{id}/cast-zone") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad_location"))
                val req = call.receive<AdminCastZoneUpdateReq>()
                val encoded = try {
                    CastZoneCodec.encode(req.castZone)
                } catch (_: IllegalArgumentException) {
                    return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_cast_zone"))
                }
                val updated = transaction {
                    Locations.update({ Locations.id eq id }) {
                        it[castZoneJson] = encoded
                    }
                }
                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "not_found"))
                } else {
                    call.respond(HttpStatusCode.OK)
                }
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
                val eventFishOptions = transaction {
                    Fish.selectAll()
                        .orderBy(Fish.name, SortOrder.ASC)
                        .map { row ->
                            val id = row[Fish.id].value
                            val name = row[Fish.name]
                            AdminCatalogOptionResp(id.toString(), "$name (${row[Fish.rarity]})")
                        }
                }
                val locationOptions = transaction {
                    Locations.select { Locations.specialEventId.isNull() }
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
                        eventFish = eventFishOptions,
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
