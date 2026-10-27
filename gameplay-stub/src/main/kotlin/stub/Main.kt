package stub

import app.Env
import db.DB
import db.DbExecution
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

data class WorkerConfig(val accountPauseSeconds: Long = 10) {
    init {
        require(accountPauseSeconds in 1..3_600) { "STUB_ACCOUNT_PAUSE_SECONDS must be 1..3600" }
    }
}

fun main(args: Array<String>) {
    if (args.contentEquals(arrayOf("--health"))) {
        val heartbeat = Path.of(System.getenv("STUB_STATE_DIR") ?: "/data/gameplay-stub", "heartbeat")
        check(Files.exists(heartbeat) && Files.getLastModifiedTime(heartbeat).toInstant()
            .isAfter(Instant.now().minusSeconds(90))) { "Worker heartbeat is stale" }
        return
    }
    require(args.all { it == "--once" || it == "--provision-only" }) { "Options: --once, --provision-only, --health" }
    val config = WorkerConfig(
        accountPauseSeconds = System.getenv("STUB_ACCOUNT_PAUSE_SECONDS")?.toLong() ?: 10,
    )
    val env = Env.fromConfig()
    val log = LoggerFactory.getLogger("GameplayStub")
    WorkerLease.acquire(env).use { lease ->
        DB.init(env, initializeSchema = false)
        try {
            runBlocking {
                val workerJob = coroutineContext.job
                val shutdown = Thread { runBlocking { workerJob.cancelAndJoin() } }
                Runtime.getRuntime().addShutdownHook(shutdown)
                try {
                    val ids = DbExecution.blocking { Roster.provision(service.FishingService()) }
                    log.info("Roster ready accounts={} russian=25 english=25 source=bot accountPauseSeconds={}", ids.size, config.accountPauseSeconds)
                    if ("--provision-only" in args) return@runBlocking
                    val stateDir = Path.of(System.getenv("STUB_STATE_DIR") ?: "/data/gameplay-stub")
                    Files.createDirectories(stateDir)
                    val completed = AtomicLong()
                    val caught = AtomicLong()
                    val failed = AtomicLong()
                    val heartbeat = launch(Dispatchers.IO) {
                        while (isActive) {
                            lease.check()
                            val tmp = stateDir.resolve("heartbeat.tmp")
                            Files.writeString(tmp, "at=${Instant.now()} accounts=${ids.size} cycles=${completed.get()} caught=${caught.get()} errors=${failed.get()}\n")
                            Files.move(tmp, stateDir.resolve("heartbeat"), StandardCopyOption.REPLACE_EXISTING)
                            delay(15_000)
                        }
                    }
                    try {
                        val cycle = GameCycle()
                        runSequentially(
                            ids,
                            once = "--once" in args,
                            pauseMillis = config.accountPauseSeconds * 1_000,
                            onError = { uid, e ->
                                failed.incrementAndGet()
                                log.error("Cycle failed userId={}; continuing with next account after pause", uid, e)
                            },
                        ) { uid ->
                            log.info("Cycle started userId={}", uid)
                            val result = cycle.run(uid)
                            if (result == GameCycle.Result.CAUGHT) caught.incrementAndGet()
                            completed.incrementAndGet()
                            log.info("Cycle complete userId={} result={}", uid, result)
                        }
                    } finally {
                        heartbeat.cancelAndJoin()
                        Files.deleteIfExists(stateDir.resolve("heartbeat"))
                        log.info("Worker stopped cycles={} caught={} errors={}", completed.get(), caught.get(), failed.get())
                    }
                } finally {
                    // Removing a hook during JVM shutdown throws; cancellation still needs DB cleanup.
                    try {
                        Runtime.getRuntime().removeShutdownHook(shutdown)
                    } catch (_: IllegalStateException) {
                        // JVM shutdown is already in progress.
                    }
                }
            }
        } catch (_: CancellationException) {
            log.info("Worker shutdown requested")
        } finally {
            DB.close()
        }
    }
}
