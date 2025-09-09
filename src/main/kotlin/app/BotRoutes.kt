package app

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
import service.TournamentService
import service.PrizeSpec
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

internal fun parseInvoicePayload(payload: String, chatId: Long): String? {
    return if ('=' in payload) {
        val parts = payload.split(';').mapNotNull {
            val p = it.split('=', limit = 2)
            if (p.size == 2) p[0] to p[1] else null
        }.toMap()
        val packId = parts["pack"]
        val payloadUser = parts["user"]?.toLongOrNull()
        if (packId != null && payloadUser == chatId) packId else null
    } else if (payload.startsWith("pack_")) {
        payload.removePrefix("pack_")
    } else {
        null
    }
}

private data class AdminDraft(
    var id: Long? = null,
    var step: AdminStep = AdminStep.NAME,
    var name: String = "",
    var start: Instant? = null,
    var end: Instant? = null,
    var fish: String? = null,
    var location: String? = null,
    var metric: String = "",
    var prizePlaces: Int = 0,
    var prizes: MutableList<PrizeSpec> = mutableListOf(),
    var currentPrize: Int = 1,
)

private enum class AdminStep { NAME, START, END, FISH, LOCATION, METRIC, PRIZE_PLACES, PRIZE }

private val METRIC_OPTIONS = listOf("largest", "smallest", "count", "rarity")
private const val METRIC_KEYBOARD = """{"keyboard":[["largest","smallest"],["count","rarity"]],"one_time_keyboard":true,"resize_keyboard":true}"""
private const val REMOVE_KEYBOARD = """{"remove_keyboard":true}"""


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

            update.preCheckoutQuery?.let { q ->
                try {
                    bot.answerPreCheckoutQuery(q.id)
                } catch (e: Exception) {
                    log.error("answerPreCheckoutQuery failed id={}", q.id, e)
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            update.callbackQuery?.let { cq ->
                if (cq.from.id == env.adminTgId) {
                    try { bot.answerCallbackQuery(cq.id) } catch (_: Exception) {}
                    val data = cq.data
                    val target = cq.message?.chat?.id ?: cq.from.id
                    when {
                        data == "create_tournament" -> {
                            adminStates[cq.from.id] = AdminDraft()
                            try {
                                bot.sendMessage(target, "Введите название турнира")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", target, e)
                            }
                        }
                        data == "list_tournaments" -> {
                            val list = tournaments.listTournaments()
                            if (list.isEmpty()) {
                                try { bot.sendMessage(target, "Турниров нет") } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                            } else {
                                val buttons = list.joinToString(",") { t ->
                                    """[{"text":"${t.name}","callback_data":"tournament_${t.id}"}]"""
                                }
                                val markup = """{"inline_keyboard":[$buttons]}"""
                                try { bot.sendMessage(target, "Турниры", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                            }
                        }
                        data != null && data.startsWith("tournament_") -> {
                            val id = data.removePrefix("tournament_").toLongOrNull()
                            if (id != null) {
                                val t = tournaments.getTournament(id)
                                if (t != null) {
                                    val markup = """{"inline_keyboard":[[{"text":"Редактировать","callback_data":"edit_tournament_$id"},{"text":"Удалить","callback_data":"delete_tournament_$id"}]]}"""
                                    try { bot.sendMessage(target, "Турнир: ${t.name}", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
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
                                        name = t.name,
                                        start = t.startTime,
                                        end = t.endTime,
                                        fish = t.fish,
                                        location = t.location,
                                        metric = t.metric,
                                        prizePlaces = t.prizePlaces,
                                        prizes = parsePrizes(t.prizesJson),
                                    )
                                    try { bot.sendMessage(target, "Введите название турнира (сейчас: ${t.name})") } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
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
                val rawPayload = sp.invoicePayload
                val packId = parseInvoicePayload(rawPayload, chatId)
                if (packId == null) {
                    val parts = rawPayload.split(';').mapNotNull {
                        val p = it.split('=', limit = 2)
                        if (p.size == 2) p[0] to p[1] else null
                    }.toMap()
                    val payloadUser = parts["user"]?.toLongOrNull()
                    when {
                        parts["pack"] == null ->
                            log.warn("successfulPayment with no packId: chatId={} payload={}", chatId, rawPayload)
                        payloadUser != null && payloadUser != chatId ->
                            log.warn(
                                "successfulPayment payload user mismatch: chatId={} payloadUser={} payload={}",
                                chatId,
                                payloadUser,
                                rawPayload
                            )
                    }
                } else {
                    val uid = fishing.ensureUserByTgId(chatId)
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
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            val chatId = message.chat.id
            val text = message.text ?: ""

            adminStates[chatId]?.let { draft ->
                when (draft.step) {
                    AdminStep.NAME -> {
                        draft.name = text
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
                            try { bot.sendMessage(chatId, "Вид рыбы (или '-' для любой)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try { bot.sendMessage(chatId, "Неверный ввод, повторите") } catch (_: Exception) {}
                        }
                    }
                    AdminStep.FISH -> {
                        val v = text.trim()
                        draft.fish = v.takeIf { it.isNotEmpty() && it != "-" }
                        draft.step = AdminStep.LOCATION
                        val current = draft.location?.let { " (сейчас: $it)" } ?: ""
                        try { bot.sendMessage(chatId, "Локация (или '-' для любой)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.LOCATION -> {
                        val v = text.trim()
                        draft.location = v.takeIf { it.isNotEmpty() && it != "-" }
                        draft.step = AdminStep.METRIC
                        val current = if (draft.metric.isNotBlank()) " (сейчас: ${draft.metric})" else ""
                        try {
                            bot.sendMessage(chatId, "Метрика (largest/smallest/count/rarity)$current", METRIC_KEYBOARD)
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
                            try { bot.sendMessage(chatId, "Неверная метрика, выберите largest, smallest, count или rarity", METRIC_KEYBOARD) } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
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
                                            draft.name,
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
                                            draft.name,
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
                            adminStates.remove(chatId)
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            if (text.startsWith("/start")) {
                val from = message.from
                val uid = fishing.ensureUserByTgId(
                    tgId = chatId,
                    firstName = from?.first_name,
                    lastName = from?.last_name,
                    username = from?.username,
                    language = from?.language_code
                )
                val lang = fishing.userLanguage(uid)
                val reply = if (lang == "ru") {
                    "Чтобы запустить игру, нажмите кнопку меню слева с надписью Open app"
                } else {
                    "To launch the game, press the menu button on the left labeled Open app"
                }
                try {
                    bot.sendMessage(chatId, reply)
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
            } else if (text.startsWith("/paysupport")) {
                val reason = text.removePrefix("/paysupport").trim()
                val uid = fishing.ensureUserByTgId(chatId)
                val reqId = PayService.createSupportRequest(uid, null, reason)
                try {
                    bot.sendMessage(chatId, "Запрос #$reqId отправлен администрации")
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
                if (env.adminTgId != 0L) {
                    try {
                        bot.sendMessage(env.adminTgId, "Запрос #$reqId от $chatId: $reason")
                    } catch (e: Exception) {
                        log.error("sendMessage failed chatId={}", env.adminTgId, e)
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
                        val markup = """{"inline_keyboard":[[{"text":"Создать турнир","callback_data":"create_tournament"},{"text":"Список турниров","callback_data":"list_tournaments"}]]}"""
                        try { bot.sendMessage(chatId, "Админ меню", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    text.startsWith("/refund") -> {
                        val id = text.split(" ").getOrNull(1)?.toLongOrNull()
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                PayService.updateSupportRequest(id, "refunded", null)
                                try {
                                    bot.sendMessage(req.userId, "Ваш запрос #$id одобрен, возврат будет выполнен")
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", req.userId, e)
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
                                try {
                                    bot.sendMessage(req.userId, "Запрос #$id отклонен: $reason")
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", req.userId, e)
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
                                try {
                                    bot.sendMessage(req.userId, "Админ уточняет по запросу #$id: $question")
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", req.userId, e)
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
private data class TgChat(val id: Long)

@Serializable
private data class TgUser(
    val id: Long,
    @SerialName("first_name") val first_name: String? = null,
    @SerialName("last_name") val last_name: String? = null,
    val username: String? = null,
    @SerialName("language_code") val language_code: String? = null,
)
