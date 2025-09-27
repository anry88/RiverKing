package util

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min

object CoinCalculator {
    private const val BASE = 1.5

    private val rarityCoefficients = mapOf(
        "common" to 1.0,
        "uncommon" to 2.0,
        "rare" to 4.0,
        "epic" to 7.0,
        "mythic" to 9.5,
        "legendary" to 12.0,
    )

    fun baseCoins(weightKg: Double, rarity: String, locationTier: Int, water: String): Int {
        val rarityCoef = rarityCoefficients[rarity] ?: 1.0
        val sizeFactor = min(ln(1.0 + weightKg) * 1.8, 8.0)
        val locationFactor = 1.0 + 0.05 * locationTier.coerceAtLeast(0)
        val waterFactor = if (water == "salt") 1.15 else 1.0
        val raw = BASE * rarityCoef * sizeFactor * locationFactor * waterFactor
        return ceil(raw).toInt().coerceAtLeast(0)
    }

    fun applyDailySoftCap(baseCoins: Int, earnedToday: Int): Int {
        val effectiveBase = baseCoins.coerceAtLeast(1)
        val safeEarned = earnedToday.coerceAtLeast(0)
        val multiplier = when {
            safeEarned >= 800 -> 0.25
            safeEarned >= 400 -> 0.5
            else -> 1.0
        }
        val awarded = effectiveBase * multiplier
        val rounded = floor(awarded + 1e-9).toInt()
        return maxOf(1, rounded)
    }

    fun computeCoins(
        weightKg: Double,
        rarity: String,
        locationTier: Int,
        water: String,
        earnedToday: Int,
    ): Int {
        val base = baseCoins(weightKg, rarity, locationTier, water)
        return applyDailySoftCap(base, earnedToday)
    }
}
