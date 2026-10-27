package stub

import db.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import service.*
import kotlin.random.Random

/** Uses the same services as the bot without issuing Telegram requests or simulating player taps. */
class GameCycle(
    private val fishing: FishingService = FishingService(),
    private val clubs: ClubService = ClubService(),
    private val waitSeconds: () -> Int = { Random.nextInt(5, 31) },
    private val awaitBite: suspend (Long) -> Unit = { delay(it) },
) {
    private val log = LoggerFactory.getLogger(GameCycle::class.java)
    private val prizes = PrizeService(TournamentService(), RatingPrizeService(), clubs, SpecialEventService())

    enum class Result { CAUGHT, ESCAPED, BUSY, NO_BAIT }

    suspend fun run(userId: Long): Result {
        DbExecution.blocking {
            fishing.touchAuthenticatedUserById(userId)
            joinClubIfNeeded(userId)
            claimRewards(userId)
            fishing.resetStaleCasting(userId)
        }
        if (DbExecution.transaction { Users.select { Users.id eq userId }.single()[Users.isCasting] }) {
            return Result.BUSY
        }
        val ready = DbExecution.blocking {
            moveLocation(userId)
            prepareBait(userId)
        }
        if (!ready) return Result.NO_BAIT

        DbExecution.blocking { fishing.startCast(userId) }
        try {
            val wait = waitSeconds().coerceIn(5, 30)
            awaitBite(wait * 1_000L)
            return DbExecution.blocking {
                // These are the /cast parameters in BotRoutes.performCastSequence.
                val escape = fishing.locationEscapeChance(userId)
                val extraEscape = if (escape.rodBonusMultiplier < 1.0) 0.05 else 0.10
                if (!fishing.hook(userId, wait, 0.0, extraEscape).success) Result.ESCAPED
                else if (fishing.cast(userId, wait, 0.0, true).caught) Result.CAUGHT else Result.ESCAPED
            }
        } catch (e: CancellationException) {
            // On restart, only this roster's stale casts are restored by resetStaleCasting.
            throw e
        } catch (e: Exception) {
            DbExecution.blocking { fishing.resetCasting(userId) }
            throw e
        }
    }

    fun joinClubIfNeeded(userId: Long) {
        if (transaction { ClubMembers.select { ClubMembers.userId eq userId }.any() }) return
        for (club in clubs.searchClubs(userId)) {
            try {
                clubs.joinClub(userId, club.id)
                log.info("Joined club userId={} clubId={}", userId, club.id)
                return
            } catch (e: ClubService.ClubException) {
                if (e.code == "already_in_club") return
                // A club may fill up or close recruitment between search and join.
            }
        }
    }

    fun claimRewards(userId: Long) {
        val lang = fishing.userLanguage(userId)
        // Claim flags and reward delivery share one transaction, so a failed grant can be retried.
        for (prize in prizes.pendingPrizes(userId, lang)) {
            transaction { prizes.claimPrize(userId, prize.id, fishing) }
        }
        transaction { ReferralService.claimAllRewards(userId, fishing) }
        if (fishing.canClaimDaily(userId)) fishing.giveDailyBaits(userId)
        QuestService.list(userId, lang) // Quest rewards are delivered automatically by FishingService.cast.
        for (achievement in AchievementService.list(userId, lang).filter { it.claimable }) {
            transaction {
                val reward = AchievementService.claim(userId, achievement.code) ?: return@transaction
                reward.rewards.forEach { prize ->
                    when {
                        prize.pack.equals(COIN_PRIZE_ID, ignoreCase = true) ->
                            fishing.addCoins(userId, prize.coins ?: prize.qty)
                        prize.pack.isNotBlank() -> repeat(prize.qty.coerceAtLeast(1)) {
                            fishing.buyPackage(userId, prize.pack)
                        }
                    }
                }
            }
        }
    }

    fun moveLocation(userId: Long): Long {
        val locations = fishing.locations(userId, fishing.userLanguage(userId))
        val current = transaction { Users.select { Users.id eq userId }.single()[Users.currentLocationId]?.value }
        val first = locations.first { it.unlocked }
        val index = locations.indexOfFirst { it.id == current }
        val next = if (index >= 0) locations.getOrNull(index + 1)?.takeIf { it.unlocked } else null
        val target = next ?: first
        try {
            fishing.setLocation(userId, target.id)
            return target.id
        } catch (e: SpecialEventService.SpecialEventException) {
            // The event may have ended, or membership changed since the location list was read.
            val fallback = fishing.locations(userId).first { !it.isEvent && it.unlocked }
            fishing.setLocation(userId, fallback.id)
            return fallback.id
        } catch (e: IllegalArgumentException) {
            if (e.message != "event_inactive") throw e
            val fallback = fishing.locations(userId).first { !it.isEvent && it.unlocked }
            fishing.setLocation(userId, fallback.id)
            return fallback.id
        }
    }

    /** Pick owned compatible bait; otherwise buy the cheapest compatible coin pack if affordable. */
    fun prepareBait(userId: Long): Boolean {
        val compatibleNames = transaction {
            val locationId = Users.select { Users.id eq userId }.single()[Users.currentLocationId]!!.value
            val eventId = Locations.select { Locations.id eq locationId }.single()[Locations.specialEventId]
            if (eventId != null) {
                Lures.selectAll().map { it[Lures.name] }.toSet() // Events intentionally allow mixed lures.
            } else {
                val fishTypes = (LocationFishWeights innerJoin Fish)
                    .select { LocationFishWeights.locationId eq locationId }
                    .map { it[Fish.water] to it[Fish.predator] }.toSet()
                Lures.selectAll().filter { (it[Lures.water] to it[Lures.predator]) in fishTypes }
                    .map { it[Lures.name] }.toSet()
            }
        }
        fun selectOwned(): Boolean {
            val owned = fishing.listLures(userId).firstOrNull { it.qty > 0 && it.name in compatibleNames }
                ?: return false
            fishing.setLure(userId, owned.id)
            return true
        }
        if (selectOwned()) return true
        val balance = transaction { Users.select { Users.id eq userId }.single()[Users.coins] }
        val pack = fishing.listShop("ru").flatMap { it.packs }
            .mapNotNull { fishing.findPack(it.id) }
            .filter { it.coinPrice != null && it.coinPrice!!.toLong() <= balance }
            .filter { it.items.any { (name, qty) -> qty > 0 && name in compatibleNames } }
            .minWithOrNull(compareBy<FishingService.ShopPackage> { it.coinPrice }.thenBy { it.id })
            ?: return false
        fishing.buyPackageWithCoins(userId, pack.id)
        log.info("Bought bait userId={} pack={} coins={}", userId, pack.id, pack.coinPrice)
        return selectOwned()
    }
}
