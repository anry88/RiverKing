package stub

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/** Exactly one account at a time: complete cycle, pause, next account; wrap after the last. */
suspend fun runSequentially(
    ids: List<Long>,
    once: Boolean = false,
    pauseMillis: Long = 0,
    awaitNext: suspend (Long) -> Unit = { delay(it) },
    canStart: (Long) -> Boolean = { true },
    onError: (Long, Exception) -> Unit,
    cycle: suspend (Long) -> Unit,
) {
    require(ids.isNotEmpty())
    do {
        for ((index, uid) in ids.withIndex()) {
            currentCoroutineContext().ensureActive()
            if (!canStart(uid)) continue
            try {
                cycle(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(uid, e)
            }
            if (!once || index != ids.lastIndex) awaitNext(pauseMillis)
        }
    } while (!once)
}
