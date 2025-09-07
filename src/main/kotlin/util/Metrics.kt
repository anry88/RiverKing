package util

import java.util.concurrent.ConcurrentHashMap

object Metrics {
    private data class MetricKey(val name: String, val tags: Map<String, String>)

    private val counters = ConcurrentHashMap<MetricKey, Double>()
    private val gauges = ConcurrentHashMap<MetricKey, Double>()

    fun counter(name: String, tags: Map<String, String> = emptyMap(), value: Number = 1) {
        val key = MetricKey(name, tags.toSortedMap())
        counters.merge(key, value.toDouble()) { a, b -> a + b }
    }

    fun gauge(name: String, value: Number, tags: Map<String, String> = emptyMap()) {
        val key = MetricKey(name, tags.toSortedMap())
        gauges[key] = value.toDouble()
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

    private fun formatLine(name: String, tags: Map<String, String>, value: Number): String {
        val tagsStr = if (tags.isEmpty()) "" else tags.entries.joinToString(",", prefix = "{", postfix = "}") {
            "${it.key}=\"${it.value}\""
        }
        return "$name$tagsStr ${value.toDouble()}\n"
    }
}

