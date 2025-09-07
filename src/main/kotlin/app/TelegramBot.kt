package app

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class TelegramBot(private val token: String) {
    fun sendMessage(chatId: Long, text: String) {
        val url = URL("https://api.telegram.org/bot$token/sendMessage")
        val data = "chat_id=$chatId&text=" + URLEncoder.encode(text, "UTF-8")
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
