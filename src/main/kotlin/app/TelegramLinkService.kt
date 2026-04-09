package app

import db.AccountLinkSessions
import db.AuthIdentities
import db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import service.FishingService
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

class TelegramLinkService(
    private val env: Env,
    private val fishing: FishingService,
    private val auth: AuthService,
    private val clock: Clock = Clock.systemUTC(),
) {
    data class LinkStart(
        val sessionToken: String,
        val telegramLink: String,
        val expiresAt: Instant,
    )

    data class MobileLoginStatus(
        val status: String,
        val error: String? = null,
        val authResult: AuthService.AuthResult? = null,
    )

    data class LinkStatus(
        val status: String,
        val error: String? = null,
        val telegramLinked: Boolean = false,
        val telegramUsername: String? = null,
    )

    data class TelegramStartCommandResult(
        val kind: String,
        val code: String,
        val language: String,
    )

    private data class StoredSession(
        val id: String,
        val purpose: LinkPurpose,
        val status: String,
        val requesterUserId: Long?,
        val resolvedUserId: Long?,
        val telegramUserId: Long?,
        val telegramUsername: String?,
        val errorCode: String?,
        val expiresAt: Instant,
        val consumedAt: Instant?,
    )

    private enum class LinkPurpose(
        val value: String,
        val commandPrefix: String,
    ) {
        MOBILE_LOGIN("mobile_login", "login_"),
        TELEGRAM_LINK("telegram_link", "link_");

        companion object {
            fun fromValue(value: String): LinkPurpose? = entries.firstOrNull { it.value == value }
        }
    }

    fun startMobileLogin(): LinkStart = createSession(LinkPurpose.MOBILE_LOGIN, requesterUserId = null)

    fun startTelegramLink(userId: Long): LinkStart = createSession(LinkPurpose.TELEGRAM_LINK, requesterUserId = userId)

    fun pollMobileLogin(sessionToken: String, deviceInfo: String? = null): MobileLoginStatus {
        val session = transaction { loadSession(sessionToken, LinkPurpose.MOBILE_LOGIN) }
            ?: return MobileLoginStatus(status = "failed", error = "invalid_session")
        if (session.status == "pending" && session.expiresAt.isBefore(now())) {
            expireSession(session.id)
            return MobileLoginStatus(status = "expired", error = "session_expired")
        }
        return when (session.status) {
            "pending" -> MobileLoginStatus(status = "pending")
            "failed" -> {
                val error = session.errorCode ?: "link_failed"
                val status = if (error == "session_expired") "expired" else "failed"
                MobileLoginStatus(status = status, error = error)
            }
            "consumed" -> MobileLoginStatus(status = "failed", error = "login_session_used")
            "completed" -> {
                val resolvedUserId = session.resolvedUserId
                    ?: return MobileLoginStatus(status = "failed", error = "invalid_session")
                if (session.consumedAt != null) {
                    return MobileLoginStatus(status = "failed", error = "login_session_used")
                }
                val authResult = auth.issueTokens(
                    userId = fishing.ensureUserById(resolvedUserId),
                    deviceInfo = deviceInfo,
                )
                transaction {
                    AccountLinkSessions.update({ AccountLinkSessions.id eq session.id }) {
                        it[AccountLinkSessions.status] = "consumed"
                        it[AccountLinkSessions.consumedAt] = now()
                    }
                }
                MobileLoginStatus(status = "authorized", authResult = authResult)
            }
            else -> MobileLoginStatus(status = "failed", error = "invalid_session")
        }
    }

    fun pollTelegramLink(userId: Long, sessionToken: String): LinkStatus {
        val session = transaction { loadSession(sessionToken, LinkPurpose.TELEGRAM_LINK) }
            ?: return LinkStatus(status = "failed", error = "invalid_session")
        if (session.requesterUserId != userId) {
            return LinkStatus(status = "failed", error = "forbidden")
        }
        if (session.status == "pending" && session.expiresAt.isBefore(now())) {
            expireSession(session.id)
            return LinkStatus(status = "expired", error = "session_expired")
        }
        return when (session.status) {
            "pending" -> LinkStatus(status = "pending")
            "failed" -> {
                val error = session.errorCode ?: "link_failed"
                val status = if (error == "session_expired") "expired" else "failed"
                LinkStatus(status = status, error = error)
            }
            "completed", "consumed" -> LinkStatus(
                status = "completed",
                telegramLinked = true,
                telegramUsername = session.telegramUsername ?: currentTelegramUsername(userId),
            )
            else -> LinkStatus(status = "failed", error = "invalid_session")
        }
    }

    fun completeFromTelegramCommand(
        rawToken: String,
        tgUser: TgWebAppAuth.TgUser,
    ): TelegramStartCommandResult {
        return when {
            rawToken.startsWith(LinkPurpose.MOBILE_LOGIN.commandPrefix) -> {
                completeMobileLogin(rawToken.removePrefix(LinkPurpose.MOBILE_LOGIN.commandPrefix), tgUser)
            }
            rawToken.startsWith(LinkPurpose.TELEGRAM_LINK.commandPrefix) -> {
                completeTelegramLink(rawToken.removePrefix(LinkPurpose.TELEGRAM_LINK.commandPrefix), tgUser)
            }
            else -> TelegramStartCommandResult(
                kind = "unknown",
                code = "invalid_session",
                language = normalizeLanguage(tgUser.languageCode),
            )
        }
    }

    private fun createSession(purpose: LinkPurpose, requesterUserId: Long?): LinkStart {
        val token = generateToken()
        val expiresAt = now().plus(SESSION_TTL)
        transaction {
            AccountLinkSessions.insert {
                it[AccountLinkSessions.id] = token
                it[AccountLinkSessions.purpose] = purpose.value
                it[AccountLinkSessions.status] = "pending"
                it[AccountLinkSessions.requesterUserId] = requesterUserId
                it[AccountLinkSessions.resolvedUserId] = null
                it[AccountLinkSessions.telegramUserId] = null
                it[AccountLinkSessions.telegramUsername] = null
                it[AccountLinkSessions.errorCode] = null
                it[AccountLinkSessions.createdAt] = now()
                it[AccountLinkSessions.expiresAt] = expiresAt
                it[AccountLinkSessions.completedAt] = null
                it[AccountLinkSessions.consumedAt] = null
            }
        }
        return LinkStart(
            sessionToken = token,
            telegramLink = "https://t.me/${env.botName}?start=${purpose.commandPrefix}$token",
            expiresAt = expiresAt,
        )
    }

    private fun completeMobileLogin(
        sessionToken: String,
        tgUser: TgWebAppAuth.TgUser,
    ): TelegramStartCommandResult {
        val current = transaction { loadSession(sessionToken, LinkPurpose.MOBILE_LOGIN) }
            ?: return TelegramStartCommandResult("mobile_login", "invalid_session", normalizeLanguage(tgUser.languageCode))
        if (current.status == "pending" && current.expiresAt.isBefore(now())) {
            expireSession(sessionToken)
            return TelegramStartCommandResult("mobile_login", "session_expired", normalizeLanguage(tgUser.languageCode))
        }
        if (current.status == "failed") {
            return TelegramStartCommandResult(
                kind = "mobile_login",
                code = current.errorCode ?: "link_failed",
                language = normalizeLanguage(tgUser.languageCode),
            )
        }
        if (current.status == "completed" || current.status == "consumed") {
            val linkedLanguage = current.resolvedUserId?.let { runCatching { fishing.userLanguage(it) }.getOrNull() }
                ?: normalizeLanguage(tgUser.languageCode)
            return TelegramStartCommandResult("mobile_login", "already_completed", linkedLanguage)
        }

        val userId = fishing.ensureUserByTgId(
            tgId = tgUser.id,
            firstName = tgUser.firstName,
            lastName = tgUser.lastName,
            username = tgUser.username,
            language = tgUser.languageCode,
        )
        val language = fishing.userLanguage(userId)
        transaction {
            val session = loadSession(sessionToken, LinkPurpose.MOBILE_LOGIN)
                ?: return@transaction
            if (session.status != "pending") return@transaction
            AccountLinkSessions.update({ AccountLinkSessions.id eq sessionToken }) {
                it[AccountLinkSessions.status] = "completed"
                it[AccountLinkSessions.resolvedUserId] = userId
                it[AccountLinkSessions.telegramUserId] = tgUser.id
                it[AccountLinkSessions.telegramUsername] = tgUser.username
                it[AccountLinkSessions.errorCode] = null
                it[AccountLinkSessions.completedAt] = now()
            }
        }
        return TelegramStartCommandResult("mobile_login", "completed", language)
    }

    private fun completeTelegramLink(
        sessionToken: String,
        tgUser: TgWebAppAuth.TgUser,
    ): TelegramStartCommandResult {
        val fallbackLanguage = normalizeLanguage(tgUser.languageCode)
        return transaction {
            val session = loadSession(sessionToken, LinkPurpose.TELEGRAM_LINK)
                ?: return@transaction TelegramStartCommandResult("telegram_link", "invalid_session", fallbackLanguage)
            val targetUserId = session.requesterUserId
                ?: return@transaction TelegramStartCommandResult("telegram_link", "invalid_session", fallbackLanguage)
            val userRow = Users.select { Users.id eq targetUserId }.singleOrNull()
                ?: return@transaction TelegramStartCommandResult("telegram_link", "invalid_session", fallbackLanguage)
            val language = userRow[Users.language]

            if (session.status == "pending" && session.expiresAt.isBefore(now())) {
                failSession(sessionToken, "session_expired")
                return@transaction TelegramStartCommandResult("telegram_link", "session_expired", language)
            }
            if (session.status == "failed") {
                return@transaction TelegramStartCommandResult(
                    kind = "telegram_link",
                    code = session.errorCode ?: "link_failed",
                    language = language,
                )
            }
            if (session.status == "completed" || session.status == "consumed") {
                return@transaction TelegramStartCommandResult("telegram_link", "already_completed", language)
            }

            val existingLinkedUserId = findUserByTelegramId(tgUser.id)
            if (existingLinkedUserId != null && existingLinkedUserId != targetUserId) {
                failSession(sessionToken, "telegram_already_bound")
                return@transaction TelegramStartCommandResult("telegram_link", "telegram_already_bound", language)
            }

            val existingIdentity = AuthIdentities.select {
                (AuthIdentities.userId eq targetUserId) and
                    (AuthIdentities.provider eq TELEGRAM_PROVIDER)
            }.singleOrNull()
            val existingSubject = existingIdentity?.get(AuthIdentities.subject)
            if (existingSubject != null && existingSubject != tgUser.id.toString()) {
                failSession(sessionToken, "user_already_linked_to_other_telegram")
                return@transaction TelegramStartCommandResult(
                    "telegram_link",
                    "user_already_linked_to_other_telegram",
                    language,
                )
            }
            val currentTgId = userRow[Users.tgId]
            if (currentTgId != null && currentTgId != tgUser.id) {
                failSession(sessionToken, "user_already_linked_to_other_telegram")
                return@transaction TelegramStartCommandResult(
                    "telegram_link",
                    "user_already_linked_to_other_telegram",
                    language,
                )
            }

            Users.update({ Users.id eq targetUserId }) {
                it[Users.tgId] = tgUser.id
                if (tgUser.firstName != null) it[Users.firstName] = tgUser.firstName
                if (tgUser.lastName != null) it[Users.lastName] = tgUser.lastName
                if (tgUser.username != null) it[Users.username] = tgUser.username
                it[Users.lastSeenAt] = now()
            }

            if (existingIdentity == null) {
                AuthIdentities.insert {
                    it[AuthIdentities.userId] = targetUserId
                    it[AuthIdentities.provider] = TELEGRAM_PROVIDER
                    it[AuthIdentities.subject] = tgUser.id.toString()
                    it[AuthIdentities.email] = null
                    it[AuthIdentities.emailVerified] = false
                    it[AuthIdentities.createdAt] = now()
                    it[AuthIdentities.lastLoginAt] = now()
                }
            } else {
                AuthIdentities.update({ AuthIdentities.id eq existingIdentity[AuthIdentities.id].value }) {
                    it[AuthIdentities.userId] = targetUserId
                    it[AuthIdentities.subject] = tgUser.id.toString()
                    it[AuthIdentities.email] = null
                    it[AuthIdentities.emailVerified] = false
                    it[AuthIdentities.lastLoginAt] = now()
                }
            }

            AccountLinkSessions.update({ AccountLinkSessions.id eq sessionToken }) {
                it[AccountLinkSessions.status] = "completed"
                it[AccountLinkSessions.resolvedUserId] = targetUserId
                it[AccountLinkSessions.telegramUserId] = tgUser.id
                it[AccountLinkSessions.telegramUsername] = tgUser.username
                it[AccountLinkSessions.errorCode] = null
                it[AccountLinkSessions.completedAt] = now()
            }
            TelegramStartCommandResult("telegram_link", "completed", language)
        }
    }

    private fun expireSession(sessionToken: String) {
        transaction {
            failSession(sessionToken, "session_expired")
        }
    }

    private fun failSession(sessionToken: String, error: String) {
        AccountLinkSessions.update({ AccountLinkSessions.id eq sessionToken }) {
            it[AccountLinkSessions.status] = "failed"
            it[AccountLinkSessions.errorCode] = error
        }
    }

    private fun currentTelegramUsername(userId: Long): String? = transaction {
        Users.select { Users.id eq userId }.singleOrNull()?.get(Users.username)
    }

    private fun findUserByTelegramId(telegramId: Long): Long? {
        val subject = telegramId.toString()
        val userFromUsers = Users
            .select { Users.tgId eq telegramId }
            .singleOrNull()
            ?.get(Users.id)
            ?.value
        if (userFromUsers != null) return userFromUsers
        return AuthIdentities
            .select {
                (AuthIdentities.provider eq TELEGRAM_PROVIDER) and
                    (AuthIdentities.subject eq subject)
            }
            .singleOrNull()
            ?.get(AuthIdentities.userId)
            ?.value
    }

    private fun loadSession(sessionToken: String, purpose: LinkPurpose): StoredSession? {
        val row = AccountLinkSessions
            .select {
                (AccountLinkSessions.id eq sessionToken) and
                    (AccountLinkSessions.purpose eq purpose.value)
            }
            .singleOrNull() ?: return null
        val storedPurpose = LinkPurpose.fromValue(row[AccountLinkSessions.purpose]) ?: return null
        return StoredSession(
            id = row[AccountLinkSessions.id],
            purpose = storedPurpose,
            status = row[AccountLinkSessions.status],
            requesterUserId = row[AccountLinkSessions.requesterUserId]?.value,
            resolvedUserId = row[AccountLinkSessions.resolvedUserId]?.value,
            telegramUserId = row[AccountLinkSessions.telegramUserId],
            telegramUsername = row[AccountLinkSessions.telegramUsername],
            errorCode = row[AccountLinkSessions.errorCode],
            expiresAt = row[AccountLinkSessions.expiresAt],
            consumedAt = row[AccountLinkSessions.consumedAt],
        )
    }

    private fun generateToken(): String =
        UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")

    private fun normalizeLanguage(raw: String?): String =
        if (raw?.startsWith("ru", ignoreCase = true) == true) "ru" else "en"

    private fun now(): Instant = clock.instant()

    companion object {
        private const val TELEGRAM_PROVIDER = "telegram"
        private val SESSION_TTL: Duration = Duration.ofMinutes(10)
    }
}
