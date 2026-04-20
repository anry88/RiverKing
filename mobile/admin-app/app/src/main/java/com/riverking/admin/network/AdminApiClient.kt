package com.riverking.admin.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TournamentDTO(
    val id: Long = 0,
    val nameRu: String,
    val nameEn: String,
    val startTime: Long, // Epoch millis. Note: backend sends/expects Instant if using kotlinx.serialization but we used Long in our AdminApiRoutes!
    val endTime: Long,
    val fish: String? = null,
    val location: String? = null,
    val metric: String,
    val prizePlaces: Int,
    val prizesJson: String
)

@Serializable
data class TournamentReq(
    val nameRu: String,
    val nameEn: String,
    val startTime: Long,
    val endTime: Long,
    val fish: String?,
    val location: String?,
    val metric: String,
    val prizePlaces: Int,
    val prizesJson: String
)

@Serializable
data class DiscountDTO(
    val packageId: String,
    val price: Int,
    val startDate: String,
    val endDate: String
)

@Serializable
data class DiscountReq(
    val packageId: String,
    val price: Int,
    val start: String,
    val end: String
)

@Serializable
data class BroadcastReq(
    val textRu: String,
    val textEn: String
)

class AdminApiClient(
    var baseUrl: String = "",
    var token: String = ""
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            if (baseUrl.isNotBlank()) {
                url(baseUrl)
            }
            if (token.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    suspend fun getTournaments(): List<TournamentDTO> {
        val response = client.get("/api/admin/tournaments")
        if (!response.status.isSuccess()) throw Exception("Failed to load tournaments: ${response.status}")
        return response.body()
    }

    suspend fun createTournament(req: TournamentReq) {
        val response = client.post("/api/admin/tournaments") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) throw Exception("Failed to create tournament: ${response.status}")
    }

    suspend fun updateTournament(id: Long, req: TournamentReq) {
        val response = client.put("/api/admin/tournaments/$id") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) throw Exception("Failed to update tournament: ${response.status}")
    }

    suspend fun deleteTournament(id: Long) {
        val response = client.delete("/api/admin/tournaments/$id")
        if (!response.status.isSuccess()) throw Exception("Failed to delete tournament: ${response.status}")
    }

    suspend fun getDiscounts(): List<DiscountDTO> {
        val response = client.get("/api/admin/discounts")
        if (!response.status.isSuccess()) throw Exception("Failed to load discounts: ${response.status}")
        return response.body()
    }

    suspend fun createDiscount(req: DiscountReq) {
        val response = client.post("/api/admin/discounts") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) throw Exception("Failed to create discount: ${response.status}")
    }

    suspend fun deleteDiscount(packageId: String) {
        val response = client.delete("/api/admin/discounts/$packageId")
        if (!response.status.isSuccess()) throw Exception("Failed to delete discount: ${response.status}")
    }

    suspend fun sendBroadcast(req: BroadcastReq) {
        val response = client.post("/api/admin/broadcast") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) throw Exception("Failed to send broadcast: ${response.status}")
    }
}
