package com.riverking.mobile.ui

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlayBillingManager(
    context: Context,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {
    var onNotice: (String) -> Unit = {}
    var onSyncPurchase: suspend (String, String, String?, Long?) -> PlayPurchaseSyncResult =
        { _, _, _, _ -> PlayPurchaseSyncResult.Failed("play_purchase_unavailable", "play_purchase_unavailable") }
    var accountIdProvider: () -> String? = { null }
    var isConsumableProduct: (String) -> Boolean = { true }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()
    private val productDetailsCache = linkedMapOf<String, ProductDetails>()
    private val processingTokens = linkedSetOf<String>()
    private var connecting = false

    fun start() {
        ensureConnected {
            syncPurchases()
        }
    }

    fun stop() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        connecting = false
        productDetailsCache.clear()
        processingTokens.clear()
    }

    fun syncPurchases() {
        ensureConnected {
            scope.launch {
                val (billingResult, purchases) = queryPurchases()
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    onNotice("play_purchase_unavailable")
                    return@launch
                }
                purchases.forEach(::handlePurchase)
            }
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        ensureConnected {
            scope.launch {
                val details = getProductDetails(productId)
                if (details == null) {
                    onNotice("play_product_unavailable")
                    return@launch
                }
                val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                val flowBuilder = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                accountIdProvider()?.takeIf { it.isNotBlank() }?.let(flowBuilder::setObfuscatedAccountId)
                val billingResult = billingClient.launchBillingFlow(activity, flowBuilder.build())
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> Unit
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> syncPurchases()
                    BillingClient.BillingResponseCode.USER_CANCELED -> onNotice("purchase_cancelled")
                    else -> onNotice("play_purchase_unavailable")
                }
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> onNotice("purchase_cancelled")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> syncPurchases()
            else -> onNotice("play_purchase_unavailable")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!processingTokens.add(purchase.purchaseToken)) return
        scope.launch {
            try {
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> processCompletedPurchase(purchase)
                    Purchase.PurchaseState.PENDING -> onNotice("purchase_pending")
                    else -> Unit
                }
            } finally {
                processingTokens.remove(purchase.purchaseToken)
            }
        }
    }

    private suspend fun processCompletedPurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull()
        if (productId.isNullOrBlank()) {
            onNotice("invalid_purchase")
            return
        }
        when (
            val syncResult = onSyncPurchase(
                productId,
                purchase.purchaseToken,
                purchase.orderId,
                purchase.purchaseTime,
            )
        ) {
            PlayPurchaseSyncResult.Completed,
            PlayPurchaseSyncResult.Duplicate,
            -> {
                if (isConsumableProduct(productId)) {
                    finalizeConsumable(purchase.purchaseToken)
                } else if (!purchase.isAcknowledged) {
                    finalizeNonConsumable(purchase.purchaseToken)
                }
            }
            is PlayPurchaseSyncResult.Failed -> onNotice(syncResult.code)
        }
    }

    private suspend fun finalizeConsumable(purchaseToken: String) {
        val billingResult = consumePurchase(purchaseToken)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            onNotice("play_finalize_failed")
        }
    }

    private suspend fun finalizeNonConsumable(purchaseToken: String) {
        val billingResult = acknowledgePurchase(purchaseToken)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            onNotice("play_finalize_failed")
        }
    }

    private fun ensureConnected(onConnected: () -> Unit) {
        if (billingClient.isReady) {
            onConnected()
            return
        }
        if (connecting) return
        connecting = true
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    connecting = false
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onConnected()
                    } else {
                        onNotice("play_purchase_unavailable")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    connecting = false
                }
            }
        )
    }

    private suspend fun getProductDetails(productId: String): ProductDetails? {
        productDetailsCache[productId]?.let { return it }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val (billingResult, details) = queryProductDetails(params)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return null
        }
        return details.firstOrNull()?.also { productDetailsCache[productId] = it }
    }

    private suspend fun queryProductDetails(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, List<ProductDetails>> = suspendCancellableCoroutine { continuation ->
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            continuation.resume(billingResult to productDetailsList)
        }
    }

    private suspend fun queryPurchases(): Pair<BillingResult, List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ) { billingResult, purchases ->
                continuation.resume(billingResult to purchases)
            }
        }

    private suspend fun acknowledgePurchase(purchaseToken: String): BillingResult =
        suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build()
            ) { billingResult ->
                continuation.resume(billingResult)
            }
        }

    private suspend fun consumePurchase(purchaseToken: String): BillingResult =
        suspendCancellableCoroutine { continuation ->
            billingClient.consumeAsync(
                ConsumeParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build()
            ) { billingResult, _ ->
                continuation.resume(billingResult)
            }
        }
}
