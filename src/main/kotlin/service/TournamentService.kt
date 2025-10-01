package service

import db.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import util.sanitizeName
import java.time.Instant

const val COIN_PRIZE_ID = "coins"

data class Tournament(
    val id: Long,
    val nameRu: String,
    val nameEn: String,
    val startTime: Instant,
    val endTime: Instant,
    val fish: String?,
    val location: String?,
    val metric: String,
    val prizePlaces: Int,
    val prizesJson: String,
)

@Serializable
data class PrizeSpec(
    val pack: String = "",
    val qty: Int = 1,
    val coins: Int? = null,
)

data class UserPrize(
    val id: Long,
    val packageId: String,
    val qty: Int,
    val rank: Int,
    val coins: Int? = null,
)

class TournamentService {
    fun createTournament(
        nameRu: String,
        nameEn: String,
        start: Instant,
        end: Instant,
        fish: String?,
        location: String?,
        metric: String,
        prizePlaces: Int,
        prizes: String,
    ): Long = transaction {
        Tournaments.insert {
            it[Tournaments.nameRu] = nameRu
            it[Tournaments.nameEn] = nameEn
            it[Tournaments.startTime] = start
            it[Tournaments.endTime] = end
            it[Tournaments.fish] = fish
            it[Tournaments.location] = location
            it[Tournaments.metric] = metric
            it[Tournaments.prizePlaces] = prizePlaces
            it[Tournaments.prizesJson] = prizes
        } get Tournaments.id
    }.value

    fun listTournaments(): List<Tournament> = transaction {
        Tournaments.selectAll().map { row ->
            Tournament(
                id = row[Tournaments.id].value,
                nameRu = row[Tournaments.nameRu],
                nameEn = row[Tournaments.nameEn],
                startTime = row[Tournaments.startTime],
                endTime = row[Tournaments.endTime],
                fish = row[Tournaments.fish],
                location = row[Tournaments.location],
                metric = row[Tournaments.metric],
                prizePlaces = row[Tournaments.prizePlaces],
                prizesJson = row[Tournaments.prizesJson],
            )
        }
    }

    fun getTournament(id: Long): Tournament? = transaction {
        Tournaments.select { Tournaments.id eq id }.singleOrNull()?.let { row ->
            Tournament(
                id = row[Tournaments.id].value,
                nameRu = row[Tournaments.nameRu],
                nameEn = row[Tournaments.nameEn],
                startTime = row[Tournaments.startTime],
                endTime = row[Tournaments.endTime],
                fish = row[Tournaments.fish],
                location = row[Tournaments.location],
                metric = row[Tournaments.metric],
                prizePlaces = row[Tournaments.prizePlaces],
                prizesJson = row[Tournaments.prizesJson],
            )
        }
    }

    fun updateTournament(
        id: Long,
        nameRu: String,
        nameEn: String,
        start: Instant,
        end: Instant,
        fish: String?,
        location: String?,
        metric: String,
        prizePlaces: Int,
        prizes: String,
    ) = transaction {
        Tournaments.update({ Tournaments.id eq id }) {
            it[Tournaments.nameRu] = nameRu
            it[Tournaments.nameEn] = nameEn
            it[Tournaments.startTime] = start
            it[Tournaments.endTime] = end
            it[Tournaments.fish] = fish
            it[Tournaments.location] = location
            it[Tournaments.metric] = metric
            it[Tournaments.prizePlaces] = prizePlaces
            it[Tournaments.prizesJson] = prizes
        }
    }

    fun deleteTournament(id: Long) = transaction {
        Tournaments.deleteWhere { Tournaments.id eq id }
    }

    fun currentTournament(now: Instant = Instant.now()): Tournament? = transaction {
        Tournaments.select {
            (Tournaments.startTime lessEq now) and (Tournaments.endTime greaterEq now)
        }.singleOrNull()?.let { row ->
            Tournament(
                id = row[Tournaments.id].value,
                nameRu = row[Tournaments.nameRu],
                nameEn = row[Tournaments.nameEn],
                startTime = row[Tournaments.startTime],
                endTime = row[Tournaments.endTime],
                fish = row[Tournaments.fish],
                location = row[Tournaments.location],
                metric = row[Tournaments.metric],
                prizePlaces = row[Tournaments.prizePlaces],
                prizesJson = row[Tournaments.prizesJson],
            )
        }
    }

