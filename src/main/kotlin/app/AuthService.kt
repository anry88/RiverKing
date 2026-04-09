package app

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import db.AuthIdentities
import db.AuthSessions
import db.PasswordCredentials
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import service.FishingService
import util.PasswordHasher
import java.time.Clock
import java.time.Duration
import java.time.Instant

class AuthService(
    private val env: Env,
    private val fishing: FishingService,
    private val clock: Clock = Clock.systemUTC(),
) {
    data class AuthResult(
        val userId: Long,
        val accessToken: String,
        val accessTokenExpiresAt: Instant,
        val refreshToken: String,
    )

    class AuthException(message: String) : IllegalArgumentException(message)

    private val tokens = AuthTokenCodec(
        secret = env.authTokenSecret,
        accessTokenTtl = Duration.ofMinutes(env.authAccessTokenTtlMinutes),
        refreshTokenTtl = Duration.ofDays(env.authRefreshTokenTtlDays),
        clock = clock,
    )

    private val googleVerifier: GoogleIdTokenVerifier? by lazy {
        if (env.googleAuthClientId.isBlank()) {
            null
        } else {
            GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(listOf(env.googleAuthClientId))
                .build()
        }
    }

    fun resolveAccessToken(token: String): Long? {
        val userId = tokens.verifyAccessToken(token) ?: return null
        return runCatching { fishing.ensureUserById(userId) }.getOrNull()
    }

    fun issueTokens(userId: Long, deviceInfo: String? = null): AuthResult {
        val access = tokens.createAccessToken(userId)
        val refresh = tokens.createRefreshSession()
        transaction {
            AuthSessions.insert {
                it[id] = refresh.sessionId
                it[AuthSessions.userId] = userId
                it[refreshTokenHash] = refresh.refreshTokenHash
                it[expiresAt] = refresh.expiresAt
                it[createdAt] = clock.instant()
                it[AuthSessions.deviceInfo] = deviceInfo?.take(255)
            }
        }
        return AuthResult(
            userId = userId,
            accessToken = access.value,
            accessTokenExpiresAt = access.expiresAt,
            refreshToken = refresh.token,
        )
    }

    fun registerPassword(
        login: String,
        password: String,
        language: String? = null,
        refToken: String? = null,
        deviceInfo: String? = null,
    ): AuthResult {
        val normalizedLogin = normalizeLogin(login)
        validateLogin(normalizedLogin)
        validatePassword(password)
        val passwordHash = PasswordHasher.hash(password)
        val userId = transaction {
            val existing = PasswordCredentials
                .select { PasswordCredentials.login eq normalizedLogin }
                .singleOrNull()
            if (existing != null) throw AuthException("login_taken")

            val createdUserId = fishing.createUser(language = language, refToken = refToken)
            PasswordCredentials.insert {
                it[PasswordCredentials.userId] = createdUserId
                it[PasswordCredentials.login] = normalizedLogin
                it[PasswordCredentials.passwordHash] = passwordHash
                it[createdAt] = clock.instant()
                it[updatedAt] = clock.instant()
            }
            AuthIdentities.insert {
                it[userId] = createdUserId
                it[provider] = "password"
                it[subject] = normalizedLogin
                it[email] = null
                it[emailVerified] = false
                it[createdAt] = clock.instant()
                it[lastLoginAt] = clock.instant()
            }
            createdUserId
        }
        return issueTokens(userId, deviceInfo)
    }

    fun loginPassword(login: String, password: String, deviceInfo: String? = null): AuthResult {
        val normalizedLogin = normalizeLogin(login)
        validatePassword(password)
        val userId = transaction {
            val row = PasswordCredentials
                .select { PasswordCredentials.login eq normalizedLogin }
                .singleOrNull() ?: throw AuthException("invalid_credentials")
            val verified = PasswordHasher.verify(password, row[PasswordCredentials.passwordHash])
            if (!verified) throw AuthException("invalid_credentials")
            val resolvedUserId = row[PasswordCredentials.userId].value
            PasswordCredentials.update({ PasswordCredentials.id eq row[PasswordCredentials.id].value }) {
                it[updatedAt] = clock.instant()
            }
            AuthIdentities.update({
                (AuthIdentities.userId eq resolvedUserId) and
                    (AuthIdentities.provider eq "password") and
                    (AuthIdentities.subject eq normalizedLogin)
            }) {
                it[lastLoginAt] = clock.instant()
            }
            resolvedUserId
        }
        fishing.ensureUserById(userId)
        return issueTokens(userId, deviceInfo)
    }

    fun loginGoogle(idToken: String, refToken: String? = null, deviceInfo: String? = null): AuthResult {
        val verifier = googleVerifier ?: throw AuthException("google_auth_disabled")
        val verified = verifier.verify(idToken) ?: throw AuthException("invalid_google_token")
        val subject = verified.payload.subject ?: throw AuthException("invalid_google_token")
        val email = verified.payload.email
        val isEmailVerified = verified.payload.emailVerified ?: false
        val userId = transaction {
            val identity = AuthIdentities
                .select {
                    (AuthIdentities.provider eq "google") and
                        (AuthIdentities.subject eq subject)
                }
                .singleOrNull()

            val resolvedUserId = if (identity != null) {
                identity[AuthIdentities.userId].value
            } else {
                val createdUserId = fishing.createUser(language = null, refToken = refToken)
                AuthIdentities.insert {
                    it[userId] = createdUserId
                    it[provider] = "google"
                    it[AuthIdentities.subject] = subject
                    it[AuthIdentities.email] = email
                    it[emailVerified] = isEmailVerified
                    it[createdAt] = clock.instant()
                    it[lastLoginAt] = clock.instant()
                }
                createdUserId
            }

            AuthIdentities.update({
                (AuthIdentities.userId eq resolvedUserId) and
                    (AuthIdentities.provider eq "google") and
                    (AuthIdentities.subject eq subject)
            }) {
                it[AuthIdentities.email] = email
                it[emailVerified] = isEmailVerified
                it[lastLoginAt] = clock.instant()
            }
            resolvedUserId
        }
        fishing.ensureUserById(userId)
        return issueTokens(userId, deviceInfo)
    }

    fun refresh(refreshToken: String, deviceInfo: String? = null): AuthResult {
        val (sessionId, secret) = tokens.parseRefreshToken(refreshToken)
            ?: throw AuthException("invalid_refresh_token")
        val secretHash = tokens.hashRefreshSecret(secret)
        val userId = transaction {
            val row = AuthSessions
                .select { AuthSessions.id eq sessionId }
                .singleOrNull() ?: throw AuthException("invalid_refresh_token")
            if (row[AuthSessions.refreshTokenHash] != secretHash) {
                throw AuthException("invalid_refresh_token")
            }
            if (!row[AuthSessions.expiresAt].isAfter(clock.instant())) {
                AuthSessions.deleteWhere { AuthSessions.id eq sessionId }
                throw AuthException("refresh_expired")
            }
            val resolvedUserId = row[AuthSessions.userId].value
            AuthSessions.deleteWhere { AuthSessions.id eq sessionId }
            resolvedUserId
        }
        fishing.ensureUserById(userId)
        return issueTokens(userId, deviceInfo)
    }

    fun logout(refreshToken: String) {
        val (sessionId, secret) = tokens.parseRefreshToken(refreshToken) ?: return
        val secretHash = tokens.hashRefreshSecret(secret)
        transaction {
            AuthSessions.deleteWhere {
                (AuthSessions.id eq sessionId) and
                    (AuthSessions.refreshTokenHash eq secretHash)
            }
        }
    }

    private fun normalizeLogin(login: String): String =
        login.trim().lowercase()

    private fun validateLogin(login: String) {
        if (!LOGIN_REGEX.matches(login)) throw AuthException("invalid_login")
    }

    private fun validatePassword(password: String) {
        if (password.length !in 8..128) throw AuthException("invalid_password")
    }

    companion object {
        private val LOGIN_REGEX = Regex("^[a-z0-9._-]{3,32}$")
    }
}
