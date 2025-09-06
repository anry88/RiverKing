package util

import java.security.SecureRandom
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.exp
import kotlin.random.Random

object Rng {
    private val secure = SecureRandom()
    fun fast(): Random = Random(secure.nextLong())

    fun logNormalKg(mean: Double, variance: Double): Double {
        val mu = ln(mean * mean / sqrt(variance + mean * mean))
        val sigma = sqrt(ln(1 + variance / (mean * mean)))
        val z = nextGaussian()
        val v = exp(mu + sigma * z)
        return maxOf(0.05, (kotlin.math.round(v * 100.0) / 100.0))
    }

    private fun nextGaussian(): Double {
        var u: Double
        var v: Double
        var s: Double
        do {
            u = Random.Default.nextDouble() * 2 - 1
            v = Random.Default.nextDouble() * 2 - 1
            s = u * u + v * v
        } while (s >= 1 || s == 0.0)
        val mul = sqrt(-2.0 * ln(s) / s)
        return u * mul
    }
}