    fun upcomingTournaments(now: Instant = Instant.now()): List<Tournament> = transaction {
        Tournaments.select { Tournaments.startTime greater now }.map { row ->
            Tournament(
                id = row[Tournaments.id].value,
                nameRu = row[Tournaments.nameRu],
                nameEn = row[Tournaments.nameEn],
                startTime = row[Tournaments.startTime],
                endTime = row[Tournaments.endTime],
                fish = row[Tournaments.fish],
                location = row[Tournaments.location],
                metric = row[Tournaments.metric],
                prizePlaces = row[Tournaments.prizePlaces],
                prizesJson = row[Tournaments.prizesJson],
            )
        }
    }

    fun pastTournaments(limit: Int = 10, now: Instant = Instant.now()): List<Tournament> = transaction {
        Tournaments.select { Tournaments.endTime less now }
            .orderBy(Tournaments.endTime, SortOrder.DESC)
            .limit(limit)
            .map { row ->
                Tournament(
                    id = row[Tournaments.id].value,
                    nameRu = row[Tournaments.nameRu],
                    nameEn = row[Tournaments.nameEn],
                    startTime = row[Tournaments.startTime],
                    endTime = row[Tournaments.endTime],
                    fish = row[Tournaments.fish],
                    location = row[Tournaments.location],
                    metric = row[Tournaments.metric],
                    prizePlaces = row[Tournaments.prizePlaces],
                    prizesJson = row[Tournaments.prizesJson],
                )
            }
    }

    data class LeaderboardEntry(
        val userId: Long,
        val user: String?,
        val value: Double,
        val rank: Int,
        val catchId: Long? = null,
        val fish: String? = null,
        val fishId: Long? = null,
        val location: String? = null,
        val at: Instant? = null,
    )

    private fun rowUser(row: ResultRow): String? {
        val fn = row.getOrNull(Users.firstName)
        val ln = row.getOrNull(Users.lastName)
        val un = row.getOrNull(Users.username)
        val nn = row.getOrNull(Users.nickname)
        val raw = when {
            !nn.isNullOrBlank() -> nn
            !fn.isNullOrBlank() || !ln.isNullOrBlank() -> listOfNotNull(fn, ln).joinToString(" ").trim()
            !un.isNullOrBlank() -> un
            else -> null
        }
        return raw?.let { sanitizeName(it) }
    }

    fun leaderboard(t: Tournament, userId: Long, limit: Int = 10): Pair<List<LeaderboardEntry>, LeaderboardEntry?> = transaction {
        var cond: Op<Boolean> = (Catches.createdAt greaterEq t.startTime) and (Catches.createdAt lessEq t.endTime)
        if (t.fish != null) {
            val rarities = setOf("common", "uncommon", "rare", "epic", "mythic", "legendary")
            cond = cond and if (t.fish in rarities) (Fish.rarity eq t.fish) else (Fish.name eq t.fish)
        }
        if (t.location != null) cond = cond and (Locations.name eq t.location)
        data class CatchRow(
            val userId: Long,
            val user: String?,
            val weight: Double,
            val catchId: Long,
            val fishId: Long,
            val fish: String,
            val location: String,
            val at: Instant,
        )
        val rows = ((Catches leftJoin Users) innerJoin Fish)
            .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
            .select { cond }
            .orderBy(Catches.createdAt, SortOrder.ASC)
            .map { row ->
                CatchRow(
                    row[Catches.userId].value,
                    rowUser(row),
                    row[Catches.weight],
                    row[Catches.id].value,
                    row[Fish.id].value,
                    row[Fish.name],
                    row[Locations.name],
                    row[Catches.createdAt],
                )
            }
        val grouped = rows.groupBy { it.userId }
            .map { (uid, list) ->
                val name = list.first().user
                val chosen = when (t.metric.lowercase()) {
                    "smallest" -> list.minByOrNull { it.weight }
                    "count" -> list.maxByOrNull { it.at }
                    "total_weight" -> list.maxByOrNull { it.weight }
                    else -> list.maxByOrNull { it.weight }
                }
                val value = when (t.metric.lowercase()) {
                    "smallest" -> chosen?.weight ?: 0.0
                    "count" -> list.size.toDouble()
                    "total_weight" -> list.sumOf { it.weight }
                    else -> chosen?.weight ?: 0.0
                }
                val hideFish = t.metric.lowercase() == "count" && t.fish == null
                LeaderboardEntry(
                    uid,
                    name,
                    value,
                    0,
                    catchId = if (hideFish) null else chosen?.catchId,
                    fish = if (hideFish) null else chosen?.fish,
                    fishId = if (hideFish) null else chosen?.fishId,
                    location = if (hideFish) null else chosen?.location,
                    at = if (hideFish) null else chosen?.at
                )
            }
        val sorted = when (t.metric.lowercase()) {
            "smallest" -> grouped.sortedBy { it.value }
            "count" -> grouped.sortedByDescending { it.value }
            "total_weight" -> grouped.sortedByDescending { it.value }
            else -> grouped.sortedByDescending { it.value }
        }
        val ranked = sorted.mapIndexed { index, e -> e.copy(rank = index + 1) }
        val mine = ranked.find { it.userId == userId }
        val top = ranked.take(limit)
        Pair(top, mine)
    }

