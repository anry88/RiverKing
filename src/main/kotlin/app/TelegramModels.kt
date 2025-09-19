package app

import kotlinx.serialization.Serializable

@Serializable
data class InlineKeyboardButton(val text: String, val callback_data: String)

@Serializable
data class InlineKeyboardMarkup(val inline_keyboard: List<List<InlineKeyboardButton>>)

@Serializable
data class InputTextMessageContent(
    @kotlinx.serialization.SerialName("message_text") val messageText: String
)

@Serializable
data class InlineQueryResultArticle(
    val type: String = "article",
    val id: String,
    val title: String,
    @kotlinx.serialization.SerialName("input_message_content") val inputMessageContent: InputTextMessageContent,
    val description: String? = null,
    @kotlinx.serialization.SerialName("thumb_url") val thumbUrl: String? = null,
    @kotlinx.serialization.SerialName("thumb_width") val thumbWidth: Int? = null,
    @kotlinx.serialization.SerialName("thumb_height") val thumbHeight: Int? = null,
)

