package stub

import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.*

class BotScheduleTest {
    private val accounts = (1..50).map { i ->
        AccountSession("v1-${i.toString().padStart(2, '0')}", i.toLong(), "bot-$i",
            if (i <= 25) "ru" else "en", "access-$i", "refresh-$i")
    }

    @Test
    fun assignsFiveAlwaysAndThreeDisjointMixedLanguageGroupsOfFifteenStably() {
        val schedule = BotSchedule(accounts)
        val reordered = BotSchedule(accounts.reversed())
        assertEquals(mapOf(BotShift.ALWAYS to 5, BotShift.DAY to 15,
            BotShift.EVENING to 15, BotShift.NIGHT to 15), accounts.groupingBy { schedule.shift(it.userId) }.eachCount())
        assertEquals(setOf(1L, 26L, 2L, 27L, 3L), accounts.filter { schedule.shift(it.userId) == BotShift.ALWAYS }.map { it.userId }.toSet())
        accounts.forEach { assertEquals(schedule.shift(it.userId), reordered.shift(it.userId)) }
        BotShift.values().forEach { shift ->
            assertEquals(setOf("ru", "en"), accounts.filter { schedule.shift(it.userId) == shift }.map { it.language }.toSet())
        }
    }

    @Test
    fun exactShiftBoundariesAndMidnightUseInclusiveStartAndExclusiveEnd() {
        val cases = mapOf(
            "00:00" to setOf(BotShift.EVENING),
            "00:59:59.999999999" to setOf(BotShift.EVENING),
            "01:00" to setOf(BotShift.NIGHT),
            "08:59:59.999999999" to setOf(BotShift.NIGHT),
            "09:00" to setOf(BotShift.DAY),
            "15:59:59.999999999" to setOf(BotShift.DAY),
            "16:00" to setOf(BotShift.DAY),
            "16:59:59.999999999" to setOf(BotShift.DAY),
            "17:00" to setOf(BotShift.EVENING),
            "23:59:59.999999999" to setOf(BotShift.EVENING),
        )
        cases.forEach { (time, shifts) ->
            assertEquals(shifts + BotShift.ALWAYS,
                BotShift.values().filter { it.isActive(LocalTime.parse(time)) }.toSet(), time)
        }
    }

    @Test
    fun allDayHasTwentyEligibleBotsWithNoGapsOrOverlappingShifts() {
        for (hour in 0..23) {
            val clock = Clock.fixed(Instant.parse("2026-11-03T${hour.toString().padStart(2, '0')}:00:00Z"), ZoneOffset.UTC)
            val schedule = BotSchedule(accounts, clock)
            assertEquals(20, accounts.count { schedule.isActive(it.userId) }, "hour=$hour")
        }
    }

    @Test
    fun usesConfiguredServerZoneAndReevaluatesClockBeforeEachAccount() {
        var now = Instant.parse("2026-11-03T08:59:59Z")
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC
            override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
            override fun instant(): Instant = now
        }
        val schedule = BotSchedule(accounts, clock)
        val day = accounts.first { schedule.shift(it.userId) == BotShift.DAY }.userId
        val night = accounts.first { schedule.shift(it.userId) == BotShift.NIGHT }.userId
        assertFalse(schedule.isActive(day)); assertTrue(schedule.isActive(night))
        // A different local zone must not accidentally be used for server UTC windows.
        assertTrue(BotSchedule(accounts, clock.withZone(ZoneId.of("Europe/Belgrade"))).isActive(day))
        now = Instant.parse("2026-11-03T09:00:00Z")
        assertTrue(schedule.isActive(day)); assertFalse(schedule.isActive(night))
    }
}
