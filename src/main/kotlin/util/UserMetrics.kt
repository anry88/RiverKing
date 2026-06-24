package util

import db.Clubs
import db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.temporal.ChronoUnit

object UserMetrics {
    fun update(clock: Clock = Clock.systemUTC()) {
        val now = clock.instant()
        val dayCutoff = now.minus(1, ChronoUnit.DAYS)
        val weekCutoff = now.minus(7, ChronoUnit.DAYS)
        val monthCutoff = now.minus(30, ChronoUnit.DAYS)

        transaction {
            val total = Users.selectAll().count()
            val day = Users.select { Users.lastSeenAt greaterEq dayCutoff }.count()
            val week = Users.select { Users.lastSeenAt greaterEq weekCutoff }.count()
            val month = Users.select { Users.lastSeenAt greaterEq monthCutoff }.count()
            val clubsTotal = Clubs.selectAll().count()

            Metrics.gauge("unique_users", day, mapOf("period" to "day"))
            Metrics.gauge("unique_users", week, mapOf("period" to "week"))
            Metrics.gauge("unique_users", month, mapOf("period" to "month"))
            Metrics.gauge("unique_users", total, mapOf("period" to "total"))
            Metrics.gauge("unique_clubs", clubsTotal, mapOf("period" to "total"))
            emitRegistrationMetrics("day") { Users.createdAt greaterEq dayCutoff }
            emitRegistrationMetrics("week") { Users.createdAt greaterEq weekCutoff }
            emitRegistrationMetrics("month") { Users.createdAt greaterEq monthCutoff }
            emitRegistrationMetrics("total") { null }
        }
    }

    private fun emitRegistrationMetrics(
        period: String,
        condition: () -> Op<Boolean>?,
    ) {
        val countExpr = Users.id.count()
        val query = Users
            .slice(Users.registrationSource, countExpr)
            .selectAll()
        condition()?.let { query.andWhere { it } }
        query
            .groupBy(Users.registrationSource)
            .forEach { row ->
                val source = row[Users.registrationSource] ?: "unknown"
                Metrics.gauge(
                    "user_registrations",
                    row[countExpr],
                    mapOf("period" to period, "source" to source),
                )
            }
    }
}
