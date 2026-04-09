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
}
