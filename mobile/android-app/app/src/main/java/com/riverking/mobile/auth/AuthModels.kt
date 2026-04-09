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
    val totalWeight: Double = 0.0,
    val todayWeight: Double = 0.0,
    val language: String = "en",
    val coins: Long = 0,
    @SerialName("todayCoins") val todayCoins: Long = 0,
    val dailyAvailable: Boolean = false,
)

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Long,
)
