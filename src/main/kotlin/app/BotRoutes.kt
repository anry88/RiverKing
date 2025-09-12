package app

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import service.FishingService
import service.PayService
import service.StarsPaymentService
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

fun Application.botRoutes(env: Env) {
    val bot = TelegramBot(env.botToken)
    val fishing = FishingService()
    val stars = StarsPaymentService(env, fishing)
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
                    text.startsWith("/refund") -> {
                        val id = text.split(" ").getOrNull(1)?.toLongOrNull()
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                var refunded = false
                                val chargeId = req.telegramChargeId
                                if (chargeId != null) {
                                    try {
                                        stars.refundStars(req.userId, chargeId)
                                        req.paymentId?.let { PayService.markPaymentRefunded(it) }
                                        refunded = true
                                    } catch (e: Exception) {
                                        log.error("refundStars failed id={} chargeId={}", id, chargeId, e)
                                    }
                                }
                                PayService.updateSupportRequest(id, "refunded", null)
                                try {
                                    val msg = if (refunded) {
                                        "Ваш запрос #$id одобрен, возврат выполнен"
                                    } else {
                                        "Ваш запрос #$id одобрен"
                                    }
                                    bot.sendMessage(req.userId, msg)
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
private data class TgChat(val id: Long)

@Serializable
private data class TgUser(
    val id: Long,
    @SerialName("first_name") val first_name: String? = null,
    @SerialName("last_name") val last_name: String? = null,
    val username: String? = null,
    @SerialName("language_code") val language_code: String? = null,
)
