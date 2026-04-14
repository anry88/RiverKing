package com.riverking.mobile.auth

import com.riverking.mobile.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) {
    suspend fun registerPassword(login: String, password: String, refToken: String? = null): AuthResponseDto =
        client.post("$baseUrl/api/auth/password/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(PasswordRegisterRequest(login = login, password = password, ref = refToken))
        }.body()

    suspend fun loginPassword(login: String, password: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/password/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(PasswordLoginRequest(login = login, password = password))
        }.body()

    suspend fun loginGoogle(idToken: String, refToken: String? = null): AuthResponseDto =
        client.post("$baseUrl/api/auth/google") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(GoogleAuthRequest(idToken = idToken, ref = refToken))
        }.body()

    suspend fun startTelegramLogin(): TelegramLinkStartDto =
        client.post("$baseUrl/api/auth/telegram/mobile/start").body()

    suspend fun pollTelegramLogin(sessionToken: String): TelegramMobileLoginStatusDto =
        client.get("$baseUrl/api/auth/telegram/mobile/status/$sessionToken").body()

    suspend fun startTelegramLink(accessToken: String): TelegramLinkStartDto =
        client.post("$baseUrl/api/auth/telegram/link/start") {
            bearerAuth(accessToken)
        }.body()

    suspend fun pollTelegramLink(accessToken: String, sessionToken: String): TelegramLinkStatusDto =
        client.get("$baseUrl/api/auth/telegram/link/status/$sessionToken") {
            bearerAuth(accessToken)
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

    suspend fun updateLanguage(accessToken: String, language: String) {
        client.post("$baseUrl/api/language") {
            bearerAuth(accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(mapOf("language" to language))
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

    suspend fun catchDetails(accessToken: String, catchId: Long): CatchDto =
        client.get("$baseUrl/api/catches/$catchId") {
            bearerAuth(accessToken)
        }.body()

    suspend fun catchCard(accessToken: String, catchId: Long): ByteArray =
        client.get("$baseUrl/api/catches/$catchId/card") {
            bearerAuth(accessToken)
        }.body()

    suspend fun currentTournament(accessToken: String): CurrentTournamentDto? {
        val response = client.get("$baseUrl/api/tournament/current") {
            bearerAuth(accessToken)
        }
        return if (response.status == HttpStatusCode.NoContent) null else response.body()
    }

    suspend fun tournament(accessToken: String, tournamentId: Long): CurrentTournamentDto =
        client.get("$baseUrl/api/tournament/$tournamentId") {
            bearerAuth(accessToken)
        }.body()

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

    suspend fun claimAchievement(accessToken: String, code: String): AchievementClaimDto =
        client.post("$baseUrl/api/achievements/$code/claim") {
            bearerAuth(accessToken)
        }.body()

    suspend fun quests(accessToken: String): QuestListDto =
        client.get("$baseUrl/api/quests") {
            bearerAuth(accessToken)
        }.body()

    suspend fun ratings(
        accessToken: String,
        mode: String,
        filter: String,
        id: String,
        period: String,
        order: String,
    ): List<CatchDto> =
        client.get("$baseUrl/api/ratings/$mode/$filter/$id") {
            bearerAuth(accessToken)
            parameter("period", period)
            parameter("order", order)
        }.body()

    suspend fun club(accessToken: String): ClubDetailsDto? {
        val response = client.get("$baseUrl/api/club") {
            bearerAuth(accessToken)
        }
        return if (response.status == HttpStatusCode.NoContent) null else response.body()
    }

    suspend fun clubChat(accessToken: String): List<ClubChatMessageDto> =
        client.get("$baseUrl/api/club/chat") {
            bearerAuth(accessToken)
        }.body()

    suspend fun searchClubs(accessToken: String, query: String?): List<ClubSummaryDto> =
        client.get("$baseUrl/api/club/search") {
            bearerAuth(accessToken)
            if (!query.isNullOrBlank()) parameter("q", query)
        }.body()

    suspend fun createClub(accessToken: String, name: String): ClubDetailsDto =
        client.post("$baseUrl/api/club/create") {
            bearerAuth(accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(ClubCreateRequestDto(name))
        }.body()

    suspend fun joinClub(accessToken: String, clubId: Long): ClubDetailsDto =
        client.post("$baseUrl/api/club/$clubId/join") {
            bearerAuth(accessToken)
        }.body()

    suspend fun updateClubInfo(accessToken: String, info: String): ClubDetailsDto =
        client.post("$baseUrl/api/club/info") {
            bearerAuth(accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(ClubInfoRequestDto(info))
        }.body()

    suspend fun updateClubSettings(
        accessToken: String,
        minJoinWeightKg: Double,
        recruitingOpen: Boolean,
    ): ClubDetailsDto =
        client.post("$baseUrl/api/club/settings") {
            bearerAuth(accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(ClubSettingsRequestDto(minJoinWeightKg, recruitingOpen))
        }.body()

    suspend fun leaveClub(accessToken: String) {
        client.post("$baseUrl/api/club/leave") {
            bearerAuth(accessToken)
        }
    }

    suspend fun clubMemberAction(accessToken: String, memberId: Long, action: String): ClubDetailsDto =
        client.post("$baseUrl/api/club/members/$memberId/$action") {
            bearerAuth(accessToken)
        }.body()

    suspend fun shop(accessToken: String): List<ShopCategoryDto> =
        client.get("$baseUrl/api/shop") {
            bearerAuth(accessToken)
        }.body()

    suspend fun buyShopWithCoins(accessToken: String, packId: String): ShopPurchaseResultDto =
        client.post("$baseUrl/api/shop/$packId/coins") {
            bearerAuth(accessToken)
        }.body()

    suspend fun completePlayPurchase(
        accessToken: String,
        packId: String,
        purchaseToken: String,
        orderId: String? = null,
        purchaseTimeMillis: Long? = null,
    ): ShopPurchaseResultDto =
        client.post("$baseUrl/api/shop/$packId/play/complete") {
            bearerAuth(accessToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                PlayPurchaseCompleteRequestDto(
                    purchaseToken = purchaseToken,
                    orderId = orderId,
                    purchaseTimeMillis = purchaseTimeMillis,
                )
            )
        }.body()

    suspend fun referrals(accessToken: String): ReferralInfoDto =
        client.get("$baseUrl/api/referrals") {
            bearerAuth(accessToken)
        }.body()

    suspend fun generateReferral(accessToken: String): ReferralLinkDto =
        client.post("$baseUrl/api/referrals") {
            bearerAuth(accessToken)
        }.body()

    suspend fun referralRewards(accessToken: String): List<ReferralRewardDto> =
        client.get("$baseUrl/api/referrals/rewards") {
            bearerAuth(accessToken)
        }.body()

    suspend fun claimReferralRewards(accessToken: String): ShopPurchaseResultDto =
        client.post("$baseUrl/api/referrals/rewards/claim") {
            bearerAuth(accessToken)
        }.body()

    suspend fun catchStats(accessToken: String, period: String): CatchStatsDto =
        client.get("$baseUrl/api/stats/catch") {
            bearerAuth(accessToken)
            parameter("period", period)
        }.body()
}
