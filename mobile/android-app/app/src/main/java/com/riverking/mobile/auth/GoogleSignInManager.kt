package com.riverking.mobile.auth

import androidx.activity.ComponentActivity
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.riverking.mobile.BuildConfig

class GoogleSignInManager {
    suspend fun requestIdToken(activity: ComponentActivity): Result<String> {
        if (!BuildConfig.GOOGLE_AUTH_ENABLED) {
            return Result.failure(IllegalStateException("Google auth is not configured"))
        }

        return runCatching {
            val credentialManager = CredentialManager.create(activity)
            val googleOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_AUTH_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val result = credentialManager.getCredential(
                context = activity,
                request = request,
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                throw IllegalStateException("Unexpected Google credential type")
            }
        }.recoverCatching { throwable ->
            when (throwable) {
                is GetCredentialException -> throw IllegalStateException(throwable.message ?: "Google sign-in failed")
                is GoogleIdTokenParsingException -> throw IllegalStateException("Google token parsing failed")
                else -> throw throwable
            }
        }
    }
}
