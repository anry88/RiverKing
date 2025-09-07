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

fun Application.botRoutes(env: Env) {
    val bot = TelegramBot(env.botToken)
    val fishing = FishingService()
    val stars = StarsPaymentService(env, fishing)
    routing {
        post("/bot") {
            val update = try { call.receive<TgUpdate>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.OK)
            }

            update.preCheckoutQuery?.let { q ->
                bot.answerPreCheckoutQuery(q.id)
                return@post call.respond(HttpStatusCode.OK)
            }

            val message = update.message ?: return@post call.respond(HttpStatusCode.OK)

            message.successfulPayment?.let { sp ->
                val chatId = message.chat.id
                val parts = sp.invoicePayload.split(';').mapNotNull {
                    val p = it.split('=', limit = 2)
                    if (p.size == 2) p[0] to p[1] else null
                }.toMap()
                val packId = parts["pack"]
                val payloadUser = parts["user"]?.toLongOrNull()
                if (packId != null && payloadUser == chatId) {
                    val uid = fishing.ensureUserByTgId(chatId)
                    fishing.buyPackage(uid, packId)
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
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            val chatId = message.chat.id
            val text = message.text ?: ""
            if (text.startsWith("/paysupport")) {
                val reason = text.removePrefix("/paysupport").trim()
                val uid = fishing.ensureUserByTgId(chatId)
                val reqId = PayService.createSupportRequest(uid, null, reason)
                bot.sendMessage(chatId, "Запрос #$reqId отправлен администрации")
                if (env.adminTgId != 0L) {
                    bot.sendMessage(env.adminTgId, "Запрос #$reqId от $chatId: $reason")
                }
            } else if (text.startsWith("/buy")) {
                val packId = text.split(" ").getOrNull(1)
                if (packId != null) {
                    try {
                        stars.sendPackageInvoice(chatId, packId)
                    } catch (_: Exception) {
                        bot.sendMessage(chatId, "Пакет не найден")
                    }
                }
            } else if (chatId == env.adminTgId) {
                when {
                    text.startsWith("/refund") -> {
                        val id = text.split(" ").getOrNull(1)?.toLongOrNull()
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                PayService.updateSupportRequest(id, "refunded", null)
                                bot.sendMessage(req.userId, "Ваш запрос #$id одобрен, возврат будет выполнен")
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
                                bot.sendMessage(req.userId, "Запрос #$id отклонен: $reason")
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
                                bot.sendMessage(req.userId, "Админ уточняет по запросу #$id: $question")
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
