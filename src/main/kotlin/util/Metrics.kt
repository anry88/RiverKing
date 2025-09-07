package util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object Metrics {
    private val log = LoggerFactory.getLogger("Metrics")
    private val client = HttpClient(CIO)
    @Volatile private var url: String? = null

    fun configure(url: String?) {
        this.url = url
    }

    fun counter(name: String, tags: Map<String, String> = emptyMap(), value: Number = 1) {
        send(name, value, tags)
    }

    fun gauge(name: String, value: Number, tags: Map<String, String> = emptyMap()) {
        send(name, value, tags)
    }

    private fun send(name: String, value: Number, tags: Map<String, String>) {
        val endpoint = url ?: return
        val tagsStr = if (tags.isEmpty()) "" else tags.entries.joinToString(",", prefix = "{", postfix = "}") {
            "${it.key}="\"${it.value}\""
        }
        val line = "$name$tagsStr $value\n"
        GlobalScope.launch {
            try {
                client.post(endpoint) {
                    setBody(line)
                }
            } catch (e: Exception) {
                log.warn("failed to push metrics", e)
            }
        }
    }
}
