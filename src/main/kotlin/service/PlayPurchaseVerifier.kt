package service

import app.Env
import com.google.auth.oauth2.GoogleCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

sealed interface PlayPurchaseVerificationResult {
    data class Purchased(
        val purchase: VerifiedPlayPurchase,
    ) : PlayPurchaseVerificationResult

    data object Pending : PlayPurchaseVerificationResult

    data object Cancelled : PlayPurchaseVerificationResult

    data object NotFound : PlayPurchaseVerificationResult

    data class Error(
        val code: String,
        val cause: Throwable? = null,
    ) : PlayPurchaseVerificationResult
}

data class VerifiedPlayPurchase(
    val lineItems: List<VerifiedPlayLineItem>,
    val orderId: String?,
    val obfuscatedAccountId: String?,
    val purchaseCompletionTime: Instant?,
    val isTestPurchase: Boolean,
)

data class VerifiedPlayLineItem(
    val productId: String,
    val quantity: Int,
    val acknowledgementState: String?,
    val consumptionState: String?,
)

interface PlayPurchaseVerifier {
    suspend fun verifyPurchase(purchaseToken: String): PlayPurchaseVerificationResult
}

class GooglePlayPurchaseVerifier private constructor(
    private val packageName: String,
    serviceAccountFile: String,
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) : PlayPurchaseVerifier {
    private val credentials: GoogleCredentials =
        File(serviceAccountFile).inputStream().use { input ->
            GoogleCredentials.fromStream(input)
                .createScoped(listOf(ANDROID_PUBLISHER_SCOPE))
        }
    private val tokenMutex = Mutex()

    override suspend fun verifyPurchase(purchaseToken: String): PlayPurchaseVerificationResult {
        return try {
            val response = client.get(buildPurchaseUrl(purchaseToken)) {
                bearerAuth(accessToken())
            }.body<PlayProductPurchaseV2Response>()
            when (response.purchaseStateContext?.purchaseState) {
                "PURCHASED" -> PlayPurchaseVerificationResult.Purchased(
                    VerifiedPlayPurchase(
                        lineItems = response.productLineItem.map {
                            VerifiedPlayLineItem(
                                productId = it.productId,
                                quantity = it.quantity ?: 1,
                                acknowledgementState = it.acknowledgementState,
                                consumptionState = it.consumptionState,
                            )
                        },
                        orderId = response.orderId?.takeIf { it.isNotBlank() },
                        obfuscatedAccountId = response.obfuscatedExternalAccountId?.takeIf { it.isNotBlank() },
                        purchaseCompletionTime = response.purchaseCompletionTime?.let(Instant::parse),
                        isTestPurchase = response.testPurchaseContext != null,
                    )
                )
                "PENDING" -> PlayPurchaseVerificationResult.Pending
                "CANCELLED" -> PlayPurchaseVerificationResult.Cancelled
                else -> PlayPurchaseVerificationResult.Error("play_verification_failed")
            }
        } catch (error: ClientRequestException) {
            when (error.response.status) {
                HttpStatusCode.NotFound -> PlayPurchaseVerificationResult.NotFound
                HttpStatusCode.Unauthorized,
                HttpStatusCode.Forbidden,
                -> PlayPurchaseVerificationResult.Error("play_verification_unavailable", error)
                else -> PlayPurchaseVerificationResult.Error("play_verification_failed", error)
            }
        } catch (error: Throwable) {
            PlayPurchaseVerificationResult.Error("play_verification_failed", error)
        }
    }

    private suspend fun accessToken(): String = tokenMutex.withLock {
        withContext(Dispatchers.IO) {
            credentials.refreshIfExpired()
            credentials.accessToken?.tokenValue ?: credentials.refreshAccessToken().tokenValue
        }
    }

    private fun buildPurchaseUrl(purchaseToken: String): String =
        "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$packageName/purchases/productsv2/tokens/$purchaseToken"

    companion object {
        fun fromEnv(env: Env): PlayPurchaseVerifier? {
            if (env.googlePlayPackageName.isBlank() || env.googlePlayServiceAccountFile.isBlank()) {
                return null
            }
            return GooglePlayPurchaseVerifier(
                packageName = env.googlePlayPackageName,
                serviceAccountFile = env.googlePlayServiceAccountFile,
            )
        }

        private const val ANDROID_PUBLISHER_SCOPE =
            "https://www.googleapis.com/auth/androidpublisher"
    }
}

@Serializable
private data class PlayProductPurchaseV2Response(
    val productLineItem: List<PlayProductLineItemResponse> = emptyList(),
    val purchaseStateContext: PlayPurchaseStateContextResponse? = null,
    val testPurchaseContext: PlayTestPurchaseContextResponse? = null,
    val orderId: String? = null,
    val obfuscatedExternalAccountId: String? = null,
    val purchaseCompletionTime: String? = null,
)

@Serializable
private data class PlayPurchaseStateContextResponse(
    val purchaseState: String? = null,
)

@Serializable
private data class PlayProductLineItemResponse(
    val productId: String,
    val quantity: Int? = null,
    val acknowledgementState: String? = null,
    val consumptionState: String? = null,
)

@Serializable
private data class PlayTestPurchaseContextResponse(
    val fopType: String? = null,
)
