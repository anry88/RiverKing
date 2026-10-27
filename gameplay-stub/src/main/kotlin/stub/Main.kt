package stub

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

fun main(args: Array<String>) {
    val state = Path.of(System.getenv("STUB_STATE_DIR") ?: "/data/gameplay-stub")
    if (args.contentEquals(arrayOf("--health"))) {
        val heartbeat = state.resolve("heartbeat")
        check(Files.exists(heartbeat) && Files.getLastModifiedTime(heartbeat).toInstant()
            .isAfter(Instant.now().minusSeconds(90))) { "Worker heartbeat is stale" }
        return
    }
    require(args.all { it == "--once" }) { "Options: --once, --health" }
    val pauseSeconds = System.getenv("STUB_ACCOUNT_PAUSE_SECONDS")?.toLong() ?: 10L
    require(pauseSeconds in 1..3_600)
    val baseUrl = System.getenv("STUB_SERVER_URL") ?: error("STUB_SERVER_URL is required")
    val uri = java.net.URI(baseUrl)
    require(uri.scheme in listOf("http", "https") && uri.host != null && uri.userInfo == null)
    Files.createDirectories(state)
    val log = LoggerFactory.getLogger("GameplayStub")
    FileChannel.open(state.resolve("worker.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
        val lock = channel.tryLock() ?: error("Another gameplay-stub worker is running")
        lock.use {
            val store = SessionStore(state.resolve("accounts.json"))
            GameServer(baseUrl, store).use { server ->
                try {
                    runBlocking {
                        val workerJob = coroutineContext.job
                        val shutdown = Thread { runBlocking { workerJob.cancelAndJoin() } }
                        Runtime.getRuntime().addShutdownHook(shutdown)
                        try {
                            server.initialize()
                            val ids = store.accounts().map { it.userId }
                            log.info("HTTP client ready accounts=50 server={} accountPauseSeconds={}", baseUrl, pauseSeconds)
                            val completed = AtomicLong()
                            val caught = AtomicLong()
                            val failed = AtomicLong()
                            val heartbeat = launch(Dispatchers.IO) {
                                while (isActive) {
                                    val tmp = state.resolve("heartbeat.tmp")
                                    Files.writeString(tmp, "at=${Instant.now()} accounts=50 cycles=${completed.get()} caught=${caught.get()} errors=${failed.get()}\n")
                                    Files.move(tmp, state.resolve("heartbeat"), StandardCopyOption.REPLACE_EXISTING)
                                    delay(15_000)
                                }
                            }
                            try {
                                runSequentially(ids, once = "--once" in args, pauseMillis = pauseSeconds * 1_000,
                                    onError = { uid, e -> failed.incrementAndGet(); log.error("Cycle failed userId={}", uid, e) },
                                ) { uid ->
                                    log.info("Cycle started userId={}", uid)
                                    val result = GameCycle(server.forAccount(uid)).run()
                                    if (result == GameCycle.Result.CAUGHT) caught.incrementAndGet()
                                    completed.incrementAndGet()
                                    log.info("Cycle complete userId={} result={}", uid, result)
                                }
                            } finally {
                                withContext(NonCancellable) {
                                    heartbeat.cancelAndJoin()
                                    Files.deleteIfExists(state.resolve("heartbeat"))
                                    log.info("Worker stopped cycles={} caught={} errors={}", completed.get(), caught.get(), failed.get())
                                }
                            }
                        } finally {
                            try { Runtime.getRuntime().removeShutdownHook(shutdown) } catch (_: IllegalStateException) { }
                        }
                    }
                } catch (_: CancellationException) {
                    log.info("Worker shutdown requested")
                }
            }
        }
    }
}
