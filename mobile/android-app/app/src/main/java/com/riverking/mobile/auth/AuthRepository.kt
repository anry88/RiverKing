package com.riverking.mobile.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

class AuthRepository(
    private val sessionStore: SecureSessionStore,
) {
    private val client = HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
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

    suspend fun logout() {
        val stored = sessionStore.read()
        if (stored != null) {
            runCatching { api.logout(stored.refreshToken) }
        }
        sessionStore.clear()
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
}
