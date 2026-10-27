package stub

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SequentialRunnerTest {
    @Test
    fun completesEachAccountBeforeTenSecondPauseAndNextAccount() = runBlocking {
        val events = mutableListOf<String>()
        runSequentially(listOf(1, 2, 3), once = true,
            awaitNext = { events += "pause:$it" },
            onError = { _, e -> throw e },
        ) { uid ->
            events += "start:$uid"
            events += "done:$uid"
        }
        assertEquals(listOf("start:1", "done:1", "pause:10000", "start:2", "done:2", "pause:10000", "start:3", "done:3"), events)
    }

    @Test
    fun failureStillPausesBeforeNextAccountAndWrapsToFirst() = runBlocking {
        val events = mutableListOf<String>()
        var attempts = 0
        assertFailsWith<CancellationException> {
            runSequentially(listOf(1, 2),
                awaitNext = { events += "pause:$it" },
                onError = { uid, _ -> events += "error:$uid" },
            ) { uid ->
                if (++attempts == 4) throw CancellationException("test complete")
                events += "cycle:$uid"
                if (attempts == 1) error("one account failed")
            }
        }
        assertEquals(listOf("cycle:1", "error:1", "pause:10000", "cycle:2", "pause:10000", "cycle:1", "pause:10000"), events)
    }
}
