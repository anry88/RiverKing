package service

import app.Env
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    class MissingPrivateChatAccessException(message: String) : Exception(message)

    class StarsPaymentException(message: String, val errorCode: Int? = null) : Exception(message)

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

    @Serializable
    private data class InvoiceLinkReq(
        val title: String,
        val description: String,
        val payload: String,
        @SerialName("provider_token") val providerToken: String,
        val currency: String,
        val prices: List<LabeledPrice>,
    )

    /** Send invoice for lure package purchase to chat. */
    suspend fun sendPackageInvoice(
        chatId: Long,
        buyerId: Long,
        packageId: String,
        language: String,
    ) = withContext(Dispatchers.IO) {
        val pack = fishing.listShop(language).flatMap { it.packs }.find { it.id == packageId }
            ?: fishing.listShop("ru").flatMap { it.packs }.find { it.id == packageId }
            ?: throw IllegalArgumentException("Unknown package")

        val invoice = Invoice(
            chatId = chatId,
            title = pack.name,
            description = pack.desc,
            payload = "pack=${pack.id};user=$buyerId",
            providerToken = env.providerToken,
            currency = "XTR",
            prices = listOf(LabeledPrice(pack.name, pack.price)),
        )

        val url = URL("https://api.telegram.org/bot${env.botToken}/sendInvoice")
        val body = Json.encodeToString(invoice)
        var status = 0
        val response = (url.openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray()) }
            status = responseCode
            val stream = if (status in 200..299) inputStream else errorStream
            val text = stream.buffered().use { it.readBytes().decodeToString() }
            disconnect()
            text
        }

        if (response.isBlank()) {
            if (status !in 200..299) {
                throw StarsPaymentException("Failed to send invoice", status)
            }
            return@withContext
        }

        val json = try {
            Json.parseToJsonElement(response).jsonObject
        } catch (_: Exception) {
            if (status !in 200..299) {
                throw StarsPaymentException("Failed to send invoice", status)
            }
            return@withContext
        }

        val ok = json["ok"]?.jsonPrimitive?.booleanOrNull
        if (ok == false) {
            val description = json["description"]?.jsonPrimitive?.content
            val errorCode = json["error_code"]?.jsonPrimitive?.intOrNull
            if (errorCode == 403 && description?.contains("can't initiate conversation", ignoreCase = true) == true) {
                throw MissingPrivateChatAccessException(description)
            }
            throw StarsPaymentException(description ?: "Failed to send invoice", errorCode)
        }
    }

    /** Create invoice link for package purchase to be used in a Mini App. */
    suspend fun createInvoiceLink(userId: Long, packageId: String, language: String): String = withContext(Dispatchers.IO) {
        val pack = fishing.listShop(language).flatMap { it.packs }.find { it.id == packageId }
            ?: fishing.listShop("ru").flatMap { it.packs }.find { it.id == packageId }
            ?: throw IllegalArgumentException("Unknown package")

        val req = InvoiceLinkReq(
            title = pack.name,
            description = pack.desc,
            payload = "pack=${pack.id};user=$userId",
            providerToken = env.providerToken,
            currency = "XTR",
            prices = listOf(LabeledPrice(pack.name, pack.price)),
        )

        val url = URL("https://api.telegram.org/bot${env.botToken}/createInvoiceLink")
        val body = Json.encodeToString(req)
        val response = (url.openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray()) }
            val stream = if (responseCode in 200..299) inputStream else errorStream
            val resp = stream.buffered().use { it.readBytes().decodeToString() }
            disconnect()
            resp
        }
        Json.parseToJsonElement(response).jsonObject["result"]!!.jsonPrimitive.content
    }

    /** Refund a Stars payment given its Telegram charge id. */
    suspend fun refundStars(tgUserId: Long, telegramPaymentChargeId: String) = withContext(Dispatchers.IO) {
        val url = URL("https://api.telegram.org/bot${env.botToken}/refundStarPayment")
        val body = "{\"user_id\":$tgUserId,\"telegram_payment_charge_id\":\"$telegramPaymentChargeId\"}"
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray()) }
            val stream = if (responseCode in 200..299) inputStream else errorStream
            stream.use { it.readBytes() }
            disconnect()
        }
    }
}

