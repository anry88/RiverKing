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
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepository(
    private val sessionStore: SecureSessionStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
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
        val response = api.registerPassword(login, password)
        persist(response)
        return api.me(response.accessToken)
    }

    suspend fun loginPassword(login: String, password: String): MeResponseDto {
        val response = api.loginPassword(login, password)
        persist(response)
        return api.me(response.accessToken)
    }

    suspend fun loginGoogle(idToken: String): MeResponseDto {
        val response = api.loginGoogle(idToken)
        persist(response)
        return api.me(response.accessToken)
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

    suspend fun performQuickCast(): FishingFlowResult {
        val wait = 1_250
        val reaction = 0.82
        val start = withFreshAccessToken { accessToken -> api.startCast(accessToken) }
        val hook = withFreshAccessToken { accessToken -> api.hook(accessToken, wait, reaction) }
        val cast = withFreshAccessToken { accessToken -> api.cast(accessToken, wait, reaction, hook.success) }
        val me = refreshProfile()
        return FishingFlowResult(
            start = start,
            hook = hook,
            cast = cast,
            me = me,
        )
    }

    suspend fun loadCurrentTournament(): CurrentTournamentDto? =
        withFreshAccessToken { accessToken -> api.currentTournament(accessToken) }

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

    suspend fun loadQuests(): QuestListDto =
        withFreshAccessToken { accessToken -> api.quests(accessToken) }

    suspend fun loadPersonalLocationRatings(locationId: String = "all"): List<CatchDto> =
        withFreshAccessToken { accessToken -> api.personalLocationRatings(accessToken, locationId) }

    suspend fun loadGlobalLocationRatings(locationId: String = "all"): List<CatchDto> =
        withFreshAccessToken { accessToken -> api.globalLocationRatings(accessToken, locationId) }

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
        else -> error.message ?: "Request failed"
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
        return decodeErrorText(raw) ?: raw.removeSurrounding("\"")
    }

    private fun decodeErrorText(raw: String): String? {
        val parsed = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        return parsed.stringValue("error") ?: parsed.stringValue("message")
    }

    private fun JsonObject.stringValue(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}
