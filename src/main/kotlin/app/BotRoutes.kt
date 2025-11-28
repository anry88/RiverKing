package app

import app.RARITY_LABELS
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import service.FishingService
import service.PayService
import service.StarsPaymentService
import service.ReferralService
import service.TournamentService
import service.RatingPrizeService
import service.PrizeService
import service.UserPrize
import service.PrizeSpec
import service.I18n
import service.COIN_PRIZE_ID
import util.Metrics
import util.sanitizeName
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import org.slf4j.LoggerFactory
import db.Users
import db.Lures
import db.Locations
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.slice
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

internal fun parseInvoicePayload(payload: String, userId: Long): String? {
    return if ('=' in payload) {
        val parts = payload.split(';').mapNotNull {
            val p = it.split('=', limit = 2)
            if (p.size == 2) p[0] to p[1] else null
        }.toMap()
        val packId = parts["pack"]
        val payloadUser = parts["user"]?.toLongOrNull()
        if (packId != null && payloadUser == userId) packId else null
    } else if (payload.startsWith("pack_")) {
        payload.removePrefix("pack_")
    } else {
        null
    }
}

private data class AdminDraft(
    var id: Long? = null,
    var step: AdminStep = AdminStep.NAME_RU,
    var nameRu: String = "",
    var nameEn: String = "",
    var start: Instant? = null,
    var end: Instant? = null,
    var fish: String? = null,
    var location: String? = null,
    var metric: String = "",
    var prizePlaces: Int = 0,
    var prizes: MutableList<PrizeSpec> = mutableListOf(),
    var currentPrize: Int = 1,
    var awaitingPrizeQuantity: Boolean = false,
    var pendingPrize: PrizeSpec? = null,
)

private enum class AdminStep { NAME_RU, NAME_EN, START, END, FISH, LOCATION, METRIC, PRIZE_PLACES, PRIZE }

private data class DiscountDraft(
    var step: DiscountStep = DiscountStep.PACK,
    var packageId: String = "",
    var packageName: String = "",
    var basePrice: Int = 0,
    var price: Int? = null,
    var start: LocalDate? = null,
    var end: LocalDate? = null,
)

private enum class DiscountStep { PACK, PRICE, START, END }

private data class BroadcastDraft(
    var step: BroadcastStep = BroadcastStep.TEXT_RU,
    var textRu: String = "",
    var textEn: String = "",
)

private enum class BroadcastStep { TEXT_RU, TEXT_EN }

private val METRIC_OPTIONS = listOf("largest", "smallest", "count", "total_weight")
private const val METRIC_KEYBOARD = """{"keyboard":[["largest","smallest"],["count","total_weight"]],"one_time_keyboard":true,"resize_keyboard":true}"""
private const val REMOVE_KEYBOARD = """{"remove_keyboard":true}"""

private val BAIT_ORDER = listOf(
    "Пресная мирная",
    "Пресная хищная",
    "Морская мирная",
    "Морская хищная",
    "Пресная мирная+",
    "Пресная хищная+",
    "Морская мирная+",
    "Морская хищная+",
)
private val BAIT_ORDER_INDEX = BAIT_ORDER.withIndex().associate { it.value to it.index }
private const val TELEGRAM_MESSAGE_LENGTH_LIMIT = 4000

private val broadcastScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val castScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
private val autoCastScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

