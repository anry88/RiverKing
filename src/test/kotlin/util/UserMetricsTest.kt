package util

import db.DB
import db.Users
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import service.FishingService
import support.testEnv

class UserMetricsTest {
    private class MutableClock(private var current: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = current
    }

    @Test
    fun exposesUniqueUserCounts() {
        val env = testEnv("user_metrics")
        DB.init(env)
        val now = Instant.parse("2024-02-01T00:00:00Z")
        val clock = MutableClock(now)
        val service = FishingService(clock)

        listOf(101L, 102L, 103L, 104L).forEach { tgId -> service.ensureUserByTgId(tgId) }

        transaction {
            Users.update({ Users.tgId eq 102L }) { it[Users.lastSeenAt] = now.minus(3, ChronoUnit.DAYS) }
            Users.update({ Users.tgId eq 103L }) { it[Users.lastSeenAt] = now.minus(20, ChronoUnit.DAYS) }
            Users.update({ Users.tgId eq 104L }) { it[Users.lastSeenAt] = now.minus(40, ChronoUnit.DAYS) }
        }

        UserMetrics.update(clock)

        val metricLines = Metrics.dump().lineSequence().filter { it.startsWith("unique_users") }.toSet()
        assertTrue(metricLines.contains("unique_users{period=\"day\"} 1.0"))
        assertTrue(metricLines.contains("unique_users{period=\"week\"} 2.0"))
        assertTrue(metricLines.contains("unique_users{period=\"month\"} 3.0"))
        assertTrue(metricLines.contains("unique_users{period=\"total\"} 4.0"))
    }
}
