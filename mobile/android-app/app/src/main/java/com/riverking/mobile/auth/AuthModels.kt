package com.riverking.mobile.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthUserDto(
    val id: Long,
    val username: String? = null,
    val needsNickname: Boolean,
    val language: String,
)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val accessTokenExpiresAt: Long,
    val refreshToken: String,
    val user: AuthUserDto,
)

@Serializable
data class TelegramLinkStartDto(
    val sessionToken: String,
    val telegramLink: String,
    val expiresAt: Long,
)

@Serializable
data class TelegramMobileLoginStatusDto(
    val status: String,
    val error: String? = null,
    val accessToken: String? = null,
    val accessTokenExpiresAt: Long? = null,
    val refreshToken: String? = null,
    val user: AuthUserDto? = null,
)

@Serializable
data class TelegramLinkStatusDto(
    val status: String,
    val error: String? = null,
    val telegramLinked: Boolean = false,
    val telegramUsername: String? = null,
)

@Serializable
data class PasswordRegisterRequest(
    val login: String,
    val password: String,
    val language: String? = null,
)

@Serializable
data class PasswordLoginRequest(
    val login: String,
    val password: String,
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class NicknameRequest(
    val nickname: String,
)

@Serializable
data class MeResponseDto(
    val id: Long,
    val username: String? = null,
    val needsNickname: Boolean,
    val telegramLinked: Boolean = false,
    val telegramUsername: String? = null,
    val lures: List<LureDto> = emptyList(),
    val currentLureId: Long? = null,
    val rods: List<RodDto> = emptyList(),
    val currentRodId: Long? = null,
    val totalWeight: Double = 0.0,
    val todayWeight: Double = 0.0,
    val locationId: Long = 0,
    val locations: List<LocationDto> = emptyList(),
    val caughtFishIds: List<Long> = emptyList(),
    val recent: List<RecentDto> = emptyList(),
    val language: String = "en",
    val coins: Long = 0,
    @SerialName("todayCoins") val todayCoins: Long = 0,
    val dailyAvailable: Boolean = false,
    val dailyStreak: Int = 0,
    val dailyRewards: List<List<DailyRewardItemDto>> = emptyList(),
    val autoFish: Boolean = false,
)

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Long,
)
