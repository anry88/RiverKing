package app

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
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

class TelegramBot(private val token: String) {
    fun sendMessage(chatId: Long, text: String, replyMarkup: String? = null) {
        val url = URL("https://api.telegram.org/bot$token/sendMessage")
        val params = mutableListOf(
            "chat_id=$chatId",
            "text=" + URLEncoder.encode(text, "UTF-8")
        )
        if (replyMarkup != null) {
            params += "reply_markup=" + URLEncoder.encode(replyMarkup, "UTF-8")
        }
        val data = params.joinToString("&")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        try {
            connection.outputStream.use { it.write(data.toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }
            if (code !in 200..299) {
                val desc = try {
                    Json.parseToJsonElement(body ?: "").jsonObject["description"]?.jsonPrimitive?.content
                } catch (_: Exception) {
                    null
                }
                val message = buildString {
                    append("HTTP $code")
                    if (!desc.isNullOrBlank()) append(": ").append(desc)
                }
                throw TelegramApiException(code, message)
            }
        } finally {
            connection.disconnect()
        }
    }

    fun answerPreCheckoutQuery(id: String, ok: Boolean = true) {
        val url = URL("https://api.telegram.org/bot$token/answerPreCheckoutQuery")
        val data = "pre_checkout_query_id=" + URLEncoder.encode(id, "UTF-8") + "&ok=$ok"
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            outputStream.use { it.write(data.toByteArray()) }
            inputStream.buffered().use { it.readBytes() }
            disconnect()
        }
    }

    fun answerCallbackQuery(id: String) {
        val url = URL("https://api.telegram.org/bot$token/answerCallbackQuery")
        val data = "callback_query_id=" + URLEncoder.encode(id, "UTF-8")
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            outputStream.use { it.write(data.toByteArray()) }
            inputStream.buffered().use { it.readBytes() }
            disconnect()
        }
    }

    fun answerInlineQuery(id: String, results: List<InlineQueryResultArticle>) {
        val url = URL("https://api.telegram.org/bot$token/answerInlineQuery")
        // omit null fields (e.g. description) while still sending defaults like cache_time
        val payload = Json { encodeDefaults = true; explicitNulls = false }
            .encodeToString(AnswerInlineQueryRequest(id, results))
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(payload.toByteArray()) }
            val code = responseCode
            val stream = if (code in 200..299) inputStream else errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }
            if (code !in 200..299) {
                val desc = try {
                    Json.parseToJsonElement(body ?: "").jsonObject["description"]?.jsonPrimitive?.content
                } catch (_: Exception) {
                    null
                }
                val message = buildString {
                    append("HTTP $code")
                    if (!desc.isNullOrBlank()) append(": ").append(desc)
                }
                throw TelegramApiException(code, message)
            }
            disconnect()
        }
    }
}
