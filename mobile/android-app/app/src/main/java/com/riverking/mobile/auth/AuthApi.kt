package com.riverking.mobile.auth

import com.riverking.mobile.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) {
    suspend fun registerPassword(login: String, password: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/password/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(PasswordRegisterRequest(login = login, password = password))
        }.body()

    suspend fun loginPassword(login: String, password: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/password/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(PasswordLoginRequest(login = login, password = password))
        }.body()

    suspend fun loginGoogle(idToken: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/google") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(GoogleAuthRequest(idToken = idToken))
        }.body()

    suspend fun refresh(refreshToken: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/refresh") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RefreshRequest(refreshToken))
        }.body()

    suspend fun logout(refreshToken: String) {
        client.post("$baseUrl/api/auth/logout") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RefreshRequest(refreshToken))
        }
    }

    suspend fun me(accessToken: String): MeResponseDto =
        client.get("$baseUrl/api/me") {
            bearerAuth(accessToken)
        }.body()

    suspend fun updateNickname(accessToken: String, nickname: String) {
        client.post("$baseUrl/api/nickname") {
            bearerAuth(accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(NicknameRequest(nickname))
        }
    }

    suspend fun changeLocation(accessToken: String, locationId: Long) {
        client.post("$baseUrl/api/location/$locationId") {
            bearerAuth(accessToken)
        }
    }

    suspend fun changeLure(accessToken: String, lureId: Long) {
        client.post("$baseUrl/api/lure/$lureId") {
            bearerAuth(accessToken)
        }
    }

    suspend fun changeRod(accessToken: String, rodId: Long) {
        client.post("$baseUrl/api/rod/$rodId") {
            bearerAuth(accessToken)
        }
    }

    suspend fun claimDaily(accessToken: String): DailyClaimResponseDto =
        client.post("$baseUrl/api/daily") {
            bearerAuth(accessToken)
        }.body()

    suspend fun startCast(accessToken: String): StartCastResultDto =
        client.post("$baseUrl/api/start-cast") {
            bearerAuth(accessToken)
        }.body()

    suspend fun hook(accessToken: String, wait: Int, reaction: Double): HookResultDto =
        client.post("$baseUrl/api/hook") {
            bearerAuth(accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(HookRequestDto(wait = wait, reaction = reaction))
        }.body()

    suspend fun cast(accessToken: String, wait: Int, reaction: Double, success: Boolean): CastResultDto =
        client.post("$baseUrl/api/cast") {
            bearerAuth(accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CastRequestDto(wait = wait, reaction = reaction, success = success))
        }.body()

    suspend fun currentTournament(accessToken: String): CurrentTournamentDto? {
        val response = client.get("$baseUrl/api/tournament/current") {
            bearerAuth(accessToken)
        }
        return if (response.status == HttpStatusCode.NoContent) null else response.body()
    }

    suspend fun upcomingTournaments(accessToken: String): List<TournamentDto> =
        client.get("$baseUrl/api/tournaments/upcoming") {
            bearerAuth(accessToken)
        }.body()

    suspend fun pastTournaments(accessToken: String): List<TournamentDto> =
        client.get("$baseUrl/api/tournaments/past") {
            bearerAuth(accessToken)
        }.body()

    suspend fun prizes(accessToken: String): List<PrizeDto> =
        client.get("$baseUrl/api/prizes") {
            bearerAuth(accessToken)
        }.body()

    suspend fun claimPrize(accessToken: String, prizeId: Long) {
        client.post("$baseUrl/api/prizes/$prizeId/claim") {
            bearerAuth(accessToken)
        }
    }

    suspend fun guide(accessToken: String): GuideDto =
        client.get("$baseUrl/api/guide") {
            bearerAuth(accessToken)
        }.body()

    suspend fun achievements(accessToken: String): List<AchievementDto> =
        client.get("$baseUrl/api/achievements") {
            bearerAuth(accessToken)
        }.body()

    suspend fun quests(accessToken: String): QuestListDto =
        client.get("$baseUrl/api/quests") {
            bearerAuth(accessToken)
        }.body()

    suspend fun personalLocationRatings(accessToken: String, locationId: String): List<CatchDto> =
        client.get("$baseUrl/api/ratings/personal/location/$locationId") {
            bearerAuth(accessToken)
        }.body()

    suspend fun globalLocationRatings(accessToken: String, locationId: String): List<CatchDto> =
        client.get("$baseUrl/api/ratings/global/location/$locationId") {
            bearerAuth(accessToken)
        }.body()
}
