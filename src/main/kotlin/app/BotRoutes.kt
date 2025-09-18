package app

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import service.PrizeSpec
import service.I18n
import util.Metrics
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.slf4j.LoggerFactory
import db.Users
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
)

private enum class AdminStep { NAME_RU, NAME_EN, START, END, FISH, LOCATION, METRIC, PRIZE_PLACES, PRIZE }

private data class BroadcastDraft(
    var step: BroadcastStep = BroadcastStep.TEXT_RU,
    var textRu: String = "",
    var textEn: String = "",
)

private enum class BroadcastStep { TEXT_RU, TEXT_EN }

private val METRIC_OPTIONS = listOf("largest", "smallest", "count")
private const val METRIC_KEYBOARD = """{"keyboard":[["largest","smallest"],["count"]],"one_time_keyboard":true,"resize_keyboard":true}"""
private const val REMOVE_KEYBOARD = """{"remove_keyboard":true}"""

private val broadcastScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


private fun parsePrizes(str: String): MutableList<PrizeSpec> {
    return try {
        Json.parseToJsonElement(str).jsonArray.map { el ->
            val obj = el.jsonObject
            PrizeSpec(
                obj["pack"]?.jsonPrimitive?.content ?: "",
                obj["qty"]?.jsonPrimitive?.int ?: 1
            )
        }.toMutableList()
    } catch (_: Exception) {
        str.split(Regex("""[,\n]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { s ->
                val parts = s.split(Regex("""[:\s]+""")).filter { it.isNotBlank() }
                val pack = parts.getOrNull(0) ?: s
                val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
                PrizeSpec(pack, qty)
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
    val adminStates = mutableMapOf<Long, AdminDraft>()
    val broadcastStates = mutableMapOf<Long, BroadcastDraft>()
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
                val link = "https://t.me/${env.botName}?startapp"
                val results = mutableListOf(
                    InlineQueryResultArticle(
                        id = "start",
                        title = if (lang == "ru") "Открыть игру" else "Open game",
                        inputMessageContent = InputTextMessageContent(link),
                        description = link
                    )
                )
                val q = iq.query.trim().lowercase()
                if (q == "/tournament" || q == "tournament") {
                    val t = tournaments.currentTournament()
                    val text = if (t != null) {
                        val tName = if (lang == "ru") t.nameRu else t.nameEn
                        val (list, _) = tournaments.leaderboard(t, iq.from.id, 10)
                        val header = "$tName\n"
                        if (list.isEmpty()) {
                            header + if (lang == "ru") "Список пуст" else "Leaderboard is empty"
                        } else {
                            val metric = t.metric.lowercase()
                            val showFish = metric != "count"
                            header + list.joinToString("\n") { e ->
                                val valueText = if (metric == "count") {
                                    e.value.toInt().toString()
                                } else {
                                    "%.2f".format(Locale.US, e.value)
                                }
                                val info = if (showFish) {
                                    val fishName = e.fish?.let { I18n.fish(it, lang) } ?: "-"
                                    "$fishName $valueText"
                                } else {
                                    valueText
                                }
                                "${e.rank}. ${e.user ?: "-"} — $info"
                            }
                        }
                    } else {
                        if (lang == "ru") "Сейчас нет активного турнира" else "No active tournament"
                    }
                    results += InlineQueryResultArticle(
                        id = "tournament",
                        title = if (lang == "ru") "Топ турнира" else "Tournament top",
                        inputMessageContent = InputTextMessageContent(text)
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

            fun trySend(chatId: Long, text: String, replyMarkup: String? = null) {
                try {
                    bot.sendMessage(chatId, text, replyMarkup)
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
            }

            fun sendLanguageMenu(chatId: Long, lang: String, prefix: String? = null) {
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
                            listOf(InlineKeyboardButton(ruLabel, "/language ru")),
                            listOf(InlineKeyboardButton(enLabel, "/language en")),
                        )
                    )
                )
                trySend(chatId, text, markup)
            }

            fun sendBaitMenu(uid: Long, chatId: Long, lang: String, prefix: String? = null) {
                val lures = fishing.listLures(uid).filter { it.qty > 0 }
                val currentId = transaction {
                    Users.select { Users.id eq uid }.single()[Users.currentLureId]?.value
                }
                val sorted = lures.sortedBy { I18n.lure(it.name, lang) }
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
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix)
                        append("\n\n")
                    }
                    append(header)
                    append("\n")
                    append(prompt)
                }
                if (sorted.isEmpty()) {
                    trySend(chatId, text)
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
                    InlineKeyboardButton(title, "/bait ${lure.id}")
                }.chunked(2)
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup)
            }

            fun sendLocationMenu(uid: Long, chatId: Long, lang: String, prefix: String? = null) {
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
                    trySend(chatId, text)
                    return
                }
                val buttons = unlocked
                    .sortedBy { I18n.location(it.name, lang) }
                    .map { loc ->
                        val label = buildString {
                            append(I18n.location(loc.name, lang))
                            if (loc.id == currentId) append(" ✅")
                        }
                        InlineKeyboardButton(label, "/location ${loc.id}")
                    }
                    .chunked(2)
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup)
            }

            fun processUserCommand(rawText: String, from: TgUser?, chatId: Long, isCallback: Boolean = false): Boolean {
                if (!rawText.startsWith("/")) return false
                val text = rawText.trim()
                val parts = text.split(" ", limit = 2)
                val command = parts[0]
                val arg = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
                val source = if (isCallback) "callback" else "message"
                when (command) {
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
                        trySend(chatId, responseText, markup)
                        return true
                    }
                    "/start" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val params = arg?.let { mapOf("payload" to it) } ?: emptyMap()
                        logCommandMetric("start", params, source)
                        val message = if (lang == "ru") {
                            """🎣 Привет! Это River King — игра про рыбалку. Играй через приложение или с помощью команд бота.

Доступные команды:
/startapp — открыть игру
/tournament — топ-10 текущего турнира
/daily — получить ежедневную награду
/language — выбрать язык
/bait — сменить приманку
/location — сменить локацию""".trimIndent()
                        } else {
                            """🎣 Welcome to River King, a fishing game you can play in the app or via bot commands.

Available commands:
/startapp — open the game
/tournament — see the current tournament top 10
/daily — claim your daily reward
/language — choose your language
/bait — change your bait
/location — change your location""".trimIndent()
                        }
                        trySend(chatId, message)
                        return true
                    }
                    "/tournament" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val t = tournaments.currentTournament()
                        val reply = if (t != null) {
                            val tName = if (lang == "ru") t.nameRu else t.nameEn
                            val (list, _) = tournaments.leaderboard(t, uid, 10)
                            val header = "$tName\n"
                            if (list.isEmpty()) {
                                header + if (lang == "ru") "Список пуст" else "Leaderboard is empty"
                            } else {
                                val metric = t.metric.lowercase()
                                val showFish = metric != "count"
                                header + list.joinToString("\n") { e ->
                                    val valueText = if (metric == "count") {
                                        e.value.toInt().toString()
                                    } else {
                                        "%.2f".format(Locale.US, e.value)
                                    }
                                    val info = if (showFish) {
                                        val fishName = e.fish?.let { I18n.fish(it, lang) } ?: "-"
                                        "$fishName $valueText"
                                    } else {
                                        valueText
                                    }
                                    "${e.rank}. ${e.user ?: "-"} — $info"
                                }
                            }
                        } else {
                            if (lang == "ru") "Сейчас нет активного турнира" else "No active tournament"
                        }
                        val params = t?.let { mapOf("metric" to it.metric.lowercase()) } ?: emptyMap()
                        logCommandMetric("tournament", params, source)
                        trySend(chatId, reply)
                        return true
                    }
                    "/daily" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val res = fishing.giveDailyBaits(uid)
                        if (res == null) {
                            val params = mapOf("result" to "not_ready")
                            logCommandMetric("daily", params, source)
                            val reply = if (lang == "ru") {
                                "Новая ежедневная награда пока недоступна"
                            } else {
                                "Your next daily reward is not ready yet"
                            }
                            trySend(chatId, reply)
                        } else {
                            val streak = res.third
                            val params = mapOf("result" to "claimed", "streak" to streak.toString())
                            logCommandMetric("daily", params, source)
                            val schedule = fishing.dailyRewardSchedule(uid)
                            val rewards = schedule.getOrNull(streak.coerceAtMost(7) - 1).orEmpty()
                            val lines = rewards.map { reward ->
                                val name = I18n.lure(reward.name, lang)
                                "• $name x${reward.qty}"
                            }
                            val body = if (lang == "ru") {
                                "Вы получили ежедневную награду (день $streak):"
                            } else {
                                "Daily reward claimed (day $streak):"
                            }
                            val text = buildString {
                                append(body)
                                if (lines.isNotEmpty()) {
                                    append("\n")
                                    append(lines.joinToString("\n"))
                                }
                            }
                            trySend(chatId, text)
                        }
                        return true
                    }
                    "/language" -> {
                        val uid = ensureUserId(from) ?: return false
                        var lang = fishing.userLanguage(uid)
                        if (arg == null) {
                            logCommandMetric("language", mapOf("action" to "show"), source)
                            sendLanguageMenu(chatId, lang)
                        } else {
                            val normalized = arg.lowercase()
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
                                sendLanguageMenu(chatId, lang, prefix)
                            } else {
                                val params = mapOf("language" to normalized, "result" to "invalid")
                                logCommandMetric("language", params, source)
                                val prefix = if (lang == "ru") {
                                    "Неизвестный язык. Используйте кнопки ниже."
                                } else {
                                    "Unknown language. Please use the buttons below."
                                }
                                sendLanguageMenu(chatId, lang, prefix)
                            }
                        }
                        return true
                    }
                    "/bait" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        if (arg == null) {
                            logCommandMetric("bait", mapOf("action" to "show"), source)
                            sendBaitMenu(uid, chatId, lang)
                        } else {
                            val lureId = arg.toLongOrNull()
                            if (lureId == null) {
                                logCommandMetric("bait", mapOf("result" to "invalid", "value" to arg), source)
                                val reply = if (lang == "ru") "Неверный формат приманки" else "Invalid bait value"
                                trySend(chatId, reply)
                            } else {
                                try {
                                    fishing.setLure(uid, lureId)
                                    logCommandMetric(
                                        "bait",
                                        mapOf("result" to "success", "value" to lureId.toString()),
                                        source
                                    )
                                    val prefix = if (lang == "ru") "Приманка обновлена" else "Bait updated"
                                    sendBaitMenu(uid, chatId, lang, prefix)
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
                                    trySend(chatId, msg)
                                }
                            }
                        }
                        return true
                    }
                    "/location" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        if (arg == null) {
                            logCommandMetric("location", mapOf("action" to "show"), source)
                            sendLocationMenu(uid, chatId, lang)
                        } else {
                            val locId = arg.toLongOrNull()
                            if (locId == null) {
                                logCommandMetric("location", mapOf("result" to "invalid", "value" to arg), source)
                                val reply = if (lang == "ru") "Неверный формат локации" else "Invalid location value"
                                trySend(chatId, reply)
                            } else {
                                try {
                                    fishing.setLocation(uid, locId)
                                    logCommandMetric(
                                        "location",
                                        mapOf("result" to "success", "value" to locId.toString()),
                                        source
                                    )
                                    val prefix = if (lang == "ru") "Локация обновлена" else "Location updated"
                                    sendLocationMenu(uid, chatId, lang, prefix)
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
                                    trySend(chatId, msg)
                                }
                            }
                        }
                        return true
                    }
                }
                return false
            }

            update.callbackQuery?.let { cq ->
                val data = cq.data
                val chatId = cq.message?.chat?.id ?: cq.from.id
                if (data != null && processUserCommand(data, cq.from, chatId, isCallback = true)) {
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
                    try {
                        fishing.buyPackage(uid, packId)
                    } catch (e: Exception) {
                        log.error("buyPackage failed uid={} packId={}", uid, packId, e)
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
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            val chatId = message.chat.id
            val userId = message.from?.id ?: return@post call.respond(HttpStatusCode.OK)
            val text = message.text ?: ""

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
                            try { bot.sendMessage(chatId, "Введите рыбу на русском или группу (common/legendary и т.д.) (или '-' для любой)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
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
                            bot.sendMessage(chatId, "Метрика (largest/smallest/count)$current", METRIC_KEYBOARD)
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
                            try { bot.sendMessage(chatId, "Неверная метрика, выберите largest, smallest или count", METRIC_KEYBOARD) } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        }
                    }
                    AdminStep.PRIZE_PLACES -> {
                        val n = text.toIntOrNull()
                        if (n != null) {
                            draft.prizePlaces = n
                            draft.prizes = MutableList(n) { i -> draft.prizes.getOrNull(i) ?: PrizeSpec("", 1) }
                            draft.currentPrize = 1
                            draft.step = AdminStep.PRIZE
                            val currentPrize = draft.prizes.getOrNull(0)?.takeIf { it.pack.isNotBlank() }?.let { " (сейчас: ${it.pack} x${it.qty})" } ?: ""
                            try { bot.sendMessage(chatId, "Награда за 1 место (pack qty)$currentPrize") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try { bot.sendMessage(chatId, "Неверный ввод, повторите") } catch (_: Exception) {}
                        }
                    }
                    AdminStep.PRIZE -> {
                        val p = text.trim().split(' ', ':')
                        val pack = p.getOrNull(0)?.takeIf { it.isNotBlank() }
                        val qty = p.getOrNull(1)?.toIntOrNull() ?: 1
                        if (pack == null || qty <= 0) {
                            try { bot.sendMessage(chatId, "Неверный ввод, укажите приз как 'pack qty'") } catch (_: Exception) {}
                            return@post call.respond(HttpStatusCode.OK)
                        }
                        val prize = PrizeSpec(pack, qty)
                        val idx = draft.currentPrize - 1
                        if (draft.prizes.size > idx) {
                            draft.prizes[idx] = prize
                        } else {
                            draft.prizes.add(prize)
                        }
                        if (draft.currentPrize < draft.prizePlaces) {
                            draft.currentPrize += 1
                            val currentPrize = draft.prizes.getOrNull(draft.currentPrize - 1)?.takeIf { it.pack.isNotBlank() }?.let { " (сейчас: ${it.pack} x${it.qty})" } ?: ""
                            try { bot.sendMessage(chatId, "Награда за ${draft.currentPrize} место (pack qty)$currentPrize") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            val start = draft.start
                            val end = draft.end
                            if (start != null && end != null) {
                                val prizesJson = draft.prizes.take(draft.prizePlaces).joinToString(prefix = "[", postfix = "]") { Json.encodeToString(it) }
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
                                    delay(30_000L)
                                }
                            }
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            if (processUserCommand(text, message.from, chatId)) {
                return@post call.respond(HttpStatusCode.OK)
            } else if (text.startsWith("/paysupport")) {
                val args = text.removePrefix("/paysupport").trim()
                val uid = fishing.ensureUserByTgId(userId)
                if (args.isEmpty()) {
                    val payments = PayService.listPayments(uid)
                    val reply = if (payments.isEmpty()) {
                        "Покупок не найдено"
                    } else {
                        val list = payments.joinToString("\n") {
                            "${it.id}: пакет ${it.packageId} на сумму ${it.amount} ${it.currency}"
                        }
                        "Выберите покупку для возврата, отправив /paysupport <ID> <причина>:\n$list"
                    }
                    try {
                        bot.sendMessage(chatId, reply)
                    } catch (e: Exception) {
                        log.error("sendMessage failed chatId={}", chatId, e)
                    }
                } else {
                    val parts = args.split(" ", limit = 2)
                    val paymentId = parts.getOrNull(0)?.toLongOrNull()
                    val reason = parts.getOrNull(1) ?: ""
                    if (paymentId != null) {
                        val payment = PayService.listPayments(uid).find { it.id == paymentId }
                        if (payment != null) {
                            val reqId = PayService.createSupportRequest(uid, paymentId, reason)
                            try {
                                bot.sendMessage(chatId, "Запрос #$reqId отправлен администрации")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", chatId, e)
                            }
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
                            try {
                                bot.sendMessage(chatId, "Покупка не найдена")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", chatId, e)
                            }
                        }
                    }
                }
            } else if (text.startsWith("/answer")) {
                val parts = text.split(" ", limit = 3)
                val uid = fishing.ensureUserByTgId(userId)
                var id = parts.getOrNull(1)?.toLongOrNull()
                val answer: String? = if (id != null) {
                    parts.getOrNull(2)
                } else {
                    id = PayService.latestInfoRequest(uid)?.id
                    parts.getOrNull(1)
                }
                if (id != null && answer != null) {
                    val reqId = id
                    val req = PayService.findSupportRequest(reqId)
                    if (req != null && req.userId == uid) {
                        PayService.updateSupportRequest(reqId, "pending", answer)
                        if (env.adminTgId != 0L) {
                            try {
                                bot.sendMessage(
                                    env.adminTgId,
                                    "Ответ по запросу #$id от $chatId: $answer\n" +
                                            "/refund $id — одобрить возврат\n" +
                                            "/reject $id <причина> — отклонить\n" +
                                            "/ask $id <вопрос> — запросить информацию"
                                )
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", env.adminTgId, e)
                            }
                        }
                        try {
                            bot.sendMessage(chatId, "Ответ отправлен администрации")
                        } catch (e: Exception) {
                            log.error("sendMessage failed chatId={}", chatId, e)
                        }
                    }
                }
            } else if (text.startsWith("/buy")) {
                val packId = text.split(" ").getOrNull(1)
                if (packId != null) {
                    try {
                        stars.sendPackageInvoice(chatId, packId)
                    } catch (_: Exception) {
                        try {
                            bot.sendMessage(chatId, "Пакет не найден")
                        } catch (e: Exception) {
                            log.error("sendMessage failed chatId={}", chatId, e)
                        }
                    }
                }
            } else if (chatId == env.adminTgId) {
                when {
                    text.startsWith("/admin") -> {
                        val markup = """{"inline_keyboard":[[{"text":"Создать турнир","callback_data":"create_tournament"},{"text":"Список турниров","callback_data":"list_tournaments"}],[{"text":"Разослать сообщение","callback_data":"broadcast_message"}]]}"""
                        try { bot.sendMessage(chatId, "Админ меню", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
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
