package stub

import app.Env
import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager

/** A session-level PostgreSQL lock prevents two deployments from driving the same accounts. */
class WorkerLease private constructor(private val connection: Connection) : Closeable {
    fun check() {
        check(connection.isValid(5)) { "Worker database lease connection lost" }
    }

    override fun close() = connection.close()

    companion object {
        fun acquire(env: Env): WorkerLease {
            require(env.dbUrl.startsWith("jdbc:postgresql:")) {
                "The standalone worker requires PostgreSQL; start/initialize the backend first."
            }
            Class.forName("org.postgresql.Driver")
            val connection = DriverManager.getConnection(env.dbUrl, env.dbUser, env.dbPass)
            try {
                connection.createStatement().use { stmt ->
                    stmt.queryTimeout = 5
                    stmt.executeQuery("SELECT pg_try_advisory_lock(1380665931, 186)").use { result ->
                        check(result.next() && result.getBoolean(1)) { "Another gameplay-stub worker is running" }
                    }
                }
                return WorkerLease(connection)
            } catch (e: Exception) {
                connection.close()
                throw e
            }
        }
    }
}
