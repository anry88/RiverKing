package com.riverking.mobile.auth

import android.content.Context
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlayReferralManager(
    private val context: Context,
) {
    suspend fun fetchInstallReferrerToken(): String? = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(context).build()
        continuation.invokeOnCancellation {
            runCatching { client.endConnection() }
        }
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                val token = if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    runCatching { parseReferralToken(client.installReferrer.installReferrer) }.getOrNull()
                } else {
                    null
                }
                runCatching { client.endConnection() }
                if (continuation.isActive) {
                    continuation.resume(token)
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                runCatching { client.endConnection() }
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        })
    }

    companion object {
        fun parseReferralToken(raw: String?): String? {
            val normalized = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val decoded = Uri.decode(normalized)
            val viaQuery = runCatching {
                Uri.parse("https://riverking.invalid/?$decoded").getQueryParameter("ref")
            }.getOrNull()?.trim()
            if (!viaQuery.isNullOrEmpty()) return viaQuery
            val match = Regex("""(?:^|&)ref=([A-Za-z0-9_-]+)(?:&|$)""").find(decoded)
            return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
        }
    }
}
