package app

import kotlinx.serialization.Serializable

@Serializable
data class InlineKeyboardButton(val text: String, val callback_data: String)

@Serializable
data class InlineKeyboardMarkup(val inline_keyboard: List<List<InlineKeyboardButton>>)

