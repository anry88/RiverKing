package service

import db.Tournaments
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
}
