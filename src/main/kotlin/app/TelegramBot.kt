package app

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

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
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            outputStream.use { it.write(data.toByteArray()) }
            inputStream.buffered().use { it.readBytes() }
            disconnect()
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
}
