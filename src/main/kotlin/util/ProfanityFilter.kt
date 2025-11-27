package util

import java.io.BufferedReader

object ProfanityFilter {
    private val patterns: List<Regex> by lazy {
        val stream = this::class.java.classLoader.getResourceAsStream("profanity.txt")
        if (stream == null) {
            emptyList()
        } else {
            stream.bufferedReader().use(BufferedReader::readLines)
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { Regex(Regex.escape(it), RegexOption.IGNORE_CASE) }
        }
    }

    fun mask(text: String): String {
        var result = text
        for (pattern in patterns) {
            result = pattern.replace(result) { matchResult -> "*".repeat(matchResult.value.length) }
        }
        return result
    }
}