    fun pendingPrizes(userId: Long): List<UserPrize> {
        data class PrizeRow(val id: Long, val packageId: String, val qty: Int, val tournamentId: Long)
        val rows = transaction {
            UserPrizes.select { (UserPrizes.userId eq userId) and (UserPrizes.claimed eq false) }
                .map {
                    PrizeRow(
                        it[UserPrizes.id].value,
                        it[UserPrizes.packageId],
                        it[UserPrizes.qty],
                        it[UserPrizes.tournamentId].value
                    )
                }
        }
        return rows.map { row ->
            val t = getTournament(row.tournamentId)
            val rank = t?.let { leaderboard(it, userId).second?.rank } ?: 0
            val coins = if (row.packageId == COIN_PRIZE_ID) row.qty else null
            UserPrize(row.id, row.packageId, row.qty, rank, coins)
        }
    }

    fun claimPrize(userId: Long, prizeId: Long, fishing: FishingService): Pair<List<FishingService.LureDTO>, Long?> {
        val (pack, qty) = transaction {
            val row = UserPrizes.select { (UserPrizes.id eq prizeId) and (UserPrizes.userId eq userId) and (UserPrizes.claimed eq false) }
                .singleOrNull() ?: error("not found")
            UserPrizes.update({ UserPrizes.id eq prizeId }) { it[claimed] = true }
            row[UserPrizes.packageId] to row[UserPrizes.qty]
        }
        if (pack == COIN_PRIZE_ID) {
            if (qty > 0) fishing.addCoins(userId, qty)
            return Pair(emptyList(), null)
        }
        var res: Pair<List<FishingService.LureDTO>, Long?>? = null
        repeat(qty) { res = fishing.buyPackage(userId, pack) }
        return res ?: Pair(emptyList(), null)
    }

    fun distributePrizes(now: Instant = Instant.now()) {
        val ended = transaction {
            Tournaments.select { Tournaments.endTime lessEq now }.mapNotNull { row ->
                val id = row[Tournaments.id].value
                val awarded = !UserPrizes.select { UserPrizes.tournamentId eq id }.empty()
                if (awarded) null else Tournament(
                    id = id,
                    nameRu = row[Tournaments.nameRu],
                    nameEn = row[Tournaments.nameEn],
                    startTime = row[Tournaments.startTime],
                    endTime = row[Tournaments.endTime],
                    fish = row[Tournaments.fish],
                    location = row[Tournaments.location],
                    metric = row[Tournaments.metric],
                    prizePlaces = row[Tournaments.prizePlaces],
                    prizesJson = row[Tournaments.prizesJson],
                )
            }
        }
        for (t in ended) {
            val (top, _) = leaderboard(t, 0L, t.prizePlaces)
            val prizes = try { Json.decodeFromString<List<PrizeSpec>>(t.prizesJson) } catch (_: Exception) { emptyList() }
            for ((idx, lb) in top.withIndex()) {
                val prize = prizes.getOrNull(idx) ?: continue
                val packageId = when {
                    prize.pack.isNotBlank() -> prize.pack
                    prize.coins != null && prize.coins > 0 -> COIN_PRIZE_ID
                    else -> null
                }
                if (packageId == null) continue
                val qty = if (packageId == COIN_PRIZE_ID) (prize.coins ?: 0) else prize.qty
                if (qty <= 0) continue
                transaction {
                    UserPrizes.insert {
                        it[UserPrizes.userId] = lb.userId
                        it[UserPrizes.tournamentId] = t.id
                        it[UserPrizes.packageId] = packageId
                        it[UserPrizes.qty] = qty
                        it[UserPrizes.claimed] = false
                    }
                }
            }
        }
    }
}
