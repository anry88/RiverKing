package com.riverking.mobile.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureSessionStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "riverking_mobile_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun read(): StoredSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_ACCESS_EXPIRES_AT, 0L)
        return StoredSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresAt = expiresAt,
            authProvider = prefs.getString(KEY_AUTH_PROVIDER, null),
        )
    }

    fun write(session: StoredSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_ACCESS_EXPIRES_AT, session.accessTokenExpiresAt)
            .putString(KEY_AUTH_PROVIDER, session.authProvider)
            .apply()
    }

    fun readLastLogin(): String? =
        prefs.getString(KEY_LAST_LOGIN, null)?.takeIf { it.isNotBlank() }

    fun writeLastLogin(login: String) {
        prefs.edit()
            .putString(KEY_LAST_LOGIN, login.trim().lowercase())
            .apply()
    }

    fun readPendingTelegramLogin(): String? =
        prefs.getString(KEY_PENDING_TELEGRAM_LOGIN, null)?.takeIf { it.isNotBlank() }

    fun writePendingTelegramLogin(sessionToken: String) {
        prefs.edit()
            .putString(KEY_PENDING_TELEGRAM_LOGIN, sessionToken)
            .apply()
    }

    fun clearPendingTelegramLogin() {
        prefs.edit()
            .remove(KEY_PENDING_TELEGRAM_LOGIN)
            .apply()
    }

    fun readPendingTelegramLink(): String? =
        prefs.getString(KEY_PENDING_TELEGRAM_LINK, null)?.takeIf { it.isNotBlank() }

    fun writePendingTelegramLink(sessionToken: String) {
        prefs.edit()
            .putString(KEY_PENDING_TELEGRAM_LINK, sessionToken)
            .apply()
    }

    fun clearPendingTelegramLink() {
        prefs.edit()
            .remove(KEY_PENDING_TELEGRAM_LINK)
            .apply()
    }

    fun readPendingReferralToken(): String? =
        prefs.getString(KEY_PENDING_REFERRAL_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun writePendingReferralToken(token: String) {
        prefs.edit()
            .putString(KEY_PENDING_REFERRAL_TOKEN, token)
            .apply()
    }

    fun clearPendingReferralToken() {
        prefs.edit()
            .remove(KEY_PENDING_REFERRAL_TOKEN)
            .apply()
    }

    fun readSeenInstallReferrerToken(): String? =
        prefs.getString(KEY_SEEN_INSTALL_REFERRER_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun writeSeenInstallReferrerToken(token: String) {
        prefs.edit()
            .putString(KEY_SEEN_INSTALL_REFERRER_TOKEN, token)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ACCESS_EXPIRES_AT)
            .remove(KEY_AUTH_PROVIDER)
            .remove(KEY_PENDING_TELEGRAM_LOGIN)
            .remove(KEY_PENDING_TELEGRAM_LINK)
            .remove(KEY_PENDING_REFERRAL_TOKEN)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_LAST_LOGIN = "last_login"
        private const val KEY_PENDING_TELEGRAM_LOGIN = "pending_telegram_login"
        private const val KEY_PENDING_TELEGRAM_LINK = "pending_telegram_link"
        private const val KEY_PENDING_REFERRAL_TOKEN = "pending_referral_token"
        private const val KEY_SEEN_INSTALL_REFERRER_TOKEN = "seen_install_referrer_token"
    }
}
