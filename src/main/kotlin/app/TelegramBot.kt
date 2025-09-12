package app

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class TelegramApiException(val code: Int, message: String) : IOException(message)

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
            stream?.buffered()?.use { it.readBytes() }
            if (code !in 200..299) {
                throw TelegramApiException(code, "HTTP $code")
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
}
