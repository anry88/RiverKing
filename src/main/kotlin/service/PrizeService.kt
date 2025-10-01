package service

import java.time.Instant

class PrizeService(
    private val tournaments: TournamentService,
    private val ratingPrizes: RatingPrizeService,
) {
    fun distributePrizes(now: Instant = Instant.now()) {
        tournaments.distributePrizes(now)
        ratingPrizes.distributeDailyPrizes(now)
    }

    fun pendingPrizes(userId: Long): List<UserPrize> {
        val tournamentPrizes = tournaments.pendingPrizes(userId)
        val rating = ratingPrizes.pendingPrizes(userId)
        return (tournamentPrizes + rating)
            .sortedWith(compareByDescending<UserPrize> { it.coins ?: 0 }.thenBy { it.rank })
    }

    fun claimPrize(userId: Long, prizeId: Long, fishing: FishingService): Pair<List<FishingService.LureDTO>, Long?> {
        return when {
            prizeId >= 0 -> tournaments.claimPrize(userId, prizeId, fishing)
            prizeId == RATING_AGGREGATE_PRIZE_ID -> {
                val coins = ratingPrizes.claimAll(userId)
                if (coins > 0) {
                    fishing.addCoins(userId, coins)
                }
                Pair(emptyList(), null)
            }
            else -> {
                ratingPrizes.claimPrize(userId, -prizeId, fishing)
                Pair(emptyList(), null)
            }
        }
    }
}
