package app

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TelegramApiException(val code: Int, message: String) : IOException(message)

@Serializable
private data class AnswerInlineQueryRequest(
    @SerialName("inline_query_id") val inlineQueryId: String,
    val results: List<InlineQueryResultArticle>,
    @SerialName("cache_time") val cacheTime: Int = 0,
)

class TelegramBot(
    private val token: String,
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    },
) {
    private val baseUrl = "https://api.telegram.org/bot$token"

    private suspend fun HttpResponse.ensureSuccess() {
        val body = bodyAsText()
        if (!status.isSuccess()) {
            val desc = runCatching {
                Json.parseToJsonElement(body).jsonObject["description"]?.jsonPrimitive?.content
            }.getOrNull()
            val message = buildString {
                append("HTTP ${status.value}")
                if (!desc.isNullOrBlank()) append(": ").append(desc)
            }
            throw TelegramApiException(status.value, message)
        }
    }

    suspend fun sendMessage(
        chatId: Long,
        text: String,
        replyMarkup: String? = null,
        replyToMessageId: Long? = null,
    ) {
        val response = client.submitForm(
            url = "$baseUrl/sendMessage",
            formParameters = Parameters.build {
                append("chat_id", chatId.toString())
                append("text", text)
                if (replyMarkup != null) append("reply_markup", replyMarkup)
                if (replyToMessageId != null) append("reply_to_message_id", replyToMessageId.toString())
            },
        )
        response.ensureSuccess()
    }

    suspend fun sendPhoto(
        chatId: Long,
        photo: ByteArray,
        caption: String? = null,
        replyMarkup: String? = null,
        replyToMessageId: Long? = null,
    ) {
        val response = client.submitFormWithBinaryData(
            url = "$baseUrl/sendPhoto",
            formData = formData {
                append("chat_id", chatId.toString())
                if (caption != null) append("caption", caption)
                if (replyMarkup != null) append("reply_markup", replyMarkup)
                if (replyToMessageId != null) {
                    append("reply_to_message_id", replyToMessageId.toString())
                }
                append(
                    key = "photo",
                    value = photo,
                    headers = Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"catch.png\"")
                        append(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
                    }
                )
            },
        )
        response.ensureSuccess()
    }

    suspend fun answerPreCheckoutQuery(id: String, ok: Boolean = true) {
        val response = client.submitForm(
            url = "$baseUrl/answerPreCheckoutQuery",
            formParameters = Parameters.build {
                append("pre_checkout_query_id", id)
                append("ok", ok.toString())
            },
        )
        response.ensureSuccess()
    }

    suspend fun answerCallbackQuery(id: String) {
        val response = client.submitForm(
            url = "$baseUrl/answerCallbackQuery",
            formParameters = Parameters.build {
                append("callback_query_id", id)
            },
        )
        response.ensureSuccess()
    }

    suspend fun answerInlineQuery(id: String, results: List<InlineQueryResultArticle>) {
        // omit null fields (e.g. description) while still sending defaults like cache_time
        val payload = Json { encodeDefaults = true; explicitNulls = false }
            .encodeToString(AnswerInlineQueryRequest(id, results))
        val response = client.post("$baseUrl/answerInlineQuery") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        response.ensureSuccess()
    }
}
