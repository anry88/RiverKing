package db

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction as exposedTransaction
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object DbExecution {
    @Volatile
    private var dispatcher: ExecutorCoroutineDispatcher? = null

    @Synchronized
    fun configure(threadCount: Int) {
        dispatcher?.close()
        val count = threadCount.coerceAtLeast(1)
        val index = AtomicInteger(1)
        dispatcher = Executors.newFixedThreadPool(count, ThreadFactory { runnable ->
            Thread(runnable, "riverking-db-${index.getAndIncrement()}").apply {
                isDaemon = true
            }
        }).asCoroutineDispatcher()
    }

    suspend fun <T> blocking(block: () -> T): T =
        withContext(currentDispatcher()) { block() }

    suspend fun <T> transaction(block: () -> T): T =
        blocking { exposedTransaction { block() } }

    @Synchronized
    fun close() {
        dispatcher?.close()
        dispatcher = null
    }

    private fun currentDispatcher(): ExecutorCoroutineDispatcher =
        dispatcher ?: error("DB execution dispatcher is not configured")
}
