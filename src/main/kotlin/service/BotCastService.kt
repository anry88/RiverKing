package service

import db.DbExecution
import kotlinx.coroutines.delay
import util.Metrics
import kotlin.random.Random

/** Server-side equivalent of Telegram /cast; callers do not perform hook/tap/catch actions. */
class BotCastService(
    private val fishing: FishingService = FishingService(),
    private val waitSeconds: () -> Int = { Random.nextInt(5, 31) },
    private val awaitBite: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun cast(userId: Long): FishingService.CastResultDTO {
        DbExecution.blocking { fishing.startCast(userId) }
        val wait = waitSeconds().coerceIn(5, 30)
        awaitBite(wait * 1_000L)
        val result = DbExecution.blocking {
            val escape = fishing.locationEscapeChance(userId)
            val extraEscape = if (escape.rodBonusMultiplier < 1.0) 0.05 else 0.10
            val hook = fishing.hook(userId, wait, 0.0, extraEscape)
            Metrics.counter("hook_total", mapOf("result" to if (hook.success) "caught" else "escaped"))
            if (hook.success) fishing.cast(userId, wait, 0.0, true)
            else FishingService.CastResultDTO(caught = false)
        }
        Metrics.counter("cast_total", mapOf("caught" to result.caught.toString()))
        Metrics.counter("bot_cast_total", mapOf("caught" to result.caught.toString()))
        result.catch?.let { catch ->
            val tags = mapOf("fish" to catch.fish, "location" to catch.location, "rarity" to catch.rarity)
            Metrics.counter("fish_caught_total", tags)
            Metrics.gauge("catch_weight_kg", catch.weight, tags)
        }
        return result
    }
}
