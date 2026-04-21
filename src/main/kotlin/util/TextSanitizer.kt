package util

private val BIDI_REGEX = Regex("[\u200E\u200F\u202A-\u202E\u2066-\u2069]")

fun sanitizeName(name: String, maxLen: Int = 30): String {
    val cleaned = BIDI_REGEX.replace(name, "")
    val filtered = ProfanityFilter.mask(cleaned)
    return filtered.take(maxLen)
}

fun sanitizeChatMessage(text: String, maxLen: Int = 500): String {
    val cleaned = BIDI_REGEX.replace(text, "")
        .replace(Regex("\\s+"), " ")
        .trim()
    val filtered = ProfanityFilter.mask(cleaned)
    return filtered.take(maxLen).trim()
}
