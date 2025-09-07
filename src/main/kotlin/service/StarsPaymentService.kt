package service

import app.Env
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for sending Telegram Stars invoices for lure packages
 * and issuing refunds. Uses plain HTTP calls to the Bot API.
 */
class StarsPaymentService(
    private val env: Env,
    private val fishing: FishingService,
) {

    @Serializable
    private data class LabeledPrice(val label: String, val amount: Int)

    @Serializable
    private data class Invoice(
        @SerialName("chat_id") val chatId: Long,
        val title: String,
        val description: String,
        val payload: String,
        @SerialName("provider_token") val providerToken: String,
        val currency: String,
        val prices: List<LabeledPrice>,
    )

    /** Send invoice for lure package purchase to chat. */
    fun sendPackageInvoice(chatId: Long, packageId: String) {
        val pack = fishing.listShop().flatMap { it.packs }.find { it.id == packageId }
            ?: throw IllegalArgumentException("Unknown package")

        val invoice = Invoice(
            chatId = chatId,
            title = pack.name,
            description = pack.desc,
            payload = "pack_${pack.id}",
            providerToken = env.providerToken,
            currency = "XTR",
            prices = listOf(LabeledPrice(pack.name, pack.price)),
        )

        val url = URL("https://api.telegram.org/bot${env.botToken}/sendInvoice")
        val body = Json.encodeToString(invoice)
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray()) }
            inputStream.use { it.readBytes() }
            disconnect()
        }
    }

    /** Refund a Stars payment given its Telegram charge id. */
    fun refundStars(userId: Long, telegramPaymentChargeId: String) {
        val url = URL("https://api.telegram.org/bot${env.botToken}/refundStarPayment")
        val body = "{\"user_id\":$userId,\"telegram_payment_charge_id\":\"$telegramPaymentChargeId\"}"
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray()) }
            inputStream.use { it.readBytes() }
            disconnect()
        }
    }
}

