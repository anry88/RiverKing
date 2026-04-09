package com.riverking.mobile.auth

import com.riverking.mobile.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.contentType
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType

class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) {
    suspend fun registerPassword(login: String, password: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/password/register") {
            contentType(ContentType.Application.Json)
            setBody(PasswordRegisterRequest(login = login, password = password))
        }.body()

    suspend fun loginPassword(login: String, password: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/password/login") {
            contentType(ContentType.Application.Json)
            setBody(PasswordLoginRequest(login = login, password = password))
        }.body()

    suspend fun loginGoogle(idToken: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleAuthRequest(idToken = idToken))
        }.body()

    suspend fun refresh(refreshToken: String): AuthResponseDto =
        client.post("$baseUrl/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }.body()

    suspend fun logout(refreshToken: String) {
        client.post("$baseUrl/api/auth/logout") {
            contentType(ContentType.Application.Json)
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
            contentType(ContentType.Application.Json)
            setBody(NicknameRequest(nickname))
        }
    }
}
