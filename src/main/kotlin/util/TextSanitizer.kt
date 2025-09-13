package util

private val BIDI_REGEX = Regex("[\u200E\u200F\u202A-\u202E\u2066-\u2069]")

fun sanitizeName(name: String, maxLen: Int = 30): String =
    BIDI_REGEX.replace(name, "").take(maxLen)
