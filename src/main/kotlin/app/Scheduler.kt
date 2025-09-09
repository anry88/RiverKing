package app

import io.ktor.server.application.*
import kotlinx.coroutines.*
import service.FishingService
import service.TournamentService
import java.time.*

object Scheduler {
    fun install(app: Application) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val tournaments = TournamentService()
        val fishing = FishingService()
        scope.launch {
            while (isActive) {
                val tz = ZoneId.of("Europe/Belgrade")
                val now = ZonedDateTime.now(tz)
                val next = now.with(LocalTime.of(0, 5)).let { if (it.isBefore(now)) it.plusDays(1) else it }
                delay(Duration.between(now, next).toMillis())
                tournaments.distributePrizes()
                // TODO: daily weather/bonuses if needed later
            }
        }
    }
}
