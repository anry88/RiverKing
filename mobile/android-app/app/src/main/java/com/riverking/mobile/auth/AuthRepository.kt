package com.riverking.mobile.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface TelegramLoginPollResult {
    data object Pending : TelegramLoginPollResult

    data object Expired : TelegramLoginPollResult

    data class Authorized(val me: MeResponseDto) : TelegramLoginPollResult

    data class Failed(val error: String) : TelegramLoginPollResult
}

sealed interface TelegramLinkPollResult {
    data object Pending : TelegramLinkPollResult

    data object Expired : TelegramLinkPollResult

    data class Completed(val me: MeResponseDto) : TelegramLinkPollResult

    data class Failed(val error: String) : TelegramLinkPollResult
}

class AuthRepository(
    private val sessionStore: SecureSessionStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
        }
        defaultRequest {
            accept(ContentType.Application.Json)
        }
    }

    private val api = AuthApi(client)

    suspend fun restoreProfile(): MeResponseDto? {
        sessionStore.read() ?: return null
        return runCatching { withFreshAccessToken { accessToken -> api.me(accessToken) } }
            .getOrElse {
                sessionStore.clear()
                null
            }
    }

    suspend fun registerPassword(login: String, password: String): MeResponseDto {
        sessionStore.writeLastLogin(login)
        val response = api.registerPassword(login, password)
        persist(response)
        return api.me(response.accessToken)
    }

    suspend fun loginPassword(login: String, password: String): MeResponseDto {
        sessionStore.writeLastLogin(login)
        val response = api.loginPassword(login, password)
        persist(response)
        return api.me(response.accessToken)
    }

    suspend fun loginGoogle(idToken: String): MeResponseDto {
        val response = api.loginGoogle(idToken)
        persist(response)
        return api.me(response.accessToken)
    }

    suspend fun startTelegramLogin(): TelegramLinkStartDto =
        api.startTelegramLogin()

    suspend fun pollTelegramLogin(sessionToken: String): TelegramLoginPollResult {
        val response = api.pollTelegramLogin(sessionToken)
        return when (response.status) {
            "pending" -> TelegramLoginPollResult.Pending
            "expired" -> TelegramLoginPollResult.Expired
            "authorized" -> {
                val authResponse = response.toAuthResponse()
                    ?: return TelegramLoginPollResult.Failed(humanizeError(response.error ?: "telegram_login_invalid"))
                persist(authResponse)
                TelegramLoginPollResult.Authorized(api.me(authResponse.accessToken))
            }
            else -> TelegramLoginPollResult.Failed(humanizeError(response.error ?: "telegram_login_failed"))
        }
    }

    suspend fun startTelegramLink(): TelegramLinkStartDto =
        withFreshAccessToken { accessToken -> api.startTelegramLink(accessToken) }

    suspend fun pollTelegramLink(sessionToken: String): TelegramLinkPollResult =
        withFreshAccessToken { accessToken ->
            val response = api.pollTelegramLink(accessToken, sessionToken)
            when (response.status) {
                "pending" -> TelegramLinkPollResult.Pending
                "expired" -> TelegramLinkPollResult.Expired
                "completed" -> TelegramLinkPollResult.Completed(api.me(accessToken))
                else -> TelegramLinkPollResult.Failed(humanizeError(response.error ?: "telegram_link_failed"))
            }
        }

    suspend fun updateNickname(nickname: String): MeResponseDto {
        return withFreshAccessToken { accessToken ->
            api.updateNickname(accessToken, nickname)
            api.me(accessToken)
        }
    }

    suspend fun refreshProfile(): MeResponseDto =
        withFreshAccessToken { accessToken -> api.me(accessToken) }

    suspend fun changeLocation(locationId: Long): MeResponseDto {
        withFreshAccessToken { accessToken -> api.changeLocation(accessToken, locationId) }
        return refreshProfile()
    }

    suspend fun changeLure(lureId: Long): MeResponseDto {
        withFreshAccessToken { accessToken -> api.changeLure(accessToken, lureId) }
        return refreshProfile()
    }

    suspend fun changeRod(rodId: Long): MeResponseDto {
        withFreshAccessToken { accessToken -> api.changeRod(accessToken, rodId) }
        return refreshProfile()
    }

    suspend fun claimDaily(): MeResponseDto {
        withFreshAccessToken { accessToken -> api.claimDaily(accessToken) }
        return refreshProfile()
    }

    suspend fun changeLanguage(language: String): MeResponseDto {
        withFreshAccessToken { accessToken -> api.updateLanguage(accessToken, language) }
        return refreshProfile()
    }

    suspend fun startCast(): StartCastResultDto =
        withFreshAccessToken { accessToken -> api.startCast(accessToken) }

    suspend fun hook(wait: Int, reaction: Double): HookResultDto =
        withFreshAccessToken { accessToken -> api.hook(accessToken, wait, reaction) }

    suspend fun cast(wait: Int, reaction: Double, success: Boolean): CastResultDto =
        withFreshAccessToken { accessToken -> api.cast(accessToken, wait, reaction, success) }

    suspend fun catchDetails(catchId: Long): CatchDto =
        withFreshAccessToken { accessToken -> api.catchDetails(accessToken, catchId) }

    suspend fun catchCard(catchId: Long): ByteArray =
        withFreshAccessToken { accessToken -> api.catchCard(accessToken, catchId) }

    suspend fun loadCurrentTournament(): CurrentTournamentDto? =
        withFreshAccessToken { accessToken -> api.currentTournament(accessToken) }

    suspend fun loadTournament(tournamentId: Long): CurrentTournamentDto =
        withFreshAccessToken { accessToken -> api.tournament(accessToken, tournamentId) }

    suspend fun loadUpcomingTournaments(): List<TournamentDto> =
        withFreshAccessToken { accessToken -> api.upcomingTournaments(accessToken) }

    suspend fun loadPastTournaments(): List<TournamentDto> =
        withFreshAccessToken { accessToken -> api.pastTournaments(accessToken) }

    suspend fun loadPendingPrizes(): List<PrizeDto> =
        withFreshAccessToken { accessToken -> api.prizes(accessToken) }

    suspend fun claimPrize(prizeId: Long): MeResponseDto {
        withFreshAccessToken { accessToken -> api.claimPrize(accessToken, prizeId) }
        return refreshProfile()
    }

    suspend fun loadGuide(): GuideDto =
        withFreshAccessToken { accessToken -> api.guide(accessToken) }

    suspend fun loadAchievements(): List<AchievementDto> =
        withFreshAccessToken { accessToken -> api.achievements(accessToken) }

    suspend fun claimAchievement(code: String): AchievementClaimDto =
        withFreshAccessToken { accessToken -> api.claimAchievement(accessToken, code) }

    suspend fun loadQuests(): QuestListDto =
        withFreshAccessToken { accessToken -> api.quests(accessToken) }

    suspend fun loadRatings(
        mode: String,
        filter: String,
        id: String,
        period: String,
        order: String,
    ): List<CatchDto> =
        withFreshAccessToken { accessToken ->
            api.ratings(accessToken, mode = mode, filter = filter, id = id, period = period, order = order)
        }

    suspend fun loadClub(): ClubDetailsDto? =
        withFreshAccessToken { accessToken -> api.club(accessToken) }

    suspend fun loadClubChat(): List<ClubChatMessageDto> =
        withFreshAccessToken { accessToken -> api.clubChat(accessToken) }

    suspend fun searchClubs(query: String?): List<ClubSummaryDto> =
        withFreshAccessToken { accessToken -> api.searchClubs(accessToken, query) }

    suspend fun createClub(name: String): ClubDetailsDto =
        withFreshAccessToken { accessToken -> api.createClub(accessToken, name) }

    suspend fun joinClub(clubId: Long): ClubDetailsDto =
        withFreshAccessToken { accessToken -> api.joinClub(accessToken, clubId) }

    suspend fun updateClubInfo(info: String): ClubDetailsDto =
        withFreshAccessToken { accessToken -> api.updateClubInfo(accessToken, info) }

    suspend fun updateClubSettings(minJoinWeightKg: Double, recruitingOpen: Boolean): ClubDetailsDto =
        withFreshAccessToken { accessToken -> api.updateClubSettings(accessToken, minJoinWeightKg, recruitingOpen) }

    suspend fun leaveClub() {
        withFreshAccessToken { accessToken -> api.leaveClub(accessToken) }
    }

    suspend fun clubMemberAction(memberId: Long, action: String): ClubDetailsDto =
        withFreshAccessToken { accessToken -> api.clubMemberAction(accessToken, memberId, action) }

    suspend fun loadShop(): List<ShopCategoryDto> =
        withFreshAccessToken { accessToken -> api.shop(accessToken) }

    suspend fun buyShopWithCoins(packId: String): MeResponseDto {
        withFreshAccessToken { accessToken -> api.buyShopWithCoins(accessToken, packId) }
        return refreshProfile()
    }

    suspend fun completePlayPurchase(
        packId: String,
        purchaseToken: String,
        orderId: String? = null,
        purchaseTimeMillis: Long? = null,
    ): MeResponseDto {
        withFreshAccessToken { accessToken ->
            api.completePlayPurchase(
                accessToken = accessToken,
                packId = packId,
                purchaseToken = purchaseToken,
                orderId = orderId,
                purchaseTimeMillis = purchaseTimeMillis,
            )
        }
        return refreshProfile()
    }

    suspend fun loadReferrals(): ReferralInfoDto =
        withFreshAccessToken { accessToken -> api.referrals(accessToken) }

    suspend fun generateReferral(): ReferralLinkDto =
        withFreshAccessToken { accessToken -> api.generateReferral(accessToken) }

    suspend fun loadReferralRewards(): List<ReferralRewardDto> =
        withFreshAccessToken { accessToken -> api.referralRewards(accessToken) }

    suspend fun claimReferralRewards(): MeResponseDto {
        withFreshAccessToken { accessToken -> api.claimReferralRewards(accessToken) }
        return refreshProfile()
    }

    suspend fun logout() {
        val stored = sessionStore.read()
        if (stored != null) {
            runCatching { api.logout(stored.refreshToken) }
        }
        sessionStore.clear()
    }

    suspend fun describeError(error: Throwable): String = when (error) {
        is ClientRequestException -> error.response.readErrorMessage() ?: "Request failed"
        is ServerResponseException -> error.response.readErrorMessage() ?: "Server error"
        is ResponseException -> error.response.readErrorMessage() ?: "Request failed"
        else -> humanizeThrowable(error)
    }

    fun isTransientNetworkError(error: Throwable): Boolean {
        val cause = error.rootCause()
        return cause is UnknownHostException ||
            cause is ConnectException ||
            cause is SocketTimeoutException ||
            cause is IOException
    }

    private fun humanizeThrowable(error: Throwable): String {
        val cause = error.rootCause()
        return when (cause) {
            is UnknownHostException -> "Can't reach the RiverKing server right now"
            is ConnectException -> "Can't connect to the RiverKing server right now"
            is SocketTimeoutException -> "The RiverKing server took too long to respond"
            else -> error.message ?: cause.message ?: "Request failed"
        }
    }

    private fun Throwable.rootCause(): Throwable {
        var current: Throwable = this
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current
    }

    private fun persist(response: AuthResponseDto) {
        sessionStore.write(
            StoredSession(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                accessTokenExpiresAt = response.accessTokenExpiresAt,
            )
        )
    }

    private fun TelegramMobileLoginStatusDto.toAuthResponse(): AuthResponseDto? {
        val accessToken = accessToken ?: return null
        val expiresAt = accessTokenExpiresAt ?: return null
        val refreshToken = refreshToken ?: return null
        val user = user ?: return null
        return AuthResponseDto(
            accessToken = accessToken,
            accessTokenExpiresAt = expiresAt,
            refreshToken = refreshToken,
            user = user,
        )
    }

    fun currentAccessToken(): String? = sessionStore.read()?.accessToken

    fun rememberLoginDraft(login: String) {
        if (login.isBlank()) return
        sessionStore.writeLastLogin(login)
    }

    fun lastLoginDraft(): String =
        sessionStore.readLastLogin().orEmpty()

    fun rememberPendingTelegramLogin(sessionToken: String) {
        sessionStore.writePendingTelegramLogin(sessionToken)
    }

    fun pendingTelegramLogin(): String? =
        sessionStore.readPendingTelegramLogin()

    fun clearPendingTelegramLogin() {
        sessionStore.clearPendingTelegramLogin()
    }

    fun rememberPendingTelegramLink(sessionToken: String) {
        sessionStore.writePendingTelegramLink(sessionToken)
    }

    fun pendingTelegramLink(): String? =
        sessionStore.readPendingTelegramLink()

    fun clearPendingTelegramLink() {
        sessionStore.clearPendingTelegramLink()
    }

    private suspend fun <T> withFreshAccessToken(block: suspend (String) -> T): T {
        val stored = sessionStore.read() ?: error("Missing session")
        return try {
            block(stored.accessToken)
        } catch (error: ClientRequestException) {
            if (error.response.status != HttpStatusCode.Unauthorized) throw error
            val refreshed = api.refresh(stored.refreshToken)
            persist(refreshed)
            block(refreshed.accessToken)
        }
    }

    private suspend fun HttpResponse.readErrorMessage(): String? {
        val raw = bodyAsText().trim()
        if (raw.isBlank()) return null
        val decoded = decodeErrorText(raw) ?: raw.removeSurrounding("\"")
        return humanizeError(decoded)
    }

    private fun decodeErrorText(raw: String): String? {
        val parsed = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        return parsed.stringValue("error") ?: parsed.stringValue("message")
    }

    private fun humanizeError(raw: String): String = when (raw) {
        "invalid_credentials" -> "Incorrect login or password"
        "login_taken" -> "This login is already taken"
        "invalid_login" -> "Use 3-32 lowercase letters, digits, dot, underscore, or dash"
        "invalid_password" -> "Password must be between 8 and 128 characters"
        "invalid_refresh_token", "refresh_expired" -> "Your session expired. Sign in again"
        "google_auth_disabled" -> "Google sign-in is not configured"
        "invalid_google_token" -> "Google sign-in could not be verified"
        "telegram_already_bound" -> "This Telegram account is already linked to another profile"
        "user_already_linked_to_other_telegram" -> "This profile is already linked to another Telegram account"
        "session_expired" -> "This confirmation link has expired"
        "invalid_session" -> "This confirmation session is no longer valid"
        "login_session_used" -> "This Telegram sign-in link has already been used"
        else -> raw
    }

    private fun JsonObject.stringValue(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}