private fun parsePrizes(str: String): MutableList<PrizeSpec> {
    return try {
        Json.parseToJsonElement(str).jsonArray.map { el ->
            val obj = el.jsonObject
            val pack = obj["pack"]?.jsonPrimitive?.content?.trim().orEmpty()
            val qty = obj["qty"]?.jsonPrimitive?.int ?: 1
            val coins = obj["coins"]?.jsonPrimitive?.content?.toIntOrNull()
            when {
                coins != null && (pack.isBlank() || pack == COIN_PRIZE_ID) -> PrizeSpec(
                    pack = COIN_PRIZE_ID,
                    qty = coins,
                    coins = coins
                )
                pack == COIN_PRIZE_ID -> PrizeSpec(
                    pack = COIN_PRIZE_ID,
                    qty = qty,
                    coins = coins ?: qty
                )
                else -> PrizeSpec(pack = pack, qty = qty, coins = coins)
            }
        }.toMutableList()
    } catch (_: Exception) {
        str.split(Regex("""[,\n]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { s ->
                val parts = s.split(Regex("""[:\s]+""")).filter { it.isNotBlank() }
                val packRaw = parts.getOrNull(0) ?: s
                val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
                val normalized = if (packRaw.equals(COIN_PRIZE_ID, ignoreCase = true)) COIN_PRIZE_ID else packRaw
                if (normalized == COIN_PRIZE_ID) {
                    PrizeSpec(COIN_PRIZE_ID, qty, qty)
                } else {
                    PrizeSpec(normalized, qty)
                }
            }
            .toMutableList()
    }
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun Application.botRoutes(env: Env) {
    val bot = TelegramBot(env.botToken)
    val fishing = FishingService()
    val stars = StarsPaymentService(env, fishing)
    val tournaments = TournamentService()
    val ratingPrizes = RatingPrizeService()
    val prizeService = PrizeService(tournaments, ratingPrizes)
    val adminStates = mutableMapOf<Long, AdminDraft>()
    val discountStates = mutableMapOf<Long, DiscountDraft>()
    val broadcastStates = mutableMapOf<Long, BroadcastDraft>()
    data class AutoCastState(
        var currentCast: Job? = null,
        var loopJob: Job = Job(),
        val stopRequested: AtomicBoolean = AtomicBoolean(false),
    )
    val autoCastJobs = mutableMapOf<Long, AutoCastState>()
    val log = LoggerFactory.getLogger("Bot")
    routing {
        post("/bot") {
            val secret = call.request.headers["X-Telegram-Bot-Api-Secret-Token"]
            if (secret == null || secret != env.telegramWebhookSecret) {
                return@post call.respond(HttpStatusCode.Forbidden)
            }
            val update = try { call.receive<TgUpdate>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.OK)
            }

            update.inlineQuery?.let { iq ->
                val from = iq.from
                val uid = fishing.ensureUserByTgId(
                    tgId = from.id,
                    firstName = from.first_name,
                    lastName = from.last_name,
                    username = from.username,
                    language = from.language_code
                )
                val lang = fishing.userLanguage(uid)
                data class InlineCommandInfo(
                    val name: String,
                    val ruDescription: String,
                    val enDescription: String,
                    val assetName: String,
                    val messageText: (lang: String, uid: Long) -> String
                )

                val inlineCommands = listOf(
                    InlineCommandInfo(
                        name = "start",
                        ruDescription = "Стартовать бота и получить список команд",
                        enDescription = "Start the bot and get the command list",
                        assetName = "start.png"
                    ) { _, _ -> "/start" },
                    InlineCommandInfo(
                        name = "startapp",
                        ruDescription = "Открыть игру",
                        enDescription = "Open the game",
                        assetName = "startapp.png"
                    ) { _, _ -> "/startapp" },
                    InlineCommandInfo(
                        name = "cast",
                        ruDescription = "Забросить снасть",
                        enDescription = "Cast your line",
                        assetName = "cast.png"
                    ) { _, _ -> "/cast" },
                    InlineCommandInfo(
                        name = "autocast",
                        ruDescription = "Запустить автоловлю",
                        enDescription = "Start auto casting",
                        assetName = "autocast.png"
                    ) { _, _ -> "/autocast" },
                    InlineCommandInfo(
                        name = "stop_autocast",
                        ruDescription = "Остановить автоловлю",
                        enDescription = "Stop auto casting",
                        assetName = "stop_autocast.png"
                    ) { _, _ -> "/stop_autocast" },
                    InlineCommandInfo(
                        name = "bait",
                        ruDescription = "Сменить приманку",
                        enDescription = "Change your bait",
                        assetName = "bait.png"
                    ) { _, _ -> "/bait" },
                    InlineCommandInfo(
                        name = "rod",
                        ruDescription = "Сменить удочку",
                        enDescription = "Change your rod",
                        assetName = "rod.png"
                    ) { _, _ -> "/rod" },
                    InlineCommandInfo(
                        name = "location",
                        ruDescription = "Сменить локацию",
                        enDescription = "Change your location",
                        assetName = "location.png"
                    ) { _, _ -> "/location" },
                    InlineCommandInfo(
                        name = "daily",
                        ruDescription = "Получить ежедневную награду",
                        enDescription = "Claim your daily reward",
                        assetName = "daily.png"
                    ) { _, _ -> "/daily" },
                    InlineCommandInfo(
                        name = "prizes",
                        ruDescription = "Забрать призы турнира",
                        enDescription = "Claim tournament prizes",
                        assetName = "prizes.png"
                    ) { _, _ -> "/prizes" },
                    InlineCommandInfo(
                        name = "shop",
                        ruDescription = "Купить приманки и удочки за звёзды",
                        enDescription = "Buy baits and rods with Stars",
                        assetName = "shop.png"
                    ) { _, _ -> "/shop" },
                    InlineCommandInfo(
                        name = "coin_shop",
                        ruDescription = "Купить наборы за монеты",
                        enDescription = "Buy bundles with coins",
                        assetName = "coin_shop.png"
                    ) { _, _ -> "/coin_shop" },
                    InlineCommandInfo(
                        name = "tournament",
                        ruDescription = "Таблица текущего турнира и твоя позиция",
                        enDescription = "View the current tournament leaderboard and your rank",
                        assetName = "tournament.png"
                    ) { _, _ -> "/tournament" },
                    InlineCommandInfo(
                        name = "daily_rating",
                        ruDescription = "Твои места в сегодняшнем ежедневном рейтинге",
                        enDescription = "View your positions in today's daily rating",
                        assetName = "daily_ratings.png"
                    ) { _, _ -> "/daily_rating" },
                    InlineCommandInfo(
                        name = "stats",
                        ruDescription = "Статистика по пойманной рыбе",
                        enDescription = "Your fishing stats",
                        assetName = "stats.png"
                    ) { _, _ -> "/stats" },
                    InlineCommandInfo(
                        name = "language",
                        ruDescription = "Выбрать язык",
                        enDescription = "Choose your language",
                        assetName = "language.png"
                    ) { currentLang, currentUid ->
                        val nickname = fishing.displayName(currentUid)
                        val fallback = if (currentLang == "ru") "не задан" else "not set"
                        val label = if (currentLang == "ru") {
                            "Текущий ник: ${nickname ?: fallback}"
                        } else {
                            "Current nickname: ${nickname ?: fallback}"
                        }
                        "/language\n$label"
                    },
                    InlineCommandInfo(
                        name = "nickname",
                        ruDescription = "Сменить ник",
                        enDescription = "Change your nickname",
                        assetName = "nickname.png"
                    ) { _, _ -> "/nickname" }
                )

                val query = iq.query.trim()
                val normalized = query.lowercase().removePrefix("/")
                val assetsBaseUrl = env.publicBaseUrl.trimEnd('/') + "/app/assets/inline_commands"
                val matched = inlineCommands.filter { normalized.isEmpty() || it.name.startsWith(normalized) }
                    .ifEmpty { inlineCommands }
                val results = matched.map { info ->
                    val messageText = info.messageText(lang, uid)
                    InlineQueryResultArticle(
                        id = info.name,
                        title = info.name,
                        description = if (lang == "ru") info.ruDescription else info.enDescription,
                        inputMessageContent = InputTextMessageContent(messageText),
                        thumbUrl = "$assetsBaseUrl/${info.assetName}"
                    )
                }

                try {
                    bot.answerInlineQuery(iq.id, results)
                } catch (e: Exception) {
                    log.error("answerInlineQuery failed id={}", iq.id, e)
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            update.preCheckoutQuery?.let { q ->
                try {
                    bot.answerPreCheckoutQuery(q.id)
                } catch (e: Exception) {
                    log.error("answerPreCheckoutQuery failed id={}", q.id, e)
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            fun logCommandMetric(name: String, params: Map<String, String> = emptyMap(), source: String) {
                Metrics.counter("bot_command_total", mapOf("command" to name, "source" to source) + params)
            }

            fun ensureUserId(user: TgUser?): Long? {
                return user?.let {
                    fishing.ensureUserByTgId(
                        tgId = it.id,
                        firstName = it.first_name,
                        lastName = it.last_name,
                        username = it.username,
                        language = it.language_code
                    )
                }
            }

            fun splitMessage(text: String, limit: Int = TELEGRAM_MESSAGE_LENGTH_LIMIT): List<String> {
                if (text.length <= limit) return listOf(text)
                val chunks = mutableListOf<String>()
                var index = 0
                while (index < text.length) {
                    var end = (index + limit).coerceAtMost(text.length)
                    if (end < text.length) {
                        val newlineIndex = text.lastIndexOf('\n', end - 1)
                        if (newlineIndex >= index) {
                            end = newlineIndex + 1
                        }
                        while (
                            end < text.length &&
                                end > index &&
                                text[end - 1].isHighSurrogate() &&
                                text[end].isLowSurrogate()
                        ) {
                            end--
                        }
                        if (end == index) {
                            end = (index + limit).coerceAtMost(text.length)
                            if (end == index && end < text.length) {
                                end++
                            }
                        }
                    }
                    chunks += text.substring(index, end)
                    index = end
                }
                return chunks
            }

            fun strikethrough(text: String): String {
                if (text.isBlank()) return text
                val combining = '\u0336'
                val builder = StringBuilder(text.length * 2)
                text.forEach { ch ->
                    builder.append(ch).append(combining)
                }
                return builder.toString()
            }

            suspend fun trySend(
                chatId: Long,
                text: String,
                replyMarkup: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val parts = splitMessage(text)
                val lastIndex = parts.lastIndex
                parts.forEachIndexed { idx, part ->
                    val markup = if (idx == lastIndex) replyMarkup else null
                    val replyTo = if (idx == 0) replyToMessageId else null
                    try {
                        bot.sendMessage(chatId, part, markup, replyTo)
                    } catch (e: Exception) {
                        log.error("sendMessage failed chatId={} part={}/{}", chatId, idx + 1, parts.size, e)
                    }
                }
            }

            fun ownedData(ownerUid: Long, payload: Any): String = "$ownerUid:${payload.toString()}"

            data class CastFlowOptions(
                val startNotification: String? = null,
                val catchFooter: String? = null,
            )

            data class CastAttemptResult(
                val started: Boolean,
                val failureReason: String? = null,
                val completion: Job? = null,
            )

            fun hasAutoSubscription(uid: Long): Boolean = transaction {
                Users.select { Users.id eq uid }.single()[Users.autoFishUntil]?.isAfter(Instant.now()) == true
            }

            fun currentSetup(uid: Long): Pair<String?, String?> = transaction {
                val userRow = Users.select { Users.id eq uid }.single()
                val lureName = userRow[Users.currentLureId]?.value?.let { id ->
                    Lures.select { Lures.id eq id }.singleOrNull()?.get(Lures.name)
                }
                val locationName = userRow[Users.currentLocationId]?.value?.let { id ->
                    Locations.select { Locations.id eq id }.singleOrNull()?.get(Locations.name)
                }
                lureName to locationName
            }

            suspend fun performCastSequence(
                uid: Long,
                chatId: Long,
                replyTo: Long?,
                source: String,
                lang: String,
                knownFish: MutableSet<Long>,
                options: CastFlowOptions = CastFlowOptions(),
            ): CastAttemptResult {
                val startRes = try {
                    fishing.startCast(uid)
                } catch (e: Exception) {
                    val reason = e.message ?: "error"
                    val text = when (reason) {
                        "casting" -> if (lang == "ru") {
                            "Заброс уже выполняется. Дождись окончания текущей попытки."
                        } else {
                            "You are already casting. Wait for the current attempt to finish."
                        }
                        "No lure selected" -> if (lang == "ru") {
                            "Сначала выбери приманку через /bait и попробуй снова."
                        } else {
                            "Select a bait with /bait first and try again."
                        }
                        "No baits" -> if (lang == "ru") {
                            "Приманки закончились. Забери ежедневную награду через /daily или купи новые в /shop."
                        } else {
                            "You have no baits left. Claim the daily reward with /daily or buy more in /shop."
                        }
                        "No suitable fish" -> if (lang == "ru") {
                            "На выбранной локации нет подходящей рыбы для этой приманки. Сменить локацию можно через /location, а приманку — через /bait."
                        } else {
                            "No suitable fish at this location for the selected bait. Switch location with /location or change bait with /bait."
                        }
                        "locked" -> if (lang == "ru") {
                            "Локация заблокирована. Сменить локацию можно через /location или продолжай ловить, чтобы открыть новую."
                        } else {
                            "This location is locked. Change your location with /location or keep fishing to unlock a new one."
                        }
                        else -> if (lang == "ru") {
                            "Не удалось забросить снасть. Попробуй ещё раз позже."
                        } else {
                            "Failed to start the cast. Please try again later."
                        }
                    }
                    logCommandMetric("cast", mapOf("result" to "error", "stage" to "start", "reason" to reason), source)
                    trySend(chatId, text, replyToMessageId = replyTo)
                    return CastAttemptResult(started = false, failureReason = reason)
                }
                if (startRes.lureChanged) {
                    val lureName = startRes.newLureName?.let { I18n.lure(it, lang) }
                    val baseLine = if (lang == "ru") {
                        if (lureName != null) {
                            "🎣 Приманка закончилась, переключились на «$lureName»."
                        } else {
                            "🎣 Приманка закончилась."
                        }
                    } else {
                        if (lureName != null) {
                            "🎣 Bait ran out, switched to \"$lureName\"."
                        } else {
                            "🎣 Bait ran out."
                        }
                    }
                    val (extraLine, button) = when {
                        startRes.recommendedRodName != null && startRes.recommendedRodUnlocked == true &&
                            startRes.recommendedRodId != null -> {
                            val rodName = I18n.rod(startRes.recommendedRodName, lang)
                            val promptLine = if (lang == "ru") {
                                "Эта приманка лучше всего работает с удочкой «$rodName». Сменить удочку?"
                            } else {
                                "This bait works best with the \"$rodName\" rod. Switch rods?"
                            }
                            val buttonLabel = if (lang == "ru") {
                                "Сменить на «$rodName»"
                            } else {
                                "Use \"$rodName\""
                            }
                            val btn = InlineKeyboardButton(
                                buttonLabel,
                                "/rod ${ownedData(uid, startRes.recommendedRodId)}"
                            )
                            Pair(promptLine, btn)
                        }
                        startRes.recommendedRodName != null && startRes.recommendedRodUnlocked == false &&
                            startRes.recommendedRodPackId != null && startRes.recommendedRodPriceStars != null -> {
                            val rodName = I18n.rod(startRes.recommendedRodName, lang)
                            val priceText = "${startRes.recommendedRodPriceStars}⭐"
                            val infoLine = if (lang == "ru") {
                                "Удочка «$rodName», подходящая для этой приманки, ещё не разблокирована. Можно открыть за $priceText."
                            } else {
                                "The \"$rodName\" rod for this bait is still locked. You can unlock it for $priceText."
                            }
                            val buttonLabel = if (lang == "ru") {
                                "🔓 Разблокировать «$rodName» — $priceText"
                            } else {
                                "🔓 Unlock \"$rodName\" — $priceText"
                            }
                            val btn = InlineKeyboardButton(
                                buttonLabel,
                                "/buy ${ownedData(uid, startRes.recommendedRodPackId)}"
                            )
                            Pair(infoLine, btn)
                        }
                        startRes.recommendedRodName != null && startRes.recommendedRodUnlocked == false -> {
                            val rodName = I18n.rod(startRes.recommendedRodName, lang)
                            val infoLine = if (lang == "ru") {
                                "Удочка «$rodName», подходящая для этой приманки, ещё не разблокирована."
                            } else {
                                "The \"$rodName\" rod for this bait is still locked."
                            }
                            Pair(infoLine, null)
                        }
                        else -> Pair(null, null)
                    }
                    val message = buildString {
                        append(baseLine)
                        if (!extraLine.isNullOrBlank()) {
                            append('\n')
                            append(extraLine)
                        }
                    }
                    val markup = button?.let { btn ->
                        Json.encodeToString(InlineKeyboardMarkup(listOf(listOf(btn))))
                    }
                    trySend(chatId, message, markup, replyToMessageId = replyTo)
                }
                val waitMessage = buildString {
                    if (!options.startNotification.isNullOrBlank()) {
                        append(options.startNotification)
                        append('\n')
                    }
                    append(
                        if (lang == "ru") {
                            "Забросили снасть. Ждём поклёвку..."
                        } else {
                            "The line is in the water. Waiting for a bite..."
                        }
                    )
                }
                trySend(chatId, waitMessage, replyToMessageId = replyTo)
                val job = castScope.launch {
                    val waitSeconds = 5 + Random.nextInt(26)
                    delay(waitSeconds * 1000L)
                    try {
                        val escapeInfo = fishing.locationEscapeChance(uid)
                        val extraEscapeChance = if (escapeInfo.rodBonusMultiplier < 1.0) 0.15 else 0.30
                        val hookRes = fishing.hook(uid, waitSeconds, 0.0, extraEscapeChance)
                        if (!hookRes.success) {
                            val escapedText = if (lang == "ru") "Рыба сорвалась!" else "The fish got away!"
                            trySend(chatId, escapedText, replyToMessageId = replyTo)
                            logCommandMetric(
                                "cast",
                                mapOf("result" to "escaped", "location" to escapeInfo.locationId.toString()),
                                source,
                            )
                            return@launch
                        }
                        val castRes = fishing.cast(uid, waitSeconds, 0.0, true)
                        val catch = castRes.catch
                        if (catch == null) {
                            val escapedText = if (lang == "ru") "Рыба сорвалась!" else "The fish got away!"
                            trySend(chatId, escapedText, replyToMessageId = replyTo)
                            logCommandMetric(
                                "cast",
                                mapOf(
                                    "result" to "escaped",
                                    "location" to escapeInfo.locationId.toString(),
                                    "stage" to "final",
                                ),
                                source,
                            )
                            return@launch
                        }
                        val fishName = I18n.fish(catch.fish, lang)
                        val locationName = I18n.location(catch.location, lang)
                        val isNew = catch.fishId?.let { it !in knownFish } == true
                        if (isNew && catch.fishId != null) {
                            knownFish.add(catch.fishId)
                        }
                        val newLine = if (isNew) {
                            if (lang == "ru") "\n✨ Новая рыба!" else "\n✨ New fish!"
                        } else {
                            ""
                        }
                        val unlockedLine = if (castRes.unlockedLocations.isNotEmpty()) {
                            val localized = castRes.unlockedLocations.map { I18n.location(it, lang) }
                            val prefix = if (lang == "ru") {
                                if (localized.size > 1) "\n📍 Открыы новые локации: " else "\n📍 Открыта новая локация: "
                            } else {
                                if (localized.size > 1) "\n📍 New locations unlocked: " else "\n📍 New location unlocked: "
                            }
                            prefix + localized.joinToString(", ")
                        } else {
                            ""
                        }
                        val rodLine = if (castRes.unlockedRods.isNotEmpty()) {
                            val localized = castRes.unlockedRods.map { I18n.rod(it, lang) }
                            val prefix = if (lang == "ru") {
                                if (localized.size > 1) "\n🎣 Открыты новые удочки: " else "\n🎣 Открыта новая удочка: "
                            } else {
                                if (localized.size > 1) "\n🎣 New rods unlocked: " else "\n🎣 New rod unlocked: "
                            }
                            prefix + localized.joinToString(", ")
                        } else {
                            ""
                        }
                        val coinsLine = when {
                            castRes.coins > 0 -> {
                                val amount = castRes.coins
                                if (lang == "ru") {
                                    "\n🪙 +${amount} монет"
                                } else {
                                    val suffix = if (amount == 1) "" else "s"
                                    "\n🪙 +${amount} coin${suffix}"
                                }
                            }
                            castRes.coins == 0 -> {
                                if (lang == "ru") {
                                    "\n🪙 Монеты не начислены из-за лимита"
                                } else {
                                    "\n🪙 No coins due to daily cap"
                                }
                            }
                            else -> ""
                        }
                        val captionBase = buildCatchCaption(
                            lang = lang,
                            fishName = fishName,
                            rarity = catch.rarity,
                            weightKg = catch.weight,
                            locationName = locationName,
                            extraLines = listOf(coinsLine, newLine, unlockedLine, rodLine),
                        )
                        var caption = appendCatchTags(captionBase, catch)
                        if (!options.catchFooter.isNullOrBlank()) {
                            caption += "\n${options.catchFooter}"
                        }
                        val caughtAt = catch.at?.let { runCatching { Instant.parse(it) }.getOrNull() }
                        val image = generateCatchImage(
                            fishInternalName = catch.fish,
                            locationInternalName = catch.location,
                            displayFishName = fishName,
                            displayLocationName = locationName,
                            weightKg = catch.weight,
                            rarity = catch.rarity,
                            lang = lang,
                            anglerName = catch.user,
                            caughtAt = caughtAt,
                        )
                        if (image != null) {
                            try {
                                bot.sendPhoto(chatId, image, caption, replyToMessageId = replyTo)
                            } catch (e: Exception) {
                                log.error("sendPhoto failed chatId={} fish={}", chatId, catch.fish, e)
                                trySend(chatId, caption, replyToMessageId = replyTo)
                            }
                        } else {
                            trySend(chatId, caption, replyToMessageId = replyTo)
                        }
                        logCommandMetric(
                            "cast",
                            mapOf(
                                "result" to "caught",
                                "rarity" to catch.rarity,
                                "location" to escapeInfo.locationId.toString(),
                                "fish" to catch.fish,
                            ),
                            source,
                        )
                        Metrics.gauge(
                            "catch_weight_kg",
                            catch.weight,
                            mapOf(
                                "fish" to catch.fish,
                                "location" to catch.location,
                                "rarity" to catch.rarity,
                            ),
                        )
                    } catch (e: Exception) {
                        log.error("cast command failed uid={} chatId={} source={}", uid, chatId, source, e)
                        fishing.resetCasting(uid)
                        val errorText = if (lang == "ru") "Не удалось завершить заброс." else "Failed to finish the cast."
                        trySend(chatId, errorText, replyToMessageId = replyTo)
                        logCommandMetric(
                            "cast",
                            mapOf("result" to "error", "stage" to "final"),
                            source,
                        )
                    }
                }
                return CastAttemptResult(started = true, completion = job)
            }

            suspend fun sendLanguageMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val ruLabel = if (lang == "ru") "Русский ✅" else "Русский"
                val enLabel = if (lang == "en") "English ✅" else "English"
                val body = if (lang == "ru") {
                    "Текущий язык: Русский\nВыберите язык:"
                } else {
                    "Current language: English\nChoose a language:"
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix)
                        append("\n\n")
                    }
                    append(body)
                }
                val markup = Json.encodeToString(
                    InlineKeyboardMarkup(
                        listOf(
                            listOf(InlineKeyboardButton(ruLabel, "/language ${ownedData(uid, "ru")}")),
                            listOf(InlineKeyboardButton(enLabel, "/language ${ownedData(uid, "en")}")),
                        )
                    )
                )
                trySend(chatId, text, markup, replyToMessageId)
            }

            fun rodBonusLabel(water: String?, predator: Boolean?, lang: String): String {
                return when {
                    water == null -> if (lang == "ru") "Бонусов нет." else "No bonus."
                    water == "fresh" && predator == true -> if (lang == "ru") {
                        "−50% шанс побега пресноводных хищных рыб."
                    } else {
                        "50% less escape chance for freshwater predator fish."
                    }
                    water == "fresh" && (predator == false) -> if (lang == "ru") {
                        "−50% шанс побега пресноводных мирных рыб."
                    } else {
                        "50% less escape chance for freshwater peaceful fish."
                    }
                    water == "salt" && predator == true -> if (lang == "ru") {
                        "−50% шанс побега морских хищных рыб."
                    } else {
                        "50% less escape chance for saltwater predator fish."
                    }
                    water == "salt" && (predator == false) -> if (lang == "ru") {
                        "−50% шанс побега морских мирных рыб."
                    } else {
                        "50% less escape chance for saltwater peaceful fish."
                    }
                    else -> if (lang == "ru") "Бонусов нет." else "No bonus."
                }
            }

            suspend fun sendBaitMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val lures = fishing.listLures(uid).filter { it.qty > 0 }
                val currentId = transaction {
                    Users.select { Users.id eq uid }.single()[Users.currentLureId]?.value
                }
                val sorted = lures.sortedWith(
                    compareBy(
                        { BAIT_ORDER_INDEX[it.name] ?: Int.MAX_VALUE },
                        { I18n.lure(it.name, lang) },
                    )
                )
                val currentName = sorted.find { it.id == currentId }?.let { I18n.lure(it.name, lang) }
                val header = if (lang == "ru") {
                    "Текущая приманка: ${currentName ?: "не выбрана"}"
                } else {
                    "Current bait: ${currentName ?: "not selected"}"
                }
                val prompt = if (sorted.isEmpty()) {
                    if (lang == "ru") "У вас нет приманок" else "You don't have any baits"
                } else {
                    if (lang == "ru") "Выберите приманку:" else "Choose a bait:"
                }
                val details = sorted.joinToString("\n") { lure ->
                    val name = I18n.lure(lure.name, lang)
                    val desc = I18n.lureDescription(lure.name, lang)
                    val qtyLabel = if (lang == "ru") "${lure.qty} шт." else "${lure.qty} pcs."
                    if (desc.isBlank()) {
                        "• $name — $qtyLabel"
                    } else {
                        "• $name — $desc ($qtyLabel)"
                    }
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix)
                        append("\n\n")
                    }
                    append(header)
                    append("\n")
                    append(prompt)
                    if (details.isNotBlank()) {
                        append("\n")
                        append(details)
                    }
                }
                if (sorted.isEmpty()) {
                    trySend(chatId, text, replyToMessageId = replyToMessageId)
                    return
                }
                val buttons = sorted.map { lure ->
                    val title = buildString {
                        append(I18n.lure(lure.name, lang))
                        append(" (")
                        append(lure.qty)
                        append(")")
                        if (lure.id == currentId) append(" ✅")
                    }
                        InlineKeyboardButton(title, "/bait ${ownedData(uid, lure.id)}")
                }.chunked(2)
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup, replyToMessageId)
            }

            suspend fun sendDiscountMenu(chatId: Long) {
                val discounts = fishing.listDiscounts().sortedBy { it.startDate }
                val body = if (discounts.isEmpty()) {
                    "Скидок сейчас нет."
                } else {
                    val lines = discounts.joinToString("\n") { d ->
                        val pack = fishing.findPack(d.packageId)
                        val name = pack?.let { I18n.text(it.name, "ru") } ?: d.packageId
                        val basePrice = pack?.price
                        val priceText = basePrice?.let { strikethrough("$it⭐") + " ${d.price}⭐" } ?: "${d.price}⭐"
                        val start = d.startDate.format(DATE_FMT)
                        val end = d.endDate.format(DATE_FMT)
                        "• $name — $priceText ($start — $end)"
                    }
                    "Текущие скидки:\n$lines"
                }
                val buttons = mutableListOf<List<InlineKeyboardButton>>()
                buttons += listOf(InlineKeyboardButton("Добавить скидку", "create_discount"))
                discounts.forEach { d ->
                    val pack = fishing.findPack(d.packageId)
                    val name = pack?.let { I18n.text(it.name, "ru") } ?: d.packageId
                    val short = if (name.length > 32) name.take(29) + "…" else name
                    buttons += listOf(InlineKeyboardButton("Убрать: $short", "remove_discount_${d.packageId}"))
                }
                buttons += listOf(InlineKeyboardButton("Закрыть", "discount_cancel"))
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                try {
                    bot.sendMessage(chatId, body, markup)
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
            }

            suspend fun sendDiscountPackPicker(chatId: Long) {
                val packs = fishing.listShop("ru").flatMap { it.packs }.sortedBy { it.name }
                if (packs.isEmpty()) {
                    try {
                        bot.sendMessage(chatId, "Магазин пуст, скидку добавить нельзя")
                    } catch (e: Exception) {
                        log.error("sendMessage failed chatId={}", chatId, e)
                    }
                    return
                }
                val buttons = packs.map { pack ->
                    InlineKeyboardButton(pack.name, "discount_pack_${pack.id}")
                }.chunked(2).toMutableList()
                buttons += listOf(InlineKeyboardButton("Отмена", "discount_cancel"))
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                try {
                    bot.sendMessage(chatId, "Выберите товар для скидки", markup)
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
            }

            suspend fun sendRodMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val rods = fishing.listRods(uid)
                val currentId = transaction {
                    Users.select { Users.id eq uid }.single()[Users.currentRodId]?.value
                }
                val header = if (lang == "ru") {
                    "Текущая удочка: ${currentId?.let { id -> rods.find { it.id == id }?.let { I18n.rod(it.name, lang) } } ?: "не выбрана"}"
                } else {
                    "Current rod: ${currentId?.let { id -> rods.find { it.id == id }?.let { I18n.rod(it.name, lang) } } ?: "not selected"}"
                }
                val prompt = if (lang == "ru") "Выберите удочку или разблокируйте новую:" else "Choose a rod or unlock a new one:"
                val details = rods.joinToString("\n") { rod ->
                    val localizedName = I18n.rod(rod.name, lang)
                    val nameLine = buildString {
                        append("• ")
                        append(localizedName)
                        if (rod.id == currentId) append(" ✅")
                    }
                    val bonusLine = rodBonusLabel(rod.bonusWater, rod.bonusPredator, lang)
                    val infoLine = if (rod.unlocked) {
                        bonusLine
                    } else {
                        val weightPart = if (rod.unlockKg > 0) {
                            val template = if (lang == "ru") "Требуется %.0f кг" else "Requires %.0f kg"
                            template.format(Locale.US, rod.unlockKg)
                        } else {
                            if (lang == "ru") "Доступна сразу" else "Available immediately"
                        }
                        val pricePart = rod.priceStars?.let {
                            val priceText = "${it}⭐"
                            if (lang == "ru") "или $priceText" else "or $priceText"
                        }
                        val combined = listOfNotNull(weightPart.takeIf { it.isNotBlank() }, pricePart).joinToString(" ")
                        val requirement = combined.ifBlank {
                            if (lang == "ru") "Доступна сразу" else "Available immediately"
                        }
                        val bonusPrefix = if (lang == "ru") "Бонус: " else "Bonus: "
                        listOf("🔒 ${requirement.trim()}", bonusPrefix + bonusLine.trim())
                            .joinToString("\n")
                    }
                    val formattedInfo = infoLine.replace("\n", "\n  ")
                    "$nameLine\n  $formattedInfo"
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix.trim())
                        append("\n\n")
                    }
                    append(header)
                    append("\n")
                    append(prompt)
                    if (details.isNotBlank()) {
                        append("\n\n")
                        append(details)
                    }
                }
                val buttons = rods.mapNotNull { rod ->
                    val localizedName = I18n.rod(rod.name, lang)
                    if (rod.unlocked) {
                        val label = if (rod.id == currentId) "$localizedName ✅" else localizedName
                        listOf(InlineKeyboardButton(label, "/rod ${ownedData(uid, rod.id)}"))
                    } else {
                        val packId = fishing.rodPackId(rod.code)
                        val price = rod.priceStars
                        if (packId != null && price != null) {
                            val label = if (lang == "ru") {
                                "🔓 $localizedName — ${price}⭐"
                            } else {
                                "🔓 $localizedName — ${price}⭐"
                            }
                            listOf(InlineKeyboardButton(label, "/buy ${ownedData(uid, packId)}"))
                        } else {
                            null
                        }
                    }
                }
                val markup = if (buttons.isEmpty()) null else Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup, replyToMessageId)
            }

            suspend fun sendLocationMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val locations = fishing.locations(uid)
                val unlocked = locations.filter { it.unlocked }
                val stored = transaction {
                    Users.select { Users.id eq uid }.single()[Users.currentLocationId]?.value
                }
                val currentId = stored?.takeIf { id -> unlocked.any { it.id == id } } ?: unlocked.firstOrNull()?.id
                val currentName = currentId?.let { id ->
                    unlocked.find { it.id == id }?.let { I18n.location(it.name, lang) }
                }
                val header = if (lang == "ru") {
                    "Текущая локация: ${currentName ?: "не выбрана"}"
                } else {
                    "Current location: ${currentName ?: "not selected"}"
                }
                val prompt = if (unlocked.isEmpty()) {
                    if (lang == "ru") "Доступных локаций нет" else "No locations available"
                } else {
                    if (lang == "ru") "Выберите локацию:" else "Choose a location:"
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix)
                        append("\n\n")
                    }
                    append(header)
                    append("\n")
                    append(prompt)
                }
                if (unlocked.isEmpty()) {
                    trySend(chatId, text, replyToMessageId = replyToMessageId)
                    return
                }
                val buttons = unlocked
                    .sortedBy { I18n.location(it.name, lang) }
                    .map { loc ->
                        val label = buildString {
                            append(I18n.location(loc.name, lang))
                            if (loc.id == currentId) append(" ✅")
                        }
                        InlineKeyboardButton(label, "/location ${ownedData(uid, loc.id)}")
                    }
                    .chunked(2)
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup, replyToMessageId)
            }

            suspend fun sendPrizes(
                uid: Long,
                chatId: Long,
                lang: String,
                prizes: List<UserPrize>? = null,
                prefix: String? = null,
                replyToMessageId: Long? = null,
                showWhenEmpty: Boolean = false,
            ) {
                val actual = prizes ?: prizeService.pendingPrizes(uid)
                val packNames = fishing.listShop(lang).flatMap { it.packs }.associate { it.id to it.name }
                val prefixText = prefix?.trim()?.takeIf { it.isNotEmpty() }
                val emptyBody = if (lang == "ru") {
                    "Нет призов для получения."
                } else {
                    "No prizes to claim."
                }
                fun displayName(prize: UserPrize): String {
                    if (prize.packageId == COIN_PRIZE_ID) {
                        return if (lang == "ru") "Монеты" else "Coins"
                    }
                    if (prize.packageId == "autofish_week") {
                        return if (lang == "ru") "Автоловля (неделя)" else "Auto Catch (week)"
                    }
                    val name = packNames[prize.packageId]
                    if (name != null) return name
                    val lureName = I18n.lure(prize.packageId, lang)
                    if (lureName != prize.packageId) return lureName
                    return prize.packageId.replace('_', ' ')
                }
                if (actual.isEmpty()) {
                    val body = if (showWhenEmpty) emptyBody else ""
                    val baseText = buildString {
                        if (!prefixText.isNullOrBlank()) append(prefixText)
                        if (body.isNotBlank()) {
                            if (!prefixText.isNullOrBlank()) append("\n\n")
                            append(body)
                        }
                    }
                    if (baseText.isNotBlank()) {
                        trySend(chatId, baseText, replyToMessageId = replyToMessageId)
                    }
                    return
                }
                val body = run {
                    val header = if (lang == "ru") {
                        "Призы, которые можно получить:"
                    } else {
                        "Prizes you can claim:"
                    }
                    val lines = actual.joinToString("\n") { prize ->
                        val name = displayName(prize)
                        val qty = if (prize.packageId == COIN_PRIZE_ID) {
                            val amount = prize.coins ?: prize.qty
                            " +$amount"
                        } else if (prize.qty > 1) {
                            " x${prize.qty}"
                        } else {
                            ""
                        }
                        val place = if (prize.rank > 0) {
                            if (lang == "ru") " (место #${prize.rank})" else " (place #${prize.rank})"
                        } else ""
                        "• $name$qty$place"
                    }
                    "$header\n$lines"
                }
                val baseText = buildString {
                    if (!prefixText.isNullOrBlank()) {
                        append(prefixText)
                        if (body.isNotBlank()) append("\n\n")
                    }
                    append(body)
                }
                val markup = Json.encodeToString(
                    InlineKeyboardMarkup(
                        actual.map { prize ->
                            val name = displayName(prize)
                            val short = if (name.length > 32) name.take(29) + "…" else name
                            val qty = if (prize.qty > 1) " x${prize.qty}" else ""
                            listOf(InlineKeyboardButton("🎁 $short$qty", "/prizeclaim ${ownedData(uid, prize.id)}"))
                        }
                    )
                )
                trySend(chatId, baseText, markup, replyToMessageId)
            }

            fun packNamesRu(): MutableMap<String, String> {
                val names = fishing.listShop("ru").flatMap { it.packs }.associate { it.id to it.name }.toMutableMap()
                if (!names.containsKey("autofish_week")) {
                    names["autofish_week"] = "Автоловля (неделя)"
                }
                return names
            }

            fun formatAdminPrize(prize: PrizeSpec?, packNames: Map<String, String>): String? {
                if (prize == null) return null
                return when {
                    prize.pack == COIN_PRIZE_ID || prize.coins != null -> {
                        val amount = prize.coins ?: prize.qty
                        if (amount <= 0) null else "Монеты $amount"
                    }
                    prize.pack.isBlank() -> null
                    else -> {
                        val name = packNames[prize.pack] ?: prize.pack
                        val qty = if (prize.qty > 1) " x${prize.qty}" else ""
                        "$name$qty"
                    }
                }
            }

            fun storePrize(draft: AdminDraft, prize: PrizeSpec) {
                val idx = draft.currentPrize - 1
                if (idx < 0) return
                while (draft.prizes.size <= idx) {
                    draft.prizes.add(PrizeSpec())
                }
                draft.prizes[idx] = prize
            }

            suspend fun finalizeTournamentDraft(chatId: Long, userId: Long, draft: AdminDraft) {
                val start = draft.start
                val end = draft.end
                if (start != null && end != null) {
                    val prizesJson = Json.encodeToString(draft.prizes.take(draft.prizePlaces))
                    try {
                        if (draft.id == null) {
                            tournaments.createTournament(
                                draft.nameRu,
                                draft.nameEn,
                                start,
                                end,
                                draft.fish,
                                draft.location,
                                draft.metric,
                                draft.prizePlaces,
                                prizesJson,
                            )
                            bot.sendMessage(chatId, "Турнир создан")
                        } else {
                            tournaments.updateTournament(
                                draft.id!!,
                                draft.nameRu,
                                draft.nameEn,
                                start,
                                end,
                                draft.fish,
                                draft.location,
                                draft.metric,
                                draft.prizePlaces,
                                prizesJson,
                            )
                            bot.sendMessage(chatId, "Турнир обновлен")
                        }
                    } catch (e: Exception) {
                        log.error("tournament save failed", e)
                    }
                }
                adminStates.remove(userId)
            }

            suspend fun sendPrizePrompt(chatId: Long, draft: AdminDraft) {
                draft.awaitingPrizeQuantity = false
                draft.pendingPrize = null
                val packNames = packNamesRu()
                val current = formatAdminPrize(draft.prizes.getOrNull(draft.currentPrize - 1), packNames)
                    ?.let { " (сейчас: $it)" } ?: ""
                val message = "Награда за ${draft.currentPrize} место$current\nВыберите кнопку или введите вручную (pack qty)."
                val buttons = mutableListOf<List<InlineKeyboardButton>>()
                buttons += listOf(listOf(InlineKeyboardButton("🪙 Монеты", "prize_coins")))
                val packButtons = packNames.entries
                    .sortedBy { it.value.lowercase() }
                    .map { InlineKeyboardButton(it.value, "prize_pack_${it.key}") }
                    .chunked(2)
                buttons += packButtons
                buttons += listOf(listOf(InlineKeyboardButton("Без приза", "prize_skip")))
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                try {
                    bot.sendMessage(chatId, message, markup)
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
            }

            suspend fun promptPrizeQuantity(chatId: Long, label: String, isCoins: Boolean) {
                val text = if (isCoins) {
                    "Введите количество монет для \"$label\""
                } else {
                    "Введите количество для \"$label\""
                }
                try {
                    bot.sendMessage(chatId, text)
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
            }

            suspend fun proceedToNextPrize(chatId: Long, userId: Long, draft: AdminDraft) {
                if (draft.currentPrize < draft.prizePlaces) {
                    draft.currentPrize += 1
                    sendPrizePrompt(chatId, draft)
                } else {
                    finalizeTournamentDraft(chatId, userId, draft)
                }
            }

            suspend fun sendShopMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                replyToMessageId: Long? = null,
            ) {
                val lockedRodCodes = fishing.listRods(uid).filterNot { it.unlocked }.map { it.code }.toSet()
                val shop = fishing.listShop(lang)
                    .map { category ->
                        category.copy(
                            packs = category.packs.filter { pack ->
                                pack.rodCode == null || pack.rodCode in lockedRodCodes
                            }
                        )
                    }
                    .filter { it.packs.isNotEmpty() }
                if (shop.isEmpty()) {
                    val emptyText = if (lang == "ru") {
                        "Магазин пока пуст."
                    } else {
                        "The shop is empty right now."
                    }
                    trySend(chatId, emptyText, replyToMessageId = replyToMessageId)
                    return
                }
                val header = if (lang == "ru") {
                    "Магазин за звёзды. Выберите товар:"
                } else {
                    "Star shop. Choose an item:"
                }
                val footer = if (lang == "ru") {
                    "Нажми на кнопку, чтобы получить счёт в звёздах."
                } else {
                    "Tap a button to receive a Stars invoice."
                }
                val text = buildString {
                    append(header)
                    shop.forEach { category ->
                        append("\n\n")
                        append(category.name)
                        category.packs.forEach { pack ->
                            append("\n• ")
                            append(pack.name)
                            append(" — ")
                            val priceText = if (pack.originalPrice != null && pack.originalPrice > pack.price) {
                                val discountPercent = ((pack.originalPrice - pack.price) * 100) / pack.originalPrice
                                val untilPart = pack.discountEnd?.format(DATE_FMT)?.let {
                                    if (lang == "ru") " до $it" else " until $it"
                                } ?: ""
                                val discountLabel = if (lang == "ru") {
                                    "скидка $discountPercent% от полной цены$untilPart"
                                } else {
                                    "discount $discountPercent% off full price$untilPart"
                                }
                                "${pack.price}⭐ ($discountLabel)"
                            } else {
                                "${pack.price}⭐"
                            }
                            append(priceText)
                            if (pack.desc.isNotBlank()) {
                                append("\n  ")
                                append(pack.desc)
                            }
                        }
                    }
                    append("\n\n")
                    append(footer)
                }.trim()

                val buttons = shop.flatMap { category ->
                    category.packs.map { pack ->
                        val labelPrice = "${pack.price}⭐"
                        val label = "${pack.name} — $labelPrice"
                        InlineKeyboardButton(label, "/buy ${ownedData(uid, pack.id)}")
                    }
                }.chunked(2)

                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                val parts = splitMessage(text)
                if (parts.size == 1) {
                    trySend(chatId, text, markup, replyToMessageId)
                } else {
                    parts.forEachIndexed { index, part ->
                        val trimmedPart = part.trimEnd()
                        val partMarkup = if (index == 0) markup else null
                        val partReplyTo = if (index == 0) replyToMessageId else null
                        trySend(chatId, trimmedPart, partMarkup, partReplyTo)
                    }
                }
            }

            suspend fun sendCoinShopMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                replyToMessageId: Long? = null,
            ) {
                val shop = fishing.listShop(lang).map { category ->
                    category.copy(packs = category.packs.filter { it.coinPrice != null })
                }.filter { it.packs.isNotEmpty() }
                if (shop.isEmpty()) {
                    val emptyText = if (lang == "ru") {
                        "Покупки за монеты пока недоступны."
                    } else {
                        "Coin shop is unavailable right now."
                    }
                    trySend(chatId, emptyText, replyToMessageId = replyToMessageId)
                    return
                }
                val locale = if (lang == "ru") Locale("ru", "RU") else Locale.US
                val formatter = NumberFormat.getIntegerInstance(locale)
                val balance = transaction { Users.select { Users.id eq uid }.single()[Users.coins] }
                val balanceText = formatter.format(balance)
                val header = if (lang == "ru") {
                    "🪙 Магазин за монеты. Баланс: $balanceText монет."
                } else {
                    "🪙 Coin shop. Balance: $balanceText coins."
                }
                val footer = if (lang == "ru") {
                    "Нажми на кнопку, чтобы купить за монеты. Или открой мини-приложение: https://t.me/${env.botName}?startapp"
                } else {
                    "Tap a button to buy with coins. Or open the mini app: https://t.me/${env.botName}?startapp"
                }
                val text = buildString {
                    append(header)
                    shop.forEach { category ->
                        append("\n\n")
                        append(category.name)
                        category.packs.forEach { pack ->
                            val coinPrice = pack.coinPrice ?: return@forEach
                            val priceText = formatter.format(coinPrice)
                            append("\n• ")
                            append(pack.name)
                            append(" — 🪙 ")
                            append(priceText)
                            if (pack.desc.isNotBlank()) {
                                append("\n  ")
                                append(pack.desc)
                            }
                        }
                    }
                    append("\n\n")
                    append(footer)
                }.trim()
                val buttons = shop.flatMap { category ->
                    category.packs.map { pack ->
                        val coinPrice = pack.coinPrice ?: return@map null
                        val priceText = formatter.format(coinPrice)
                        val label = "${pack.name} — 🪙 $priceText"
                        InlineKeyboardButton(label, "/coinbuy ${ownedData(uid, pack.id)}")
                    }.filterNotNull()
                }.chunked(2)
                val markup = if (buttons.isEmpty()) null else Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup, replyToMessageId)
            }

            val keywordCommandMap = mapOf(
                "рыба" to "/cast",
                "рыбалка" to "/cast",
                "fish" to "/cast",
                "fishing" to "/cast",
                "cast" to "/cast",
                "casting" to "/cast",
                "bait" to "/bait",
                "приманка" to "/bait",
                "локация" to "/location",
                "место" to "/location",
                "place" to "/location",
                "location" to "/location",
                "удочка" to "/rod",
                "rod" to "/rod",
                "магазин" to "/shop",
                "shop" to "/shop",
                "статистика" to "/stats",
                "статы" to "/stats",
                "стата" to "/stats",
                "statistics" to "/stats",
                "stats" to "/stats",
                "info" to "/stats",
                "information" to "/stats",
                "язык" to "/language",
                "language" to "/language",
                "ник" to "/nickname",
                "никнейм" to "/nickname",
                "nick" to "/nickname",
                "nickname" to "/nickname",
                "приз" to "/prizes",
                "призы" to "/prizes",
                "prize" to "/prizes",
                "prizes" to "/prizes",
                "рейтинг" to "/daily_rating",
                "rating" to "/daily_rating",
                "leaderboard" to "/daily_rating",
                "турнир" to "/tournament",
                "tournament" to "/tournament",
            )

            suspend fun processUserCommand(
                rawText: String,
                from: TgUser?,
                chatId: Long,
                isCallback: Boolean = false,
                messageId: Long? = null,
                sourceOverride: String? = null,
            ): Boolean {
                val text = rawText.trim()
                if (text.isEmpty()) return false

                val keywordCommand = keywordCommandMap[text.lowercase(Locale.ROOT)]
                if (keywordCommand != null && !text.startsWith("/")) {
                    return processUserCommand(
                        keywordCommand,
                        from,
                        chatId,
                        isCallback,
                        messageId,
                        sourceOverride,
                    )
                }

                if (!text.startsWith("/")) return false
                val commandLine = text.lineSequence().firstOrNull()?.trim().orEmpty()
                if (commandLine.isEmpty()) return false
                val parts = commandLine.split(" ", limit = 2)
                val command = parts[0]
                val commandTarget = command.substringAfter('@', "").takeIf { it.isNotEmpty() }
                if (commandTarget != null && !commandTarget.equals(env.botName, ignoreCase = true)) {
                    return false
                }
                val commandName = command.substringBefore('@')
                val arg = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
                val source = sourceOverride ?: if (isCallback) "callback" else "message"
                val replyTo = messageId
                fun ownedArg(raw: String?, uid: Long): Pair<String?, Boolean> {
                    if (!isCallback) return raw to false
                    if (raw == null) return null to false
                    val prefix = "$uid:"
                    return if (raw.startsWith(prefix)) {
                        raw.removePrefix(prefix) to false
                    } else {
                        null to true
                    }
                }
                when (commandName) {
                    "/startapp" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        logCommandMetric("startapp", source = source)
                        val link = "https://t.me/${env.botName}?startapp"
                        val responseText = if (lang == "ru") {
                            "Нажми кнопку, чтобы открыть игру"
                        } else {
                            "Tap the button to open the game"
                        }
                        val buttonText = if (lang == "ru") "Открыть игру" else "Open game"
                        val markup = """{"inline_keyboard":[[{"text":"$buttonText","url":"$link"}]]}"""
                        trySend(chatId, responseText, markup, replyTo)
                        return true
                    }
                    "/start" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val params = arg?.let { mapOf("payload" to it) } ?: emptyMap()
                        logCommandMetric("start", params, source)
                        val message = if (lang == "ru") {
                            """🎣 Привет! Это River King — игра про рыбалку. Играй через приложение или с помощью команд бота. Команды работают и в групповых чатах, если добавить туда бота и дать ему доступ к сообщениям.

Доступные команды:
/cast — забросить снасть
/autocast — запустить автоловлю
/stop_autocast — остановить автоловлю
/bait — сменить приманку
/rod — выбрать удочку
/location — сменить локацию
/daily — получить ежедневную награду
/prizes — забрать призы турнира
/shop — купить приманки и удочки за звёзды
/coin_shop — купить наборы за монеты
/tournament — таблица текущего турнира и твоя позиция
/daily_rating — текущее место твоего лучшего улова в ежедневном рейтинге
/stats — статистика по пойманной рыбе
/language — выбрать язык
/nickname — сменить ник""".trimIndent()
                        } else {
                            """🎣 Welcome to River King, a fishing game you can play in the app or via bot commands. Bot commands also work in group chats if you add the bot and allow it to access messages.

Available commands:
/cast — cast your line
/autocast — start auto casting
/stop_autocast — stop auto casting
/bait — change your bait
/rod — change your rod
/location — change your location
/daily — claim your daily reward
/prizes — claim tournament prizes
/shop — buy baits and rods with Stars
/coin_shop — buy bundles with coins
/tournament — view the current tournament leaderboard and your rank
/daily_rating — view the current placement of your best catch in today's daily rating
/stats — your fishing stats
/language — choose your language
/nickname — change your nickname""".trimIndent()
                        }
                        val openButtonText = if (lang == "ru") "Открыть игру" else "Open game"
                        val channelButtonText = if (lang == "ru") "Присоединиться к каналу" else "Join the channel"
                        val gameLink = "https://t.me/${env.botName}?startapp"
                        val channelLink = if (lang == "ru") {
                            "https://t.me/riverking_ru"
                        } else {
                            "https://t.me/riverking_en"
                        }
                        val markup = Json.encodeToString(
                            mapOf(
                                "inline_keyboard" to listOf(
                                    listOf(mapOf("text" to openButtonText, "url" to gameLink)),
                                    listOf(mapOf("text" to channelButtonText, "url" to channelLink)),
                                )
                            )
                        )
                        trySend(chatId, message, markup, replyTo)
                        return true
                    }
                    "/tournament" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val t = tournaments.currentTournament()
                        val reply = if (t != null) {
                            val tName = if (lang == "ru") t.nameRu else t.nameEn
                            val (list, mine) = tournaments.leaderboard(t, uid, t.prizePlaces)
                            val metric = t.metric.lowercase()
                            val isAggregate = metric == "count" || metric == "total_weight"
                            val includeFishName = metric == "largest" || metric == "smallest"
                            val remaining = Duration.between(Instant.now(), t.endTime)
                            val safeRemaining = if (remaining.isNegative) Duration.ZERO else remaining
                            val totalMinutes = safeRemaining.toMinutes()
                            val days = totalMinutes / (24 * 60)
                            val hours = (totalMinutes % (24 * 60)) / 60
                            val minutes = totalMinutes % 60
                            val timeText = if (lang == "ru") {
                                "Осталось: ${days}д ${hours}ч ${minutes}м"
                            } else {
                                "Time left: ${days}d ${hours}h ${minutes}m"
                            }
                            val fishName = t.fish?.takeIf { it.isNotBlank() }?.let { I18n.fish(it, lang) }
                                ?: if (lang == "ru") "любая" else "any"
                            val locationName = t.location?.takeIf { it.isNotBlank() }
                                ?.let { I18n.location(it, lang) }
                                ?: if (lang == "ru") "любая" else "any"
                            val metricText = I18n.tournamentMetric(t.metric, lang)
                            val conditions = if (lang == "ru") {
                                "Рыба: $fishName\nЛокация: $locationName\nТип: $metricText"
                            } else {
                                "Fish: $fishName\nLocation: $locationName\nType: $metricText"
                            }
                            val header = buildString {
                                appendLine(tName)
                                appendLine(timeText)
                                appendLine(conditions)
                                appendLine()
                            }
                            val body = if (list.isEmpty()) {
                                if (lang == "ru") "Список пуст" else "Leaderboard is empty"
                            } else {
                                list.joinToString("\n") { e ->
                                    val valueText = if (metric == "count") {
                                        e.value.toInt().toString()
                                    } else {
                                        "%.2f".format(Locale.US, e.value)
                                    }
                                    val info = if (includeFishName) {
                                        val fishName = e.fish?.let { I18n.fish(it, lang) } ?: "-"
                                        "$fishName $valueText"
                                    } else {
                                        valueText
                                    }
                                    val userName = e.user?.let { "\u2066$it\u2069" } ?: "-"
                                    "\u200E${e.rank}. $userName — $info"
                                }
                            }
                            val mineLine = if (mine != null && mine.rank > t.prizePlaces) {
                                val valueText = if (metric == "count") {
                                    mine.value.toInt().toString()
                                } else {
                                    "%.2f".format(Locale.US, mine.value)
                                }
                                val line = if (lang == "ru") {
                                    when {
                                        includeFishName -> {
                                            val fishName = mine.fish?.let { I18n.fish(it, lang) } ?: "-"
                                            "Твоя позиция: ${mine.rank}. Рыба: $fishName $valueText"
                                        }
                                        isAggregate -> {
                                            if (metric == "count") {
                                                "Твоя позиция: ${mine.rank}. Поймано: $valueText"
                                            } else {
                                                "Твоя позиция: ${mine.rank}. Вес: $valueText"
                                            }
                                        }
                                        else -> "Твоя позиция: ${mine.rank}. Вес: $valueText"
                                    }
                                } else {
                                    when {
                                        includeFishName -> {
                                            val fishName = mine.fish?.let { I18n.fish(it, lang) } ?: "-"
                                            "Your position: ${mine.rank}. Fish: $fishName $valueText"
                                        }
                                        isAggregate -> {
                                            if (metric == "count") {
                                                "Your position: ${mine.rank}. Caught: $valueText"
                                            } else {
                                                "Your position: ${mine.rank}. Weight: $valueText"
                                            }
                                        }
                                        else -> "Your position: ${mine.rank}. Weight: $valueText"
                                    }
                                }
                                "\u200E$line"
                            } else null
                            buildString {
                                append(header)
                                append(body)
                                if (mineLine != null) {
                                    append("\n\n")
                                    append(mineLine)
                                }
                            }
                        } else {
                            if (lang == "ru") "Сейчас нет активного турнира" else "No active tournament"
                        }
                        val params = t?.let { mapOf("metric" to it.metric.lowercase()) } ?: emptyMap()
                        logCommandMetric("tournament", params, source)
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/daily_rating" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val positions = fishing.dailyRatingPositions(uid)
                        val reply = if (positions.isEmpty()) {
                            if (lang == "ru") {
                                "Сегодня ты ещё не участвовал в ежедневном рейтинге."
                            } else {
                                "You haven't entered today's daily rating yet."
                            }
                        } else {
                            val header = if (lang == "ru") {
                                "Твои места в сегодняшнем ежедневном рейтинге:"
                            } else {
                                "Your positions in today's daily rating:"
                            }
                            val locale = if (lang == "ru") Locale("ru", "RU") else Locale.US
                            val numberFormat = NumberFormat.getIntegerInstance(locale)
                            val weightFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(locale))
                            fun englishOrdinal(rank: Int): String {
                                val mod100 = rank % 100
                                val suffix = if (mod100 in 11..13) {
                                    "th"
                                } else {
                                    when (rank % 10) {
                                        1 -> "st"
                                        2 -> "nd"
                                        3 -> "rd"
                                        else -> "th"
                                    }
                                }
                                return "$rank$suffix"
                            }
                            fun formatPlace(rank: Int): String = if (lang == "ru") {
                                "$rank место"
                            } else {
                                "${englishOrdinal(rank)} place"
                            }
                            val body = positions.joinToString("\n") { pos ->
                                val locationName = I18n.location(pos.location, lang)
                                val fishName = I18n.fish(pos.bestFish, lang)
                                val rarityName = RARITY_LABELS[lang]?.get(pos.bestRarity)
                                    ?: RARITY_LABELS["en"]?.get(pos.bestRarity)
                                    ?: pos.bestRarity
                                val weightText = weightFormat.format(pos.bestWeight)
                                val coins = pos.prizeCoins?.let {
                                    val coinsText = numberFormat.format(it)
                                    if (lang == "ru") {
                                        ", $coinsText монет"
                                    } else {
                                        ", $coinsText coins"
                                    }
                                } ?: ""
                                "• $locationName — ${formatPlace(pos.rank)}. $fishName (${rarityName}), $weightText ${if (lang == "ru") "кг" else "kg"}$coins"
                            }
                            "$header\n$body"
                        }
                        logCommandMetric("daily_rating", source = source)
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/prizes" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val prizes = prizeService.pendingPrizes(uid)
                        logCommandMetric("prizes", mapOf("count" to prizes.size.toString()), source)
                        sendPrizes(
                            uid,
                            chatId,
                            lang,
                            prizes = prizes,
                            replyToMessageId = replyTo,
                            showWhenEmpty = true,
                        )
                        return true
                    }
                    "/daily" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val schedule = fishing.dailyRewardSchedule(uid)
                        val res = fishing.giveDailyBaits(uid)
                        fun rewardLines(rewards: List<FishingService.DailyReward>): List<String> =
                            rewards.map { reward ->
                                val name = I18n.lure(reward.name, lang)
                                "• $name x${reward.qty}"
                            }
                        if (res == null) {
                            val params = mapOf("result" to "not_ready")
                            logCommandMetric("daily", params, source)
                            val streak = transaction {
                                Users.select { Users.id eq uid }.single()[Users.dailyStreak]
                            }
                            val displayDay = (if (streak <= 0) 1 else streak + 1).coerceAtMost(7)
                            val rewards = schedule.getOrNull(displayDay - 1).orEmpty()
                            val alreadyClaimed = if (lang == "ru") {
                                "Ежедневная награда уже получена."
                            } else {
                                "Daily reward already claimed."
                            }
                            val header = if (lang == "ru") {
                                "Награда завтра (день $displayDay):"
                            } else {
                                "Tomorrow's reward (day $displayDay):"
                            }
                            val lines = rewardLines(rewards)
                            val reply = buildString {
                                append(alreadyClaimed)
                                append("\n\n")
                                append(header)
                                if (lines.isNotEmpty()) {
                                    append("\n\n")
                                    append(lines.joinToString("\n"))
                                }
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                        } else {
                            val streak = res.third
                            val params = mapOf("result" to "claimed", "streak" to streak.toString())
                            logCommandMetric("daily", params, source)
                            val displayDay = streak.coerceAtMost(7)
                            val rewards = schedule.getOrNull(displayDay - 1).orEmpty()
                            val tomorrowDay = (streak + 1).coerceAtMost(7)
                            val tomorrowRewards = schedule.getOrNull(tomorrowDay - 1).orEmpty()
                            val todayHeader = if (lang == "ru") {
                                "Вы получили ежедневную награду (день $displayDay):"
                            } else {
                                "Daily reward claimed (day $displayDay):"
                            }
                            val tomorrowHeader = if (lang == "ru") {
                                "Награда завтра (день $tomorrowDay):"
                            } else {
                                "Tomorrow's reward (day $tomorrowDay):"
                            }
                            val todayLines = rewardLines(rewards)
                            val tomorrowLines = rewardLines(tomorrowRewards)
                            val text = buildString {
                                append(todayHeader)
                                if (todayLines.isNotEmpty()) {
                                    append("\n")
                                    append(todayLines.joinToString("\n"))
                                }
                                append("\n\n")
                                append(tomorrowHeader)
                                if (tomorrowLines.isNotEmpty()) {
                                    append("\n")
                                    append(tomorrowLines.joinToString("\n"))
                                }
                            }
                            trySend(chatId, text, replyToMessageId = replyTo)
                        }
                        return true
                    }
                    "/stats" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val totalCount = fishing.totalCaughtCount(uid)
                        val totalWeight = fishing.totalCaughtKg(uid)
                        val todayCount = fishing.todayCaughtCount(uid)
                        val todayWeight = fishing.todayCaughtKg(uid)
                        val rarityStats = fishing.catchStatsByRarity(uid)
                        val unit = if (lang == "ru") "кг" else "kg"
                        val countLabel = if (lang == "ru") "шт." else "fish"
                        fun formatWeight(value: Double) = "%.2f".format(Locale.US, value) + " $unit"
                        val breakdown = if (rarityStats.isEmpty()) {
                            ""
                        } else {
                            val title = if (lang == "ru") "\n\nРедкость:" else "\n\nBy rarity:"
                            val lines = rarityStats.joinToString("\n") { stat ->
                                val rarity = RARITY_LABELS[lang]?.get(stat.rarity) ?: stat.rarity
                                "• $rarity — ${stat.count} $countLabel, ${formatWeight(stat.weight)}"
                            }
                            title + "\n" + lines
                        }
                        val reply = if (lang == "ru") {
                            """🐟 Всего поймано: $totalCount $countLabel (${formatWeight(totalWeight)})
📅 За сегодня: $todayCount $countLabel (${formatWeight(todayWeight)})$breakdown""".trimIndent()
                        } else {
                            """🐟 Total caught: $totalCount $countLabel (${formatWeight(totalWeight)})
📅 Today: $todayCount $countLabel (${formatWeight(todayWeight)})$breakdown""".trimIndent()
                        }
                        logCommandMetric("stats", mapOf("result" to "shown"), source)
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/shop" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        logCommandMetric("shop", mapOf("action" to "show"), source)
                        sendShopMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        return true
                    }
                    "/coin_shop" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        logCommandMetric("coin_shop", mapOf("action" to "show"), source)
                        sendCoinShopMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        return true
                    }
                    "/buy" -> {
                        val buyerTgId = from?.id ?: return false
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value.isNullOrBlank()) {
                            logCommandMetric("buy", mapOf("result" to "missing_arg"), source)
                            val reply = if (lang == "ru") {
                                "Укажи набор или воспользуйся командой /shop."
                            } else {
                                "Specify a bundle id or use /shop."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val packId = value ?: return true
                        val packInfo = fishing.findPack(packId)
                        if (packInfo?.rodCode != null && fishing.hasRod(uid, packInfo.rodCode)) {
                            Metrics.counter(
                                "shop_purchase_denied_total",
                                mapOf("pack" to packId, "currency" to "stars", "reason" to "rod_unlocked"),
                            )
                            val reply = if (lang == "ru") {
                                "Эта удочка уже разблокирована."
                            } else {
                                "You already unlocked this rod."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val invoiceChatId = if (chatId < 0) buyerTgId else chatId
                        Metrics.counter(
                            "shop_purchase_click_total",
                            mapOf("pack" to packId, "currency" to "stars"),
                        )
                        try {
                            stars.sendPackageInvoice(invoiceChatId, buyerTgId, packId, lang)
                            logCommandMetric("buy", mapOf("result" to "sent", "pack" to packId), source)
                            if (invoiceChatId != chatId) {
                                val info = if (lang == "ru") {
                                    "Счёт отправлен в личные сообщения."
                                } else {
                                    "The invoice was sent to your private chat."
                                }
                                trySend(chatId, info, replyToMessageId = replyTo)
                            }
                        } catch (e: StarsPaymentService.MissingPrivateChatAccessException) {
                            log.warn("sendInvoice missing private chat access chatId={} pack={}", chatId, packId)
                            logCommandMetric(
                                "buy",
                                mapOf("result" to "dm_required", "pack" to packId),
                                source,
                            )
                            Metrics.counter(
                                "shop_purchase_denied_total",
                                mapOf("pack" to packId, "currency" to "stars", "reason" to "dm_required"),
                            )
                            if (invoiceChatId != chatId) {
                                val link = "https://t.me/${env.botName}?start=shop"
                                val info = if (lang == "ru") {
                                    "Бот не может отправить счёт в личные сообщения. Разреши боту писать, нажав Старт: $link"
                                } else {
                                    "I couldn't send the invoice to your private chat. Allow the bot to message you by pressing Start: $link"
                                }
                                trySend(chatId, info, replyToMessageId = replyTo)
                            } else {
                                val reply = if (lang == "ru") {
                                    "Не удалось отправить счёт. Попробуй ещё раз позже."
                                } else {
                                    "Failed to send the invoice. Please try again later."
                                }
                                trySend(chatId, reply, replyToMessageId = replyTo)
                            }
                        } catch (e: Exception) {
                            log.error("sendInvoice failed chatId={} pack={}", chatId, packId, e)
                            logCommandMetric("buy", mapOf("result" to "error", "pack" to packId), source)
                            Metrics.counter(
                                "shop_purchase_failed_total",
                                mapOf("pack" to packId, "currency" to "stars", "reason" to "invoice_error"),
                            )
                            val reply = if (invoiceChatId != chatId) {
                                val link = "https://t.me/${env.botName}?start=start"
                                if (lang == "ru") {
                                    "Не удалось отправить счёт. Возможно, бот не может писать тебе личные сообщения — разреши ему писать, перейдя по ссылке: $link. Попробуй ещё раз позже."
                                } else {
                                    "Failed to send the invoice. The bot might not be allowed to message you privately — allow it by opening: $link. Please try again later."
                                }
                            } else {
                                if (lang == "ru") {
                                    "Не удалось отправить счёт. Попробуй ещё раз позже."
                                } else {
                                    "Failed to send the invoice. Please try again later."
                                }
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                        }
                        return true
                    }
                    "/coinbuy" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        val packId = value
                        if (packId.isNullOrBlank()) {
                            logCommandMetric("coin_buy", mapOf("result" to "missing_arg"), source)
                            val reply = if (lang == "ru") {
                                "Укажи набор или воспользуйся командой /coin_shop."
                            } else {
                                "Specify a bundle id or use /coin_shop."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val pack = fishing.listShop(lang).flatMap { it.packs }.find { it.id == packId }
                        if (pack == null || pack.coinPrice == null) {
                            logCommandMetric(
                                "coin_buy",
                                mapOf("result" to "invalid_pack", "pack" to packId),
                                source,
                            )
                            val reply = if (lang == "ru") {
                                "Этот набор нельзя купить за монеты."
                            } else {
                                "This bundle can't be purchased with coins."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val locale = if (lang == "ru") Locale("ru", "RU") else Locale.US
                        val formatter = NumberFormat.getIntegerInstance(locale)
                        Metrics.counter(
                            "coin_shop_purchase_click_total",
                            mapOf("pack" to packId, "currency" to "coins"),
                        )
                        return try {
                            fishing.buyPackageWithCoins(uid, packId)
                            Metrics.counter(
                                "coin_shop_purchase_complete_total",
                                mapOf("pack" to packId, "currency" to "coins"),
                            )
                            logCommandMetric(
                                "coin_buy",
                                mapOf("result" to "purchased", "pack" to packId),
                                source,
                            )
                            val balance = transaction {
                                Users.select { Users.id eq uid }.single()[Users.coins]
                            }
                            val priceText = formatter.format(pack.coinPrice)
                            val balanceText = formatter.format(balance)
                            val header = if (lang == "ru") {
                                "Набор \"${pack.name}\" куплен за 🪙 $priceText. Баланс: $balanceText монет."
                            } else {
                                "Bundle \"${pack.name}\" bought for 🪙 $priceText. Balance: $balanceText coins."
                            }
                            val details = if (pack.items.isEmpty()) {
                                header
                            } else {
                                val lines = pack.items.joinToString("\n") { (lure, qty) ->
                                    "• $lure x$qty"
                                }
                                "$header\n$lines"
                            }
                            trySend(chatId, details, replyToMessageId = replyTo)
                            true
                        } catch (e: FishingService.NotEnoughCoinsException) {
                            Metrics.counter(
                                "coin_shop_purchase_failed_total",
                                mapOf("pack" to packId, "currency" to "coins", "reason" to "insufficient"),
                            )
                            logCommandMetric(
                                "coin_buy",
                                mapOf("result" to "insufficient", "pack" to packId),
                                source,
                            )
                            val requiredText = formatter.format(e.required)
                            val balanceText = formatter.format(e.balance)
                            val reply = if (lang == "ru") {
                                "Недостаточно монет: нужно 🪙 $requiredText, у тебя только 🪙 $balanceText."
                            } else {
                                val coinWord = if (e.balance == 1L) "coin" else "coins"
                                "Not enough coins: 🪙 $requiredText required, you only have 🪙 $balanceText $coinWord."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            true
                        } catch (_: FishingService.CoinPurchaseUnavailableException) {
                            Metrics.counter(
                                "coin_shop_purchase_failed_total",
                                mapOf("pack" to packId, "currency" to "coins", "reason" to "unavailable"),
                            )
                            logCommandMetric(
                                "coin_buy",
                                mapOf("result" to "unavailable", "pack" to packId),
                                source,
                            )
                            val reply = if (lang == "ru") {
                                "Покупка этого набора за монеты временно недоступна."
                            } else {
                                "Buying this bundle with coins is temporarily unavailable."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            true
                        } catch (e: Exception) {
                            Metrics.counter(
                                "coin_shop_purchase_failed_total",
                                mapOf("pack" to packId, "currency" to "coins", "reason" to "error"),
                            )
                            log.error("coin buy failed chatId={} pack={}", chatId, packId, e)
                            logCommandMetric(
                                "coin_buy",
                                mapOf("result" to "error", "pack" to packId),
                                source,
                            )
                            val reply = if (lang == "ru") {
                                "Не удалось купить набор. Попробуй ещё раз позже."
                            } else {
                                "Failed to buy the bundle. Please try again later."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            true
                        }
                    }
                    "/cast" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val knownFish = fishing.caughtFishIds(uid).toMutableSet()
                        performCastSequence(uid, chatId, replyTo, source, lang, knownFish)
                        return true
                    }
                    "/autocast" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        if (!hasAutoSubscription(uid)) {
                            logCommandMetric("autocast", mapOf("result" to "no_subscription"), source)
                            val reply = if (lang == "ru") {
                                "У тебя нет подписки на автоловлю."
                            } else {
                                "You don't have an auto-fishing subscription."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        if (autoCastJobs.containsKey(uid)) {
                            logCommandMetric("autocast", mapOf("result" to "already_running"), source)
                            val reply = if (lang == "ru") {
                                "Автоловля уже запущена."
                            } else {
                                "Auto casting is already running."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val (lureNameRaw, locationNameRaw) = currentSetup(uid)
                        val lureLabel = lureNameRaw?.let { I18n.lure(it, lang) } ?: "—"
                        val locationLabel = locationNameRaw?.let { I18n.location(it, lang) } ?: "—"
                        val privateChatId = from?.id ?: chatId
                        val dmText = if (lang == "ru") {
                            "Запускаю автоловлю: приманка «$lureLabel», локация «$locationLabel»."
                        } else {
                            "Starting auto casting with \"$lureLabel\" at \"$locationLabel\"."
                        }
                        val dmResult = runCatching { bot.sendMessage(privateChatId, dmText) }
                        if (dmResult.isFailure) {
                            log.warn("Failed to start auto casting via DM for uid={}", uid, dmResult.exceptionOrNull())
                            logCommandMetric("autocast", mapOf("result" to "dm_failed"), source)
                            if (chatId != privateChatId) {
                                val warn = if (lang == "ru") {
                                    "Не могу отправить сообщение в личку. Разреши боту писать, отправив /start в личные сообщения."
                                } else {
                                    "I couldn't send you a private message. Please allow messages by sending /start to the bot in private."
                                }
                                trySend(chatId, warn, replyToMessageId = replyTo)
                            } else {
                                val warn = if (lang == "ru") {
                                    "Не удалось отправить сообщение. Попробуй ещё раз позже."
                                } else {
                                    "Failed to send the message. Please try again later."
                                }
                                trySend(chatId, warn, replyToMessageId = replyTo)
                            }
                            return true
                        }
                        val stopHint = if (lang == "ru") {
                            "Остановить автоловлю: /stop_autocast"
                        } else {
                            "Stop auto casting: /stop_autocast"
                        }
                        val state = AutoCastState()
                        val loopJob = autoCastScope.launch {
                            val knownFish = fishing.caughtFishIds(uid).toMutableSet()
                            try {
                                while (isActive && !state.stopRequested.get()) {
                                    if (!hasAutoSubscription(uid)) {
                                        val expired = if (lang == "ru") {
                                            "Подписка на автоловлю закончилась. Автозаброс остановлен."
                                        } else {
                                            "Your auto-fishing subscription has expired. Auto casting stopped."
                                        }
                                        trySend(privateChatId, expired)
                                        logCommandMetric("autocast", mapOf("result" to "expired"), source)
                                        break
                                    }
                                    val (currentLure, currentLocation) = currentSetup(uid)
                                    val lureText = currentLure?.let { I18n.lure(it, lang) } ?: "—"
                                    val locationText = currentLocation?.let { I18n.location(it, lang) } ?: "—"
                                    val startNote = if (lang == "ru") {
                                        "🤖 Автоловля: заброс с приманкой «$lureText» на локации «$locationText»."
                                    } else {
                                        "🤖 Auto casting: casting with \"$lureText\" at \"$locationText\"."
                                    }
                                    val result = performCastSequence(
                                        uid,
                                        privateChatId,
                                        null,
                                        "auto",
                                        lang,
                                        knownFish,
                                        CastFlowOptions(startNotification = startNote, catchFooter = stopHint),
                                    )
                                    if (!result.started) {
                                        val stopMessage = when (result.failureReason) {
                                            "No baits" -> if (lang == "ru") {
                                                "Автоловля остановлена: приманки закончились."
                                            } else {
                                                "Auto casting stopped: you are out of baits."
                                            }
                                            "casting" -> if (lang == "ru") {
                                                "Автоловля остановлена: дождись завершения текущего заброса и попробуй снова."
                                            } else {
                                                "Auto casting stopped: wait for the current cast to finish and try again."
                                            }
                                            else -> if (lang == "ru") {
                                                "Автоловля остановлена из-за ошибки. Попробуй снова."
                                            } else {
                                                "Auto casting stopped because of an error. Please try again."
                                            }
                                        }
                                        trySend(privateChatId, stopMessage)
                                        break
                                    }
                                    state.currentCast = result.completion
                                    result.completion?.join()
                                    state.currentCast = null
                                    if (state.stopRequested.get()) break
                                    delay(3000L)
                                }
                            } finally {
                                if (!state.stopRequested.get()) {
                                    state.currentCast?.cancel()
                                }
                                state.currentCast = null
                                autoCastJobs.remove(uid)
                            }
                        }
                        state.loopJob = loopJob
                        autoCastJobs[uid] = state
                        logCommandMetric("autocast", mapOf("result" to "started"), source)
                        if (chatId != privateChatId) {
                            val ack = if (lang == "ru") {
                                "Запустили автоловлю. Сообщения будут приходить в личку."
                            } else {
                                "Auto casting started. Updates will be sent in private chat."
                            }
                            trySend(chatId, ack, replyToMessageId = replyTo)
                        }
                        return true
                    }
                    "/stop_autocast" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val state = autoCastJobs[uid]
                        return if (state == null) {
                            logCommandMetric("stop_autocast", mapOf("result" to "not_running"), source)
                            val reply = if (lang == "ru") {
                                "Автоловля не запущена."
                            } else {
                                "Auto casting is not running."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            true
                        } else {
                            state.stopRequested.set(true)
                            val reply = if (lang == "ru") {
                                "Автоловля остановлена. Текущий заброс завершится."
                            } else {
                                "Auto casting stopped. The current cast will finish."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            logCommandMetric("stop_autocast", mapOf("result" to "stopped"), source)
                            true
                        }
                    }
                    "/language" -> {
                        val uid = ensureUserId(from) ?: return false
                        var lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value == null) {
                            logCommandMetric("language", mapOf("action" to "show"), source)
                            sendLanguageMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        } else {
                            val normalized = value.lowercase()
                            if (normalized in setOf("ru", "en")) {
                                fishing.setLanguage(uid, normalized)
                                lang = normalized
                                val params = mapOf("language" to normalized, "result" to "success")
                                logCommandMetric("language", params, source)
                                val prefix = if (normalized == "ru") {
                                    "Язык переключен на Русский"
                                } else {
                                    "Language switched to English"
                                }
                                sendLanguageMenu(uid, chatId, lang, prefix, replyTo)
                            } else {
                                val params = mapOf("language" to normalized, "result" to "invalid")
                                logCommandMetric("language", params, source)
                                val prefix = if (lang == "ru") {
                                    "Неизвестный язык. Используйте кнопки ниже."
                                } else {
                                    "Unknown language. Please use the buttons below."
                                }
                                sendLanguageMenu(uid, chatId, lang, prefix, replyTo)
                            }
                        }
                        return true
                    }
                    "/bait" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value == null) {
                            logCommandMetric("bait", mapOf("action" to "show"), source)
                            sendBaitMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        } else {
                            val lureId = value.toLongOrNull()
                            if (lureId == null) {
                                logCommandMetric("bait", mapOf("result" to "invalid", "value" to value), source)
                                val reply = if (lang == "ru") "Неверный формат приманки" else "Invalid bait value"
                                trySend(chatId, reply, replyToMessageId = replyTo)
                            } else {
                                try {
                                    fishing.setLure(uid, lureId)
                                    logCommandMetric(
                                        "bait",
                                        mapOf("result" to "success", "value" to lureId.toString()),
                                        source
                                    )
                                    val prefix = if (lang == "ru") "Приманка обновлена" else "Bait updated"
                                    sendBaitMenu(uid, chatId, lang, prefix, replyTo)
                                } catch (e: Exception) {
                                    val reason = e.message ?: "error"
                                    logCommandMetric(
                                        "bait",
                                        mapOf("result" to "error", "value" to lureId.toString(), "reason" to reason),
                                        source
                                    )
                                    val msg = when (reason) {
                                        "casting" -> if (lang == "ru") "Нельзя менять приманку во время заброса" else "You can't change bait while casting"
                                        "no lure" -> if (lang == "ru") "Эта приманка недоступна" else "This bait is not available"
                                        else -> if (lang == "ru") "Не удалось сменить приманку" else "Failed to change bait"
                                    }
                                    trySend(chatId, msg, replyToMessageId = replyTo)
                                }
                            }
                        }
                        return true
                    }
                    "/rod" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value == null) {
                            logCommandMetric("rod", mapOf("action" to "show"), source)
                            sendRodMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        } else {
                            val rodId = value.toLongOrNull()
                            if (rodId == null) {
                                logCommandMetric("rod", mapOf("result" to "invalid", "value" to value), source)
                                val reply = if (lang == "ru") "Неверное значение удочки" else "Invalid rod value"
                                trySend(chatId, reply, replyToMessageId = replyTo)
                            } else {
                                try {
                                    fishing.setRod(uid, rodId)
                                    logCommandMetric(
                                        "rod",
                                        mapOf("result" to "success", "value" to rodId.toString()),
                                        source,
                                    )
                                    val prefix = if (lang == "ru") "Удочка обновлена" else "Rod updated"
                                    sendRodMenu(uid, chatId, lang, prefix, replyTo)
                                } catch (e: Exception) {
                                    val reason = e.message ?: "error"
                                    logCommandMetric(
                                        "rod",
                                        mapOf("result" to "error", "value" to rodId.toString(), "reason" to reason),
                                        source,
                                    )
                                    val msg = when (reason) {
                                        "casting" -> if (lang == "ru") "Нельзя менять удочку во время заброса" else "You can't change the rod while casting"
                                        "locked" -> if (lang == "ru") "Эта удочка еще недоступна" else "This rod is locked"
                                        "no rod" -> if (lang == "ru") "Эта удочка недоступна" else "You don't have this rod"
                                        else -> if (lang == "ru") "Не удалось сменить удочку" else "Failed to change rod"
                                    }
                                    trySend(chatId, msg, replyToMessageId = replyTo)
                                }
                            }
                        }
                        return true
                    }
                    "/location" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value == null) {
                            logCommandMetric("location", mapOf("action" to "show"), source)
                            sendLocationMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        } else {
                            val locId = value.toLongOrNull()
                            if (locId == null) {
                                logCommandMetric("location", mapOf("result" to "invalid", "value" to value), source)
                                val reply = if (lang == "ru") "Неверный формат локации" else "Invalid location value"
                                trySend(chatId, reply, replyToMessageId = replyTo)
                            } else {
                                try {
                                    fishing.setLocation(uid, locId)
                                    logCommandMetric(
                                        "location",
                                        mapOf("result" to "success", "value" to locId.toString()),
                                        source
                                    )
                                    val prefix = if (lang == "ru") "Локация обновлена" else "Location updated"
                                    sendLocationMenu(uid, chatId, lang, prefix, replyTo)
                                } catch (e: Exception) {
                                    val reason = e.message ?: "error"
                                    logCommandMetric(
                                        "location",
                                        mapOf("result" to "error", "value" to locId.toString(), "reason" to reason),
                                        source
                                    )
                                    val msg = when (reason) {
                                        "casting" -> if (lang == "ru") "Нельзя менять локацию во время заброса" else "You can't change location while casting"
                                        "locked" -> if (lang == "ru") "Локация еще недоступна" else "Location is still locked"
                                        "bad location" -> if (lang == "ru") "Локация не найдена" else "Location not found"
                                        else -> if (lang == "ru") "Не удалось сменить локацию" else "Failed to change location"
                                    }
                                    trySend(chatId, msg, replyToMessageId = replyTo)
                                }
                            }
                        }
                        return true
                    }
                    "/nickname" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        if (arg.isNullOrBlank()) {
                            logCommandMetric("nickname", mapOf("result" to "missing_arg"), source)
                            val currentLabel = fishing.displayName(uid)
                                ?.takeIf { it.isNotBlank() }
                                ?: if (lang == "ru") "не установлен" else "not set"
                            val reply = if (lang == "ru") {
                                "Текущий ник: \"$currentLabel\".\nУкажи новый ник после команды, например: /nickname Рыбак"
                            } else {
                                "Current nickname: \"$currentLabel\".\nProvide a new nickname after the command, e.g. /nickname Fisher"
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val sanitized = sanitizeName(arg).replace('\n', ' ').trim()
                        if (sanitized.isEmpty()) {
                            logCommandMetric("nickname", mapOf("result" to "invalid"), source)
                            val reply = if (lang == "ru") {
                                "Ник не может быть пустым и должен быть короче 30 символов."
                            } else {
                                "Nickname can't be empty and must be under 30 characters."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val currentNickname = transaction {
                            Users.select { Users.id eq uid }.single()[Users.nickname]
                        }
                        if (currentNickname != null && currentNickname == sanitized) {
                            logCommandMetric("nickname", mapOf("result" to "same"), source)
                            val reply = if (lang == "ru") {
                                "Этот ник уже установлен."
                            } else {
                                "This nickname is already set."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val currentLabel = fishing.displayName(uid)
                            ?.takeIf { it.isNotBlank() }
                            ?: if (lang == "ru") "не установлен" else "not set"
                        val confirmText = if (lang == "ru") {
                            "Вы хотите изменить имя с \"$currentLabel\" на \"$sanitized\"?"
                        } else {
                            "Do you want to change your name from \"$currentLabel\" to \"$sanitized\"?"
                        }
                        val prompt = if (lang == "ru") {
                            "$confirmText\nПодтвердите действие кнопкой ниже."
                        } else {
                            "$confirmText\nConfirm the action using the buttons below."
                        }
                        val confirmLabel = if (lang == "ru") "✅ Да" else "✅ Yes"
                        val cancelLabel = if (lang == "ru") "❌ Нет" else "❌ No"
                        val markup = Json.encodeToString(
                            InlineKeyboardMarkup(
                                listOf(
                                    listOf(InlineKeyboardButton(confirmLabel, "/nickname_confirm ${ownedData(uid, sanitized)}")),
                                    listOf(InlineKeyboardButton(cancelLabel, "/nickname_cancel ${ownedData(uid, "cancel")}")),
                                )
                            )
                        )
                        logCommandMetric("nickname", mapOf("result" to "prompt"), source)
                        trySend(chatId, prompt, markup, replyTo)
                        return true
                    }
                    "/nickname_confirm" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        val newName = value?.let { sanitizeName(it).replace('\n', ' ').trim() } ?: ""
                        if (newName.isEmpty()) {
                            logCommandMetric("nickname", mapOf("result" to "invalid_confirm"), source)
                            val reply = if (lang == "ru") {
                                "Не удалось изменить ник. Попробуй ещё раз."
                            } else {
                                "Couldn't update the nickname. Please try again."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val updatedNickname = fishing.setNickname(uid, newName)
                        logCommandMetric("nickname", mapOf("result" to "updated"), source)
                        val reply = if (lang == "ru") {
                            "Ник изменён на \"$updatedNickname\"."
                        } else {
                            "Nickname changed to \"$updatedNickname\"."
                        }
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/nickname_cancel" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (_, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        logCommandMetric("nickname", mapOf("result" to "cancel"), source)
                        val reply = if (lang == "ru") {
                            "Изменение ника отменено."
                        } else {
                            "Nickname change cancelled."
                        }
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/prizeclaim" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        val prizeId = value?.toLongOrNull()
                        if (prizeId == null) {
                            logCommandMetric("prizes_claim", mapOf("result" to "invalid_id"), source)
                            val reply = if (lang == "ru") {
                                "Неверный идентификатор приза."
                            } else {
                                "Invalid prize identifier."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val pending = prizeService.pendingPrizes(uid)
                        val prize = pending.find { it.id == prizeId }
                        if (prize == null) {
                            logCommandMetric("prizes_claim", mapOf("result" to "not_found"), source)
                            val reply = if (lang == "ru") {
                                "Приз не найден или уже получен."
                            } else {
                                "Prize not found or already claimed."
                            }
                            sendPrizes(
                                uid,
                                chatId,
                                lang,
                                prizes = pending,
                                prefix = reply,
                                replyToMessageId = replyTo,
                                showWhenEmpty = true,
                            )
                            return true
                        }
                        val isCoins = prize.packageId == COIN_PRIZE_ID
                        val pack = if (isCoins) null else fishing.findPack(prize.packageId)
                        val packNames = fishing.listShop(lang).flatMap { it.packs }.associate { it.id to it.name }
                        val displayName = if (isCoins) {
                            if (lang == "ru") "Монеты" else "Coins"
                        } else {
                            packNames[prize.packageId]
                                ?: pack?.name?.let { I18n.text(it, lang) }
                                ?: prize.packageId.replace('_', ' ')
                        }
                        val success = try {
                            prizeService.claimPrize(uid, prizeId, fishing)
                            logCommandMetric(
                                "prizes_claim",
                                mapOf(
                                    "result" to "claimed",
                                    "pack" to (if (isCoins) COIN_PRIZE_ID else prize.packageId),
                                    "qty" to prize.qty.toString()
                                ),
                                source
                            )
                            if (isCoins) {
                                val amount = prize.coins ?: prize.qty
                                if (lang == "ru") {
                                    "Начислено $amount монет."
                                } else {
                                    "$amount coins added."
                                }
                            } else {
                                val items = pack?.items.orEmpty()
                                val header = if (lang == "ru") {
                                    val label = if (prize.qty > 1) "$displayName x${prize.qty}" else displayName
                                    "Приз \"$label\" получен."
                                } else {
                                    val label = if (prize.qty > 1) "$displayName x${prize.qty}" else displayName
                                    "Prize \"$label\" claimed."
                                }
                                if (items.isEmpty()) {
                                    header
                                } else {
                                    val lines = items.joinToString("\n") { (lure, qty) ->
                                        val lureName = I18n.lure(lure, lang)
                                        val total = qty * prize.qty
                                        "• $lureName x$total"
                                    }
                                    "$header\n$lines"
                                }
                            }
                        } catch (e: Exception) {
                            logCommandMetric(
                                "prizes_claim",
                                mapOf("result" to "error", "pack" to (if (isCoins) COIN_PRIZE_ID else prize.packageId)),
                                source
                            )
                            if (lang == "ru") {
                                "Не удалось получить приз. Попробуй ещё раз позже."
                            } else {
                                "Couldn't claim the prize. Please try again later."
                            }
                        }
                        sendPrizes(
                            uid,
                            chatId,
                            lang,
                            prefix = success,
                            replyToMessageId = replyTo,
                            showWhenEmpty = true,
                        )
                        return true
                    }
                }
                return false
            }

            update.callbackQuery?.let { cq ->
                val data = cq.data
                val chatId = cq.message?.chat?.id ?: cq.from.id
                if (data != null && processUserCommand(
                        data,
                        cq.from,
                        chatId,
                        isCallback = true,
                        messageId = cq.message?.message_id,
                    )
                ) {
                    try { bot.answerCallbackQuery(cq.id) } catch (_: Exception) {}
                    return@post call.respond(HttpStatusCode.OK)
                }
                if (cq.from.id == env.adminTgId) {
                    try { bot.answerCallbackQuery(cq.id) } catch (_: Exception) {}
                    val data = cq.data
                    val target = cq.message?.chat?.id ?: cq.from.id
                    when {
                        data == "create_tournament" -> {
                            adminStates[cq.from.id] = AdminDraft()
                            try {
                                bot.sendMessage(target, "Введите название турнира на русском")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", target, e)
                            }
                        }
                        data == "discounts_menu" -> {
                            sendDiscountMenu(target)
                        }
                        data == "create_discount" -> {
                            discountStates[cq.from.id] = DiscountDraft(step = DiscountStep.PACK)
                            sendDiscountPackPicker(target)
                        }
                        data == "discount_cancel" -> {
                            discountStates.remove(cq.from.id)
                        }
                        data != null && data.startsWith("discount_pack_") -> {
                            val id = data.removePrefix("discount_pack_")
                            val pack = fishing.findPack(id)
                            if (pack != null) {
                                val name = I18n.text(pack.name, "ru")
                                val existing = fishing.getDiscount(id)
                                val draft = DiscountDraft(
                                    step = DiscountStep.PRICE,
                                    packageId = id,
                                    packageName = name,
                                    basePrice = pack.price,
                                    price = existing?.price,
                                    start = existing?.startDate,
                                    end = existing?.endDate,
                                )
                                discountStates[cq.from.id] = draft
                                val current = existing?.let {
                                    val start = it.startDate.format(DATE_FMT)
                                    val end = it.endDate.format(DATE_FMT)
                                    "\nТекущая скидка: ${it.price}⭐ ($start — до $end)"
                                } ?: ""
                                val message = "Введите цену в звёздах для \"$name\" (обычная цена: ${pack.price}⭐)$current"
                                try {
                                    bot.sendMessage(target, message)
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", target, e)
                                }
                            }
                        }
                        data != null && data.startsWith("prize_pack_") -> {
                            val draft = adminStates[cq.from.id]
                            if (draft != null && draft.step == AdminStep.PRIZE) {
                                val id = data.removePrefix("prize_pack_")
                                val names = packNamesRu()
                                val label = names[id] ?: id
                                draft.pendingPrize = PrizeSpec(id, 1)
                                draft.awaitingPrizeQuantity = true
                                promptPrizeQuantity(target, label, false)
                            }
                        }
                        data == "prize_coins" -> {
                            val draft = adminStates[cq.from.id]
                            if (draft != null && draft.step == AdminStep.PRIZE) {
                                draft.pendingPrize = PrizeSpec(COIN_PRIZE_ID, 0, 0)
                                draft.awaitingPrizeQuantity = true
                                promptPrizeQuantity(target, "Монеты", true)
                            }
                        }
                        data == "prize_skip" -> {
                            val draft = adminStates[cq.from.id]
                            if (draft != null && draft.step == AdminStep.PRIZE) {
                                draft.pendingPrize = null
                                draft.awaitingPrizeQuantity = false
                                storePrize(draft, PrizeSpec())
                                proceedToNextPrize(target, cq.from.id, draft)
                            }
                        }
                        data != null && data.startsWith("remove_discount_") -> {
                            val id = data.removePrefix("remove_discount_")
                            val removed = fishing.removeDiscount(id) > 0
                            val packName = fishing.findPack(id)?.let { I18n.text(it.name, "ru") } ?: id
                            val reply = if (removed) {
                                "Скидка для \"$packName\" удалена"
                            } else {
                                "Скидка для \"$packName\" не найдена"
                            }
                            discountStates.remove(cq.from.id)
                            try {
                                bot.sendMessage(target, reply)
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", target, e)
                            }
                            sendDiscountMenu(target)
                        }
                        data == "list_tournaments" -> {
                            val current = tournaments.currentTournament()
                            val upcoming = tournaments.upcomingTournaments()
                            val past = tournaments.pastTournaments(10)
                            val list = buildList {
                                if (current != null) add(current)
                                addAll(upcoming)
                                addAll(past)
                            }
                            if (list.isEmpty()) {
                                try { bot.sendMessage(target, "Турниров нет") } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                            } else {
                                val buttons = list.map { t ->
                                    listOf(InlineKeyboardButton(t.nameRu, "tournament_${t.id}"))
                                }
                                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                                try { bot.sendMessage(target, "Турниры", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                            }
                        }
                        data == "broadcast_message" -> {
                            broadcastStates[cq.from.id] = BroadcastDraft()
                            try { bot.sendMessage(target, "Введите текст на русском") } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                        }
                        data != null && data.startsWith("tournament_") -> {
                            val id = data.removePrefix("tournament_").toLongOrNull()
                            if (id != null) {
                                val t = tournaments.getTournament(id)
                                if (t != null) {
                                    val markup = """{"inline_keyboard":[[{"text":"Редактировать","callback_data":"edit_tournament_$id"},{"text":"Удалить","callback_data":"delete_tournament_$id"}]]}"""
                                    try { bot.sendMessage(target, "Турнир: ${t.nameRu}", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                                }
                            }
                        }
                        data != null && data.startsWith("delete_tournament_") -> {
                            val id = data.removePrefix("delete_tournament_").toLongOrNull()
                            if (id != null) {
                                try { tournaments.deleteTournament(id); bot.sendMessage(target, "Турнир удален") } catch (e: Exception) { log.error("deleteTournament failed", e) }
                            }
                        }
                        data != null && data.startsWith("edit_tournament_") -> {
                            val id = data.removePrefix("edit_tournament_").toLongOrNull()
                            if (id != null) {
                                val t = tournaments.getTournament(id)
                                if (t != null) {
                                    adminStates[cq.from.id] = AdminDraft(
                                        id = id,
                                        nameRu = t.nameRu,
                                        nameEn = t.nameEn,
                                        start = t.startTime,
                                        end = t.endTime,
                                        fish = t.fish,
                                        location = t.location,
                                        metric = t.metric,
                                        prizePlaces = t.prizePlaces,
                                        prizes = parsePrizes(t.prizesJson),
                                    )
                                    try { bot.sendMessage(target, "Введите название турнира на русском (сейчас: ${t.nameRu})") } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                                }
                            }
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            val message = update.message ?: return@post call.respond(HttpStatusCode.OK)

            message.successfulPayment?.let { sp ->
                val chatId = message.chat.id
                val userId = message.from?.id
                if (userId == null) {
                    log.warn("successfulPayment with no user id: chatId={} payload={}", chatId, sp.invoicePayload)
                    return@post call.respond(HttpStatusCode.OK)
                }
                val rawPayload = sp.invoicePayload
                val packId = parseInvoicePayload(rawPayload, userId)
                if (packId == null) {
                    val parts = rawPayload.split(';').mapNotNull {
                        val p = it.split('=', limit = 2)
                        if (p.size == 2) p[0] to p[1] else null
                    }.toMap()
                    val payloadUser = parts["user"]?.toLongOrNull()
                    when {
                        parts["pack"] == null ->
                            log.warn("successfulPayment with no packId: chatId={} payload={}", chatId, rawPayload)
                        payloadUser != null && payloadUser != userId ->
                            log.warn(
                                "successfulPayment payload user mismatch: userId={} payloadUser={} payload={}",
                                userId,
                                payloadUser,
                                rawPayload
                            )
                    }
                } else {
                    val uid = fishing.ensureUserByTgId(userId)
                    val purchaseSucceeded = try {
                        fishing.buyPackage(uid, packId)
                        true
                    } catch (e: Exception) {
                        Metrics.counter(
                            "shop_purchase_failed_total",
                            mapOf("pack" to packId, "currency" to "stars", "reason" to "buy_error"),
                        )
                        log.error("buyPackage failed uid={} packId={}", uid, packId, e)
                        false
                    }
                    try {
                        PayService.recordPayment(
                            uid,
                            packId,
                            PayService.PaymentInfo(
                                providerChargeId = sp.providerPaymentChargeId,
                                telegramChargeId = sp.telegramPaymentChargeId,
                                amount = sp.totalAmount,
                                currency = sp.currency,
                            )
                        )
                    } catch (e: Exception) {
                        log.error("recordPayment failed uid={} packId={}", uid, packId, e)
                    }
                    try {
                        fishing.findPack(packId)?.let { pack ->
                            ReferralService.onPurchase(uid, pack)
                        }
                    } catch (e: Exception) {
                        log.error("referral reward failed uid={} packId={}", uid, packId, e)
                    }
                    if (purchaseSucceeded) {
                        Metrics.counter(
                            "shop_purchase_complete_total",
                            mapOf("pack" to packId, "currency" to "stars"),
                        )
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            val chatId = message.chat.id
            val userId = message.from?.id ?: return@post call.respond(HttpStatusCode.OK)
            val text = message.text ?: ""
            val commandSource = if (message.viaBot != null) "inline" else "message"

            discountStates[userId]?.let { draft ->
                if (text == "/cancel") {
                    discountStates.remove(userId)
                    try {
                        bot.sendMessage(chatId, "Настройка скидки отменена")
                    } catch (e: Exception) {
                        log.error("sendMessage failed chatId={}", chatId, e)
                    }
                    return@post call.respond(HttpStatusCode.OK)
                }
                when (draft.step) {
                    DiscountStep.PACK -> {
                        try {
                            bot.sendMessage(chatId, "Выберите товар кнопкой ниже")
                        } catch (_: Exception) {}
                    }
                    DiscountStep.PRICE -> {
                        val value = text.trim().toIntOrNull()
                        if (value == null || value <= 0) {
                            try {
                                bot.sendMessage(chatId, "Укажите цену числом")
                            } catch (_: Exception) {}
                        } else {
                            draft.price = value
                            draft.step = DiscountStep.START
                            val current = draft.start?.format(DATE_FMT)?.let { " (сейчас: $it)" } ?: ""
                            try {
                                bot.sendMessage(chatId, "Дата начала скидки (дд.мм.гггг)$current")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", chatId, e)
                            }
                        }
                    }
                    DiscountStep.START -> {
                        val date = runCatching { LocalDate.parse(text.trim(), DATE_FMT) }.getOrNull()
                        if (date == null) {
                            try {
                                bot.sendMessage(chatId, "Неверный формат даты, используйте дд.мм.гггг")
                            } catch (_: Exception) {}
                        } else {
                            draft.start = date
                            draft.step = DiscountStep.END
                            val current = draft.end?.format(DATE_FMT)?.let { " (сейчас: $it)" } ?: ""
                            try {
                                bot.sendMessage(chatId, "Дата окончания скидки (дд.мм.гггг)$current")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", chatId, e)
                            }
                        }
                    }
                    DiscountStep.END -> {
                        val date = runCatching { LocalDate.parse(text.trim(), DATE_FMT) }.getOrNull()
                        val start = draft.start
                        val price = draft.price
                        if (date == null || start == null || price == null) {
                            try {
                                bot.sendMessage(chatId, "Неверный формат даты, используйте дд.мм.гггг")
                            } catch (_: Exception) {}
                        } else if (!date.isAfter(start)) {
                            try {
                                bot.sendMessage(chatId, "Дата окончания должна быть позже даты начала")
                            } catch (_: Exception) {}
                        } else {
                            draft.end = date
                            fishing.setDiscount(draft.packageId, price, start, date)
                            val startStr = start.format(DATE_FMT)
                            val endStr = date.format(DATE_FMT)
                            val messageText = buildString {
                                append("Скидка для \"${draft.packageName}\" сохранена: ")
                                append(strikethrough("${draft.basePrice}⭐"))
                                append(" → ${price}⭐ ($startStr — до $endStr)")
                            }
                            discountStates.remove(userId)
                            try {
                                bot.sendMessage(chatId, messageText)
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", chatId, e)
                            }
                            sendDiscountMenu(chatId)
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            adminStates[userId]?.let { draft ->
                when (draft.step) {
                    AdminStep.NAME_RU -> {
                        draft.nameRu = text
                        draft.step = AdminStep.NAME_EN
                        val current = draft.nameEn.takeIf { it.isNotBlank() }?.let { " (сейчас: $it)" } ?: ""
                        try { bot.sendMessage(chatId, "Название турнира на английском$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.NAME_EN -> {
                        draft.nameEn = text
                        draft.step = AdminStep.START
                        val current = draft.start?.atZone(ZoneOffset.UTC)?.format(DATE_FMT)?.let { " (сейчас: $it)" } ?: ""
                        try { bot.sendMessage(chatId, "Дата начала (дд.мм.гггг)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.START -> {
                        val d = runCatching { LocalDate.parse(text, DATE_FMT) }.getOrNull()
                        if (d != null) {
                            draft.start = d.atStartOfDay(ZoneOffset.UTC).toInstant()
                            draft.step = AdminStep.END
                            val current = draft.end?.atZone(ZoneOffset.UTC)?.format(DATE_FMT)?.let { " (сейчас: $it)" } ?: ""
                            try { bot.sendMessage(chatId, "Дата окончания (дд.мм.гггг)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try { bot.sendMessage(chatId, "Неверный ввод, повторите") } catch (_: Exception) {}
                        }
                    }
                    AdminStep.END -> {
                        val d = runCatching { LocalDate.parse(text, DATE_FMT) }.getOrNull()
                        if (d != null) {
                            draft.end = d.atStartOfDay(ZoneOffset.UTC).toInstant()
                            draft.step = AdminStep.FISH
                            val current = draft.fish?.let { " (сейчас: $it)" } ?: ""
                            try { bot.sendMessage(chatId, "Введите рыбу на русском или группу (common/mythic/legendary и т.д.) (или '-' для любой)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try { bot.sendMessage(chatId, "Неверный ввод, повторите") } catch (_: Exception) {}
                        }
                    }
                    AdminStep.FISH -> {
                        val v = text.trim()
                        draft.fish = v.takeIf { it.isNotEmpty() && it != "-" }
                        draft.step = AdminStep.LOCATION
                        val current = draft.location?.let { " (сейчас: $it)" } ?: ""
                        try { bot.sendMessage(chatId, "Локация на русском (или '-' для любой)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.LOCATION -> {
                        val v = text.trim()
                        draft.location = v.takeIf { it.isNotEmpty() && it != "-" }
                        draft.step = AdminStep.METRIC
                        val current = if (draft.metric.isNotBlank()) " (сейчас: ${draft.metric})" else ""
                        try {
                            bot.sendMessage(chatId, "Метрика (largest/smallest/count/total_weight)$current", METRIC_KEYBOARD)
                        } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.METRIC -> {
                        val m = text.lowercase()
                        if (m in METRIC_OPTIONS) {
                            draft.metric = m
                            draft.step = AdminStep.PRIZE_PLACES
                            val current = if (draft.prizePlaces != 0) " (сейчас: ${draft.prizePlaces})" else ""
                            try {
                                bot.sendMessage(chatId, "Количество призовых мест$current", REMOVE_KEYBOARD)
                            } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try {
                                bot.sendMessage(chatId, "Неверная метрика, выберите largest, smallest, count или total_weight", METRIC_KEYBOARD)
                            } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        }
                    }
                    AdminStep.PRIZE_PLACES -> {
                        val n = text.toIntOrNull()
                        if (n != null) {
                            draft.prizePlaces = n
                            draft.prizes = MutableList(n) { i -> draft.prizes.getOrNull(i) ?: PrizeSpec() }
                            draft.currentPrize = 1
                            draft.step = AdminStep.PRIZE
                            sendPrizePrompt(chatId, draft)
                        } else {
                            try { bot.sendMessage(chatId, "Неверный ввод, повторите") } catch (_: Exception) {}
                        }
                    }
                    AdminStep.PRIZE -> {
                        if (draft.awaitingPrizeQuantity && draft.pendingPrize != null) {
                            val amount = text.trim().toIntOrNull()
                            if (amount == null || amount <= 0) {
                                try { bot.sendMessage(chatId, "Неверный ввод, укажите положительное число") } catch (_: Exception) {}
                                return@post call.respond(HttpStatusCode.OK)
                            }
                            val pending = draft.pendingPrize!!
                            val prize = if (pending.pack == COIN_PRIZE_ID) {
                                pending.copy(qty = amount, coins = amount)
                            } else {
                                pending.copy(qty = amount)
                            }
                            storePrize(draft, prize)
                            draft.pendingPrize = null
                            draft.awaitingPrizeQuantity = false
                            proceedToNextPrize(chatId, userId, draft)
                        } else {
                            val parts = text.trim().split(' ', ':')
                            val pack = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                            val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
                            if (pack == null || qty <= 0) {
                                try { bot.sendMessage(chatId, "Неверный ввод, укажите приз как 'pack qty'") } catch (_: Exception) {}
                                return@post call.respond(HttpStatusCode.OK)
                            }
                            val normalized = if (pack.equals(COIN_PRIZE_ID, ignoreCase = true)) COIN_PRIZE_ID else pack
                            val prize = if (normalized == COIN_PRIZE_ID) {
                                PrizeSpec(normalized, qty, qty)
                            } else {
                                PrizeSpec(normalized, qty)
                            }
                            storePrize(draft, prize)
                            proceedToNextPrize(chatId, userId, draft)
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            broadcastStates[userId]?.let { draft ->
                when (draft.step) {
                    BroadcastStep.TEXT_RU -> {
                        draft.textRu = text
                        draft.step = BroadcastStep.TEXT_EN
                        try { bot.sendMessage(chatId, "Введите текст на английском") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    BroadcastStep.TEXT_EN -> {
                        draft.textEn = text
                        broadcastStates.remove(userId)
                        try { bot.sendMessage(chatId, "Начинаю рассылку") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        val users = transaction {
                            Users.slice(Users.tgId, Users.language).selectAll().map { it[Users.tgId] to it[Users.language] }
                        }
                        broadcastScope.launch {
                            users.forEachIndexed { index, (uid, lang) ->
                                val msg = when (lang) {
                                    "ru" -> draft.textRu
                                    "en" -> draft.textEn
                                    else -> draft.textRu + "\n" + draft.textEn
                                }
                                try {
                                    bot.sendMessage(uid, msg)
                                } catch (e: TelegramApiException) {
                                    when (e.code) {
                                        403 -> log.warn("User {} blocked bot; skipping broadcast", uid)
                                        400, 404 -> log.warn("Failed to deliver to {}: {}", uid, e.message)
                                        else -> log.error(
                                            "sendMessage failed chatId={} code={} message={}",
                                            uid,
                                            e.code,
                                            e.message,
                                            e
                                        )
                                    }
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", uid, e)
                                }
                                if (index < users.lastIndex) {
                                    delay(5_000L)
                                }
                            }
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            if (processUserCommand(
                    text,
                    message.from,
                    chatId,
                    messageId = message.message_id,
                    sourceOverride = commandSource,
                )
            ) {
                return@post call.respond(HttpStatusCode.OK)
            } else if (text.startsWith("/paysupport")) {
                val args = text.removePrefix("/paysupport").trim()
                val uid = fishing.ensureUserByTgId(userId)
                val lang = fishing.userLanguage(uid)
                val payments = PayService.listPayments(uid)
                val packNames = fishing.listShop(lang).flatMap { it.packs }.associate { it.id to it.name }
                val source = commandSource
                if (args.isEmpty()) {
                    if (payments.isEmpty()) {
                        val reply = if (lang == "ru") {
                            "Покупок не найдено"
                        } else {
                            "No purchases found"
                        }
                        logCommandMetric("paysupport", mapOf("result" to "empty"), source)
                        trySend(chatId, reply)
                    } else {
                        val list = payments.joinToString("\n") {
                            val label = packNames[it.packageId] ?: it.packageId
                            "${it.id}: $label — ${it.amount} ${it.currency}"
                        }
                        val reply = if (lang == "ru") {
                            "Выберите покупку для возврата, отправив /paysupport <ID> <причина>:\n$list"
                        } else {
                            "Choose a payment for refund by sending /paysupport <ID> <reason>:\n$list"
                        }
                        logCommandMetric("paysupport", mapOf("action" to "list"), source)
                        trySend(chatId, reply)
                    }
                } else {
                    val parts = args.split(" ", limit = 2)
                    val paymentId = parts.getOrNull(0)?.toLongOrNull()
                    val reason = parts.getOrNull(1)?.trim() ?: ""
                    if (paymentId == null) {
                        val reply = if (lang == "ru") {
                            "Неверный формат. Используйте /paysupport <ID> <причина>."
                        } else {
                            "Invalid format. Use /paysupport <ID> <reason>."
                        }
                        logCommandMetric("paysupport", mapOf("result" to "invalid_args"), source)
                        trySend(chatId, reply)
                    } else {
                        val payment = payments.find { it.id == paymentId }
                        if (payment != null) {
                            val reqId = PayService.createSupportRequest(uid, paymentId, reason)
                            val reply = if (lang == "ru") {
                                "Запрос #$reqId отправлен администрации"
                            } else {
                                "Request #$reqId has been sent to the admins"
                            }
                            logCommandMetric("paysupport", mapOf("result" to "submitted"), source)
                            trySend(chatId, reply)
                            if (env.adminTgId != 0L) {
                                val msg = "Запрос #$reqId от $chatId, платеж $paymentId: $reason\n" +
                                        "/refund $reqId — одобрить возврат\n" +
                                        "/reject $reqId <причина> — отклонить\n" +
                                        "/ask $reqId <вопрос> — запросить информацию"
                                try {
                                    bot.sendMessage(env.adminTgId, msg)
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", env.adminTgId, e)
                                }
                            }
                        } else {
                            val reply = if (lang == "ru") {
                                "Покупка не найдена"
                            } else {
                                "Purchase not found"
                            }
                            logCommandMetric("paysupport", mapOf("result" to "not_found"), source)
                            trySend(chatId, reply)
                        }
                    }
                }
            } else if (text.startsWith("/answer")) {
                val parts = text.split(" ", limit = 3)
                val uid = fishing.ensureUserByTgId(userId)
                val lang = fishing.userLanguage(uid)
                val source = commandSource
                var id = parts.getOrNull(1)?.toLongOrNull()
                val answer: String? = if (id != null) {
                    parts.getOrNull(2)
                } else {
                    id = PayService.latestInfoRequest(uid)?.id
                    parts.getOrNull(1)
                }
                if (id == null || answer.isNullOrBlank()) {
                    val reply = if (lang == "ru") {
                        "Укажи запрос и ответ: /answer <ID> <ответ>"
                    } else {
                        "Provide a request and reply: /answer <ID> <message>"
                    }
                    logCommandMetric("answer", mapOf("result" to "invalid_args"), source)
                    trySend(chatId, reply)
                } else {
                    val reqId = id!!
                    val req = PayService.findSupportRequest(reqId)
                    if (req != null && req.userId == uid) {
                        PayService.updateSupportRequest(reqId, "pending", answer)
                        if (env.adminTgId != 0L) {
                            try {
                                bot.sendMessage(
                                    env.adminTgId,
                                    "Ответ по запросу #$reqId от $chatId: $answer\n" +
                                            "/refund $reqId — одобрить возврат\n" +
                                            "/reject $reqId <причина> — отклонить\n" +
                                            "/ask $reqId <вопрос> — запросить информацию"
                                )
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", env.adminTgId, e)
                            }
                        }
                        val reply = if (lang == "ru") {
                            "Ответ отправлен администрации"
                        } else {
                            "Your reply has been sent to the admins"
                        }
                        logCommandMetric("answer", mapOf("result" to "sent"), source)
                        trySend(chatId, reply)
                    } else {
                        val reply = if (lang == "ru") {
                            "Запрос не найден"
                        } else {
                            "Request not found"
                        }
                        logCommandMetric("answer", mapOf("result" to "not_found"), source)
                        trySend(chatId, reply)
                    }
                }
            } else if (chatId == env.adminTgId) {
                when {
                    text.startsWith("/admin") -> {
                        val markup = Json.encodeToString(
                            InlineKeyboardMarkup(
                                listOf(
                                    listOf(
                                        InlineKeyboardButton("Создать турнир", "create_tournament"),
                                        InlineKeyboardButton("Список турниров", "list_tournaments"),
                                    ),
                                    listOf(
                                        InlineKeyboardButton("Разослать сообщение", "broadcast_message"),
                                        InlineKeyboardButton("Скидки магазина", "discounts_menu"),
                                    ),
                                )
                            )
                        )
                        try { bot.sendMessage(chatId, "Админ меню", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        discountStates.remove(userId)
                    }
                    text.startsWith("/refund") -> {
                        val id = text.split(" ").getOrNull(1)?.toLongOrNull()
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                val payment = req.paymentId?.let { PayService.findPayment(it) }
                                    ?: PayService.latestPayment(req.userId)
                                val tgId = fishing.userTgId(req.userId)
                                if (payment != null && tgId != null) {
                                    try {
                                        stars.refundStars(tgId, payment.telegramChargeId)
                                        PayService.markPaymentRefunded(payment.id)
                                        fishing.removeAutoFishMonth(req.userId)
                                        PayService.updateSupportRequest(id, "refunded", null)
                                        try {
                                            bot.sendMessage(tgId, "Ваш запрос #$id одобрен, возврат будет выполнен")
                                        } catch (e: Exception) {
                                            log.error("sendMessage failed chatId={}", tgId, e)
                                        }
                                    } catch (e: Exception) {
                                        log.error(
                                            "refund failed userId={} chargeId={}",
                                            req.userId,
                                            payment.telegramChargeId,
                                            e
                                        )
                                    }
                                } else {
                                    log.warn(
                                        "No payment or tgId found for refund userId={} requestId={}",
                                        req.userId,
                                        id
                                    )
                                }
                            }
                        }
                    }
                    text.startsWith("/reject") -> {
                        val parts = text.split(" ", limit = 3)
                        val id = parts.getOrNull(1)?.toLongOrNull()
                        val reason = parts.getOrNull(2) ?: "Причина не указана"
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                PayService.updateSupportRequest(id, "rejected", reason)
                                val tgId = fishing.userTgId(req.userId)
                                if (tgId != null) {
                                    try {
                                        bot.sendMessage(tgId, "Запрос #$id отклонен: $reason")
                                    } catch (e: Exception) {
                                        log.error("sendMessage failed chatId={}", tgId, e)
                                    }
                                } else {
                                    log.warn("No tgId found for userId={} requestId={}", req.userId, id)
                                }
                            }
                        }
                    }
                    text.startsWith("/ask") -> {
                        val parts = text.split(" ", limit = 3)
                        val id = parts.getOrNull(1)?.toLongOrNull()
                        val question = parts.getOrNull(2) ?: return@post call.respond(HttpStatusCode.OK)
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                PayService.updateSupportRequest(id, "info", question)
                                val tgId = fishing.userTgId(req.userId)
                                if (tgId != null) {
                                    try {
                                        bot.sendMessage(
                                            tgId,
                                            "Админ уточняет по запросу #$id: $question\n" +
                                                    "Ответьте командой /answer $id <ответ>"
                                        )
                                    } catch (e: Exception) {
                                        log.error("sendMessage failed chatId={}", tgId, e)
                                    }
                                } else {
                                    log.warn("No tgId found for userId={} requestId={}", req.userId, id)
                                }
                            }
                        }
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
private data class TgUpdate(
    val message: TgMessage? = null,
    @SerialName("inline_query") val inlineQuery: TgInlineQuery? = null,
    @SerialName("pre_checkout_query") val preCheckoutQuery: TgPreCheckoutQuery? = null,
    @SerialName("callback_query") val callbackQuery: TgCallbackQuery? = null,
)

@Serializable
private data class TgMessage(
    val message_id: Long,
    val chat: TgChat,
    val from: TgUser? = null,
    val text: String? = null,
    @SerialName("successful_payment") val successfulPayment: TgSuccessfulPayment? = null,
    @SerialName("via_bot") val viaBot: TgUser? = null,
)

@Serializable
private data class TgPreCheckoutQuery(val id: String)

@Serializable
private data class TgSuccessfulPayment(
    @SerialName("telegram_payment_charge_id") val telegramPaymentChargeId: String,
    @SerialName("provider_payment_charge_id") val providerPaymentChargeId: String? = null,
    @SerialName("total_amount") val totalAmount: Int,
    val currency: String,
    @SerialName("invoice_payload") val invoicePayload: String,
)

@Serializable
private data class TgCallbackQuery(
    val id: String,
    val from: TgUser,
    val message: TgMessage? = null,
    val data: String? = null,
)

@Serializable
private data class TgInlineQuery(
    val id: String,
    val from: TgUser,
    val query: String,
)

@Serializable
private data class TgChat(val id: Long)

@Serializable
private data class TgUser(
    val id: Long,
    @SerialName("first_name") val first_name: String? = null,
    @SerialName("last_name") val last_name: String? = null,
    val username: String? = null,
    @SerialName("language_code") val language_code: String? = null,
)
