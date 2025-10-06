package app

import io.ktor.server.application.*
import kotlinx.coroutines.*
import service.PrizeService
import service.RatingPrizeService
import service.TournamentService
import java.time.*

object Scheduler {
    fun install(app: Application) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val tournaments = TournamentService()
        val ratingPrizes = RatingPrizeService()
        val prizeService = PrizeService(tournaments, ratingPrizes)
        scope.launch {
            prizeService.distributePrizes()
            while (isActive) {
                delay(Duration.ofMinutes(5).toMillis())
                prizeService.distributePrizes()
                // TODO: daily weather/bonuses if needed later
            }
        }
    }
}
