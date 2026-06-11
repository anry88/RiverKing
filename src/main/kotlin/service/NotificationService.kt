package service

import app.TelegramBot
import app.TelegramApiException
import app.InlineKeyboardButton
import app.InlineKeyboardMarkup
import db.Users
import kotlinx.coroutines.delay
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

        return sendWithRetry(userId, tgId, text, finalMarkup)
    }

    private suspend fun sendWithRetry(userId: Long, tgId: Long, text: String, markup: String?): Boolean {
        return try {
            bot.sendMessage(tgId, text, markup)
            true
        } catch (e: TelegramApiException) {
            handleTelegramFailure(userId, tgId, text, markup, e)
        } catch (e: Exception) {
            log.error("Unexpected error sending notification to $userId: ${e.message}", e)
            false
        }
    }

    private suspend fun handleTelegramFailure(
        userId: Long,
        tgId: Long,
        text: String,
        markup: String?,
        error: TelegramApiException,
    ): Boolean {
        if (isBlockedByUser(error)) {
            disableNotifications(userId, tgId)
            return false
        }

        if (error.code == 429) {
            val retryDelayMs = ((error.retryAfterSeconds ?: 1) + 1).coerceAtMost(60).toLong() * 1_000L
            log.warn(
                "Telegram rate limited notification userId={} tgId={} retryAfterSeconds={} delayMs={}",
                userId,
                tgId,
                error.retryAfterSeconds,
                retryDelayMs
            )
            delay(retryDelayMs)
            return try {
                bot.sendMessage(tgId, text, markup)
                true
            } catch (retryError: TelegramApiException) {
                if (isBlockedByUser(retryError)) {
                    disableNotifications(userId, tgId)
                } else {
                    log.error(
                        "Failed to retry notification to user {} (tg {}): {}",
                        userId,
                        tgId,
                        retryError.message
                    )
                }
                false
            } catch (retryError: Exception) {
                log.error("Unexpected retry error sending notification to $userId: ${retryError.message}", retryError)
                false
            }
        }

        log.error("Failed to send notification to user $userId (tg $tgId): ${error.message}")
        return false
    }

    private fun isBlockedByUser(error: TelegramApiException): Boolean =
        error.code == 403 || error.message?.contains("forbidden", ignoreCase = true) == true

    private fun disableNotifications(userId: Long, tgId: Long) {
        log.info("User $userId (tg $tgId) blocked the bot. Disabling notifications.")
        transaction {
            Users.update({ Users.id eq userId }) {
                it[canReceiveNotifications] = false
            }
        }
    }
}
