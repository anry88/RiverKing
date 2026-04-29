package service

import app.TelegramBot
import app.TelegramApiException
import app.InlineKeyboardButton
import app.InlineKeyboardMarkup
import db.Users
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class NotificationService(private val bot: TelegramBot) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    suspend fun sendNotification(
        userId: Long,
        tgId: Long,
        text: String,
        buttonText: String? = null,
        buttonCallback: String? = null,
        markup: String? = null
    ): Boolean {
        val finalMarkup = markup ?: if (buttonText != null && buttonCallback != null) {
            Json.encodeToString(InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton(buttonText, buttonCallback)))))
        } else null

        return try {
            bot.sendMessage(tgId, text, finalMarkup)
            true
        } catch (e: TelegramApiException) {
            if (e.code == 403 || e.message?.contains("forbidden", ignoreCase = true) == true) {
                log.info("User $userId (tg $tgId) blocked the bot. Disabling notifications.")
                transaction {
                    Users.update({ Users.id eq userId }) {
                        it[canReceiveNotifications] = false
                    }
                }
            } else {
                log.error("Failed to send notification to user $userId (tg $tgId): ${e.message}")
            }
            false
        } catch (e: Exception) {
            log.error("Unexpected error sending notification to $userId: ${e.message}", e)
            false
        }
    }
}
