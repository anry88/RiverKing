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
import kotlin.random.Random

data class WorkerConfig(val intervalSeconds: Long = 300, val staggerSeconds: Long = 2) {
    init {
        require(intervalSeconds in 30..86_400) { "STUB_INTERVAL_SECONDS must be 30..86400" }
        require(staggerSeconds in 0..60) { "STUB_STAGGER_SECONDS must be 0..60" }
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
        intervalSeconds = System.getenv("STUB_INTERVAL_SECONDS")?.toLong() ?: 300,
        staggerSeconds = System.getenv("STUB_STAGGER_SECONDS")?.toLong() ?: 2,
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
                    log.info("Roster ready accounts={} russian=25 english=25 source=bot intervalSeconds={}", ids.size, config.intervalSeconds)
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
                        coroutineScope {
                            ids.mapIndexed { index, uid ->
                                launch {
                                    delay(index * config.staggerSeconds * 1_000)
                                    val cycle = GameCycle()
                                    do {
                                        try {
                                            val result = cycle.run(uid)
                                            if (result == GameCycle.Result.CAUGHT) caught.incrementAndGet()
                                            completed.incrementAndGet()
                                            log.info("Cycle complete userId={} result={}", uid, result)
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            failed.incrementAndGet()
                                            log.error("Cycle failed userId={}; retrying next interval", uid, e)
                                        }
                                        if ("--once" in args) break
                                        // Keep accounts spread out after restarts and avoid synchronized database bursts.
                                        delay(config.intervalSeconds * 1_000 + Random.nextLong(0, 15_001))
                                    } while (isActive)
                                }
                            }.joinAll()
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
