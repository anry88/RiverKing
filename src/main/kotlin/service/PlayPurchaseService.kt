package service

import db.Payments
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class PlayPurchaseService(
    private val fishing: FishingService,
    private val verifier: PlayPurchaseVerifier?,
) {
    sealed interface CompletionResult {
        data class Success(
            val lures: List<FishingService.LureDTO>,
            val currentLureId: Long?,
        ) : CompletionResult

        data object Duplicate : CompletionResult

        data class Failure(
            val code: String,
        ) : CompletionResult
    }

    suspend fun completePurchase(
        userId: Long,
        packageId: String,
        purchaseToken: String,
    ): CompletionResult {
        val pack = fishing.findPack(packageId) ?: return CompletionResult.Failure("bad_package")
        val purchaseVerifier = verifier ?: return CompletionResult.Failure("play_verification_unavailable")
        val verification = purchaseVerifier.verifyPurchase(purchaseToken)
        val verifiedPurchase = when (verification) {
            is PlayPurchaseVerificationResult.Purchased -> verification.purchase
            PlayPurchaseVerificationResult.Pending -> return CompletionResult.Failure("purchase_pending")
            PlayPurchaseVerificationResult.Cancelled -> return CompletionResult.Failure("purchase_cancelled")
            PlayPurchaseVerificationResult.NotFound -> return CompletionResult.Failure("invalid_purchase")
            is PlayPurchaseVerificationResult.Error -> return CompletionResult.Failure(verification.code)
        }
        val matchingLineItem = verifiedPurchase.lineItems.singleOrNull { it.productId == packageId }
            ?: return CompletionResult.Failure("purchase_product_mismatch")
        if (verifiedPurchase.lineItems.size != 1 || matchingLineItem.quantity != 1) {
            return CompletionResult.Failure("purchase_quantity_unsupported")
        }
        if (matchingLineItem.consumptionState == "CONSUMPTION_STATE_CONSUMED") {
            return CompletionResult.Failure("purchase_already_consumed")
        }
        val paymentChargeId = verifiedPurchase.orderId ?: "play:$purchaseToken"
        if (hasDuplicatePurchase(purchaseToken, paymentChargeId)) {
            return CompletionResult.Duplicate
        }
        val expectedAccountId = userId.toString()
        if (verifiedPurchase.obfuscatedAccountId != null && verifiedPurchase.obfuscatedAccountId != expectedAccountId) {
            return CompletionResult.Failure("purchase_user_mismatch")
        }
        if (pack.rodCode != null && fishing.hasRod(userId, pack.rodCode)) {
            return CompletionResult.Failure("rod_unlocked")
        }
        val result = try {
            fishing.buyPackage(userId, packageId)
        } catch (_: Exception) {
            return CompletionResult.Failure("bad_package")
        }
        PayService.recordPayment(
            userId = userId,
            packageId = packageId,
            info = PayService.PaymentInfo(
                providerChargeId = purchaseToken,
                telegramChargeId = paymentChargeId,
                amount = pack.price,
                currency = "PLAY",
            )
        )
        ReferralService.onPurchase(userId, pack)
        return CompletionResult.Success(
            lures = result.first,
            currentLureId = result.second,
        )
    }

    private fun hasDuplicatePurchase(
        purchaseToken: String,
        paymentChargeId: String,
    ): Boolean = transaction {
        !Payments.select {
            (((Payments.providerChargeId eq purchaseToken) or (Payments.telegramChargeId eq paymentChargeId)) and
                (Payments.refunded eq false))
        }.empty()
    }
}
