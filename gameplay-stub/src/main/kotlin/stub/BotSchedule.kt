package stub

import java.time.Clock
import java.time.LocalTime

enum class BotShift(val window: String) {
    ALWAYS("24/7"), DAY("09:00-17:00"), EVENING("16:00-01:00"), NIGHT("01:00-09:00");

    // Start is inclusive, end is exclusive; the evening shift crosses midnight.
    fun isActive(time: LocalTime): Boolean = when (this) {
        ALWAYS -> true
        DAY -> time.hour in 9..16
        EVENING -> time.hour >= 16 || time.hour < 1
        NIGHT -> time.hour in 1..8
    }
}

class BotSchedule(accounts: List<AccountSession>, private val clock: Clock = Clock.systemUTC()) {
    private val shifts: Map<Long, BotShift>

    init {
        require(accounts.size == 50 && accounts.map { it.userId }.distinct().size == 50 &&
            accounts.map { it.slot }.distinct().size == 50) { "Expected 50 unique bot accounts" }
        val russian = accounts.filter { it.language == "ru" }.sortedBy { it.slot }
        val english = accounts.filter { it.language == "en" }.sortedBy { it.slot }
        require(russian.size == 25 && english.size == 25)
        // Stable slot order preserves membership across restarts and session-file reordering.
        val ordered = russian.zip(english) { ru, en -> listOf(ru, en) }.flatten()
        shifts = ordered.mapIndexed { index, account ->
            account.userId to when (index) {
                in 0..4 -> BotShift.ALWAYS
                in 5..19 -> BotShift.DAY
                in 20..34 -> BotShift.EVENING
                else -> BotShift.NIGHT
            }
        }.toMap()
    }

    fun shift(userId: Long): BotShift = shifts.getValue(userId)
    fun isActive(userId: Long): Boolean = shift(userId).isActive(LocalTime.now(clock))
}
