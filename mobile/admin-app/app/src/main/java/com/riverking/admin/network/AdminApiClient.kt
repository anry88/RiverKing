package com.riverking.admin.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
    val startTime: Long,
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

@Serializable
data class BroadcastResp(
    val status: String = "",
    val count: Int = 0
)

@Serializable
data class CatalogOptionDTO(
    val id: String,
    val label: String
)

@Serializable
data class PrizeOptionDTO(
    val id: String,
    val label: String,
    val defaultQty: Int,
    val coins: Boolean = false
)

@Serializable
data class DiscountPackageDTO(
    val id: String,
    val name: String,
    val category: String,
    val basePrice: Int,
    val currentPrice: Int,
    val activeDiscountPrice: Int? = null,
    val activeDiscountStart: String? = null,
    val activeDiscountEnd: String? = null
)

@Serializable
data class AdminCatalogDTO(
    val metrics: List<CatalogOptionDTO> = emptyList(),
    val fish: List<CatalogOptionDTO> = emptyList(),
    val locations: List<CatalogOptionDTO> = emptyList(),
    val tournamentPrizes: List<PrizeOptionDTO> = emptyList(),
    val discountPackages: List<DiscountPackageDTO> = emptyList()
)

class AdminApiClient(
    var baseUrl: String = "",
    var token: String = ""
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun apiUrl(path: String): String {
        val base = baseUrl.trimEnd('/')
        return "$base$path"
    }

    suspend fun getTournaments(offset: Int = 0, limit: Int = 10): List<TournamentDTO> {
        val response = client.get(apiUrl("/api/admin/tournaments?offset=$offset&limit=$limit")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
        return response.body()
    }

    suspend fun getCatalog(): AdminCatalogDTO {
        val response = client.get(apiUrl("/api/admin/catalog")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
        return response.body()
    }

    suspend fun createTournament(req: TournamentReq) {
        val response = client.post(apiUrl("/api/admin/tournaments")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
    }

    suspend fun updateTournament(id: Long, req: TournamentReq) {
        val response = client.put(apiUrl("/api/admin/tournaments/$id")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
    }

    suspend fun deleteTournament(id: Long) {
        val response = client.delete(apiUrl("/api/admin/tournaments/$id")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
    }

    suspend fun getDiscounts(): List<DiscountDTO> {
        val response = client.get(apiUrl("/api/admin/discounts")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
        return response.body()
    }

    suspend fun createDiscount(req: DiscountReq) {
        val response = client.post(apiUrl("/api/admin/discounts")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
    }

    suspend fun deleteDiscount(packageId: String) {
        val response = client.delete(apiUrl("/api/admin/discounts/$packageId")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
    }

    suspend fun sendBroadcast(req: BroadcastReq): BroadcastResp {
        val response = client.post(apiUrl("/api/admin/broadcast")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) throw Exception("Failed: ${response.status}")
        return response.body()
    }
}
