package service

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

@Serializable
data class CastZonePointDTO(
    val x: Double,
    val y: Double,
)

@Serializable
data class CastZoneDTO(
    val points: List<CastZonePointDTO>,
)

object CastZoneCodec {
    private const val MIN_POINTS = 3
    private const val MAX_POINTS = 64
    private const val MIN_AREA = 0.0004

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(zone: CastZoneDTO?): String? = zone?.let {
        validate(it)
        json.encodeToString(it)
    }

    fun decode(raw: String?): CastZoneDTO? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString<CastZoneDTO>(raw).also(::validate)
        }.getOrNull()
    }

    fun validateNullable(zone: CastZoneDTO?) {
        if (zone != null) validate(zone)
    }

    fun validate(zone: CastZoneDTO) {
        val points = zone.points
        if (points.size !in MIN_POINTS..MAX_POINTS) {
            throw IllegalArgumentException("invalid_cast_zone")
        }
        if (points.any { !it.x.isFinite() || !it.y.isFinite() || it.x !in 0.0..1.0 || it.y !in 0.0..1.0 }) {
            throw IllegalArgumentException("invalid_cast_zone")
        }
        if (polygonArea(points) < MIN_AREA) {
            throw IllegalArgumentException("invalid_cast_zone")
        }
    }

    private fun polygonArea(points: List<CastZonePointDTO>): Double {
        var sum = 0.0
        points.forEachIndexed { index, point ->
            val next = points[(index + 1) % points.size]
            sum += point.x * next.y - next.x * point.y
        }
        return abs(sum) / 2.0
    }
}
