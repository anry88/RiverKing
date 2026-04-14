package service

import java.time.Instant

class PrizeService(
    private val tournaments: TournamentService,
    private val ratingPrizes: RatingPrizeService,
    private val clubs: ClubService,
) {
    fun distributePrizes(now: Instant = Instant.now()) {
        tournaments.distributePrizes(now)
        ratingPrizes.distributeDailyPrizes(now)
        clubs.distributeWeeklyRewards(now)
    }

    fun pendingPrizes(userId: Long, language: String = "ru"): List<UserPrize> {
        val tournamentPrizes = tournaments.pendingPrizes(userId, language)
        val rating = ratingPrizes.pendingPrizes(userId)
        val clubRewards = clubs.pendingRewards(userId)
        return (tournamentPrizes + rating + clubRewards)
            .sortedWith(compareByDescending<UserPrize> { it.coins ?: 0 }.thenBy { it.rank })
    }

    fun claimPrize(userId: Long, prizeId: Long, fishing: FishingService): Pair<List<FishingService.LureDTO>, Long?> {
        return when {
            prizeId == RATING_AGGREGATE_PRIZE_ID -> {
                val coins = ratingPrizes.claimAll(userId)
                if (coins > 0) {
                    fishing.addCoins(userId, coins)
                    clubs.addContribution(userId, coins)
                }
                Pair(emptyList(), null)
            }
            prizeId < 0 -> {
                val coins = ratingPrizes.claimPrize(userId, -prizeId)
                if (coins > 0) {
                    fishing.addCoins(userId, coins)
                    clubs.addContribution(userId, coins)
                }
                Pair(emptyList(), null)
            }
            clubs.hasPendingReward(userId, prizeId) -> {
                val coins = clubs.claimReward(userId, prizeId)
                if (coins > 0) fishing.addCoins(userId, coins)
                Pair(emptyList(), null)
            }
            else -> tournaments.claimPrize(userId, prizeId, fishing)
        }
    }
}
