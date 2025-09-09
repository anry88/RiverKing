package service

import db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

data class Tournament(
    val id: Long,
    val name: String,
    val startTime: Instant,
    val endTime: Instant,
    val fish: String?,
    val location: String?,
    val metric: String,
    val prizePlaces: Int,
    val prizesJson: String,
)

class TournamentService {
    fun createTournament(
        name: String,
        start: Instant,
        end: Instant,
        fish: String?,
        location: String?,
        metric: String,
        prizePlaces: Int,
        prizes: String,
    ): Long = transaction {
        Tournaments.insert {
            it[Tournaments.name] = name
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
                name = row[Tournaments.name],
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
                name = row[Tournaments.name],
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
        name: String,
        start: Instant,
        end: Instant,
        fish: String?,
        location: String?,
        metric: String,
        prizePlaces: Int,
        prizes: String,
    ) = transaction {
        Tournaments.update({ Tournaments.id eq id }) {
            it[Tournaments.name] = name
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
                name = row[Tournaments.name],
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
                name = row[Tournaments.name],
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
    )

    private fun rowUser(row: ResultRow): String? {
        val fn = row.getOrNull(Users.firstName)
        val ln = row.getOrNull(Users.lastName)
        val un = row.getOrNull(Users.username)
        val nn = row.getOrNull(Users.nickname)
        return when {
            !nn.isNullOrBlank() -> nn
            !fn.isNullOrBlank() || !ln.isNullOrBlank() -> listOfNotNull(fn, ln).joinToString(" ").trim()
            !un.isNullOrBlank() -> un
            else -> null
        }
    }

    fun leaderboard(t: Tournament, userId: Long, limit: Int = 10): Pair<List<LeaderboardEntry>, LeaderboardEntry?> = transaction {
        var cond: Op<Boolean> = (Catches.createdAt greaterEq t.startTime) and (Catches.createdAt lessEq t.endTime)
        if (t.fish != null) cond = cond and (Fish.name eq t.fish)
        if (t.location != null) cond = cond and (Locations.name eq t.location)
        val rows = ((Catches leftJoin Users) innerJoin Fish)
            .join(Locations, JoinType.INNER, onColumn = Catches.locationId, otherColumn = Locations.id)
            .select { cond }
            .map { row -> Triple(row[Catches.userId].value, rowUser(row), row[Catches.weight]) }
        val grouped = rows.groupBy { it.first }
            .map { (uid, list) ->
                val name = list.first().second
                val weights = list.map { it.third }
                val value = when (t.metric.lowercase()) {
                    "smallest" -> weights.minOrNull() ?: 0.0
                    "count" -> list.size.toDouble()
                    else -> weights.maxOrNull() ?: 0.0
                }
                LeaderboardEntry(uid, name, value, 0)
            }
        val sorted = when (t.metric.lowercase()) {
            "smallest" -> grouped.sortedBy { it.value }
            "count" -> grouped.sortedByDescending { it.value }
            else -> grouped.sortedByDescending { it.value }
        }
        val ranked = sorted.mapIndexed { index, e -> e.copy(rank = index + 1) }
        val top = ranked.take(limit)
        val mine = ranked.find { it.userId == userId }
        Pair(top, mine)
    }
}
