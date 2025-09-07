package util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object Metrics {
    private val log = LoggerFactory.getLogger("Metrics")
    private val client = HttpClient(CIO)
    @Volatile private var url: String? = null

    private data class MetricKey(val name: String, val tags: Map<String, String>)

    private val counters = ConcurrentHashMap<MetricKey, Double>()
    private val gauges = ConcurrentHashMap<MetricKey, Double>()

    fun configure(url: String?) {
        this.url = url
    }

    fun counter(name: String, tags: Map<String, String> = emptyMap(), value: Number = 1) {
        val key = MetricKey(name, tags.toSortedMap())
        counters.merge(key, value.toDouble()) { a, b -> a + b }
        push(name, value, tags)
    }

    fun gauge(name: String, value: Number, tags: Map<String, String> = emptyMap()) {
        val key = MetricKey(name, tags.toSortedMap())
        gauges[key] = value.toDouble()
        push(name, value, tags)
    }

    fun dump(): String {
        val sb = StringBuilder()
        counters.forEach { (k, v) ->
            sb.append(formatLine(k.name, k.tags, v))
        }
        gauges.forEach { (k, v) ->
            sb.append(formatLine(k.name, k.tags, v))
        }
        return sb.toString()
    }

    private fun push(name: String, value: Number, tags: Map<String, String>) {
        val endpoint = url ?: return
        val line = formatLine(name, tags, value)
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

    private fun formatLine(name: String, tags: Map<String, String>, value: Number): String {
        val tagsStr = if (tags.isEmpty()) "" else tags.entries.joinToString(",", prefix = "{", postfix = "}") {
            "${it.key}=\"${it.value}\""
        }
        return "$name$tagsStr ${value.toDouble()}\n"
    }
}

