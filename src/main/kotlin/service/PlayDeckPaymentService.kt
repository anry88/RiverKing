package service

import app.Env
import db.Payments
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import util.Metrics
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PlayDeckPaymentService(
    private val env: Env,
    private val fishing: FishingService,
) {
    data class Order(
        val externalId: String,
        val amount: Int,
        val description: String,
        val photoUrl: String?,
        val isTest: Boolean,
    )

    sealed class CheckoutResult {
        data object Accepted : CheckoutResult()
        data class Rejected(val code: String) : CheckoutResult()
    }

    sealed class CompletionResult {
        data class Success(val lures: List<FishingService.LureDTO>, val currentLureId: Long?) : CompletionResult()
        data object Duplicate : CompletionResult()
        data class Failure(val code: String) : CompletionResult()
    }

    @Serializable
    private data class OrderPayload(
        val userId: Long,
        val packageId: String,
        val issuedAt: Long,
        val nonce: String,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val random = SecureRandom()

    fun createOrder(userId: Long, packageId: String, language: String): Order {
        if (env.playDeckGameToken.isBlank() && !env.devMode) {
            error("playdeck_unconfigured")
        }
        val pack = fishing.listShop(language).flatMap { it.packs }.find { it.id == packageId }
            ?: fishing.listShop("ru").flatMap { it.packs }.find { it.id == packageId }
            ?: error("bad_package")
        if (pack.rodCode != null && fishing.hasRod(userId, pack.rodCode)) {
            error("rod_unlocked")
        }
        val payload = OrderPayload(
            userId = userId,
            packageId = packageId,
            issuedAt = Instant.now().epochSecond,
            nonce = randomNonce(),
        )
        val encodedPayload = base64Url(json.encodeToString(payload).toByteArray())
        val signature = hmacHex(orderSecret(), encodedPayload)
        val iconName = if (packageId.startsWith("autofish")) "autofish" else packageId
        val photoUrl = env.publicBaseUrl.trimEnd('/').takeIf { it.isNotBlank() }
            ?.let { "$it/app/assets/shop/$iconName.png" }
        return Order(
            externalId = "pd.$encodedPayload.$signature",
            amount = pack.price,
            description = pack.desc.take(255),
            photoUrl = photoUrl,
            isTest = env.playDeckTestPayments,
        )
    }

    fun validateCheckout(hash: String, checkout: JsonObject): CheckoutResult {
        if (!verifyPostback(hash, checkout)) return CheckoutResult.Rejected("bad_signature")
        val externalId = checkout["externalId"]?.jsonPrimitive?.content ?: return CheckoutResult.Rejected("bad_external_id")
        val payload = parseOrderPayload(externalId) ?: return CheckoutResult.Rejected("bad_external_id")
        val amount = checkout["amount"]?.jsonPrimitive?.intOrNull ?: return CheckoutResult.Rejected("bad_amount")
        val telegramId = checkout["telegramId"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: return CheckoutResult.Rejected("bad_telegram_id")
        return validateOrder(payload, amount, telegramId)?.let { CheckoutResult.Rejected(it) }
            ?: CheckoutResult.Accepted
    }

    fun completePayment(hash: String, payment: JsonObject): CompletionResult {
        if (!verifyPostback(hash, payment)) return CompletionResult.Failure("bad_signature")
        if (payment["successful"]?.jsonPrimitive?.booleanOrNull != true) {
            return CompletionResult.Failure("payment_not_successful")
        }
        val externalId = payment["externalId"]?.jsonPrimitive?.content ?: return CompletionResult.Failure("bad_external_id")
        val payload = parseOrderPayload(externalId) ?: return CompletionResult.Failure("bad_external_id")
        val amount = payment["amount"]?.jsonPrimitive?.intOrNull ?: return CompletionResult.Failure("bad_amount")
        val telegramId = payment["telegramId"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: return CompletionResult.Failure("bad_telegram_id")
        validateOrder(payload, amount, telegramId)?.let { return CompletionResult.Failure(it) }

        val chargeId = chargeId(externalId)
        if (hasPayment(chargeId)) return CompletionResult.Duplicate
        val result = try {
            fishing.buyPackage(payload.userId, payload.packageId)
        } catch (e: Exception) {
            return CompletionResult.Failure(e.message ?: "buy_failed")
        }
        PayService.recordPayment(
            payload.userId,
            payload.packageId,
            PayService.PaymentInfo(
                providerChargeId = chargeId,
                telegramChargeId = chargeId,
                amount = amount,
                currency = "XTR",
            )
        )
        fishing.findPack(payload.packageId)?.let { pack ->
            ReferralService.onPurchase(payload.userId, pack)
        }
        Metrics.counter(
            "shop_purchase_complete_total",
            mapOf("pack" to payload.packageId, "currency" to "playdeck"),
        )
        return CompletionResult.Success(result.first, result.second)
    }

    private fun validateOrder(payload: OrderPayload, amount: Int, telegramId: Long): String? {
        val pack = fishing.findPack(payload.packageId) ?: return "bad_package"
        if (amount != pack.price) return "amount_mismatch"
        val linkedTelegramId = fishing.userTgId(payload.userId)
        if (linkedTelegramId != null && linkedTelegramId != telegramId) return "telegram_id_mismatch"
        if (pack.rodCode != null && fishing.hasRod(payload.userId, pack.rodCode)) return "rod_unlocked"
        return null
    }

    private fun parseOrderPayload(externalId: String): OrderPayload? {
        val parts = externalId.split('.')
        if (parts.size != 3 || parts[0] != "pd") return null
        val expectedSignature = hmacHex(orderSecret(), parts[1])
        if (!constantTimeEquals(expectedSignature, parts[2])) return null
        val bytes = runCatching { Base64.getUrlDecoder().decode(parts[1]) }.getOrNull() ?: return null
        return runCatching { json.decodeFromString<OrderPayload>(bytes.decodeToString()) }.getOrNull()
    }

    private fun verifyPostback(hash: String, payload: JsonObject): Boolean {
        val token = env.playDeckGameToken.takeIf { it.isNotBlank() } ?: return false
        val dataCheckString = payload.entries
            .sortedBy { it.key }
            .joinToString("\n") { (key, value) -> "$key=${checkStringValue(value)}" }
        val secretKey = hmac("WebAppData".toByteArray(), token.toByteArray())
        val computed = hmacHex(secretKey, dataCheckString)
        return constantTimeEquals(computed, hash)
    }

    private fun hasPayment(chargeId: String): Boolean = transaction {
        Payments.select {
            (((Payments.telegramChargeId eq chargeId) or (Payments.providerChargeId eq chargeId)) and
                (Payments.refunded eq false))
        }.any()
    }

    private fun orderSecret(): ByteArray =
        (env.playDeckGameToken.ifBlank { env.authTokenSecret }).toByteArray()

    private fun randomNonce(): String {
        val bytes = ByteArray(12)
        random.nextBytes(bytes)
        return base64Url(bytes)
    }

    private fun chargeId(externalId: String): String = "playdeck:$externalId"

    private fun checkStringValue(value: JsonElement): String = value.jsonPrimitive.content

    private fun base64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun hmacHex(key: ByteArray, data: String): String =
        hmac(key, data.toByteArray()).toHex()

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(left.toByteArray(), right.toByteArray())
}
