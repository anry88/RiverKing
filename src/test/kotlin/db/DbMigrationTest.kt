package db

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import service.FishingService
import support.testEnv
import java.sql.DriverManager
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DbMigrationTest {
    @Test
    fun `db init migrates users tg_id to nullable and backfills telegram identity`() {
        val env = testEnv("db-migration").copy(devMode = false)
        DB.init(env)

        val userId = transaction {
            val insertedUserId = Users.insertAndGetId {
                it[tgId] = 4321L
                it[level] = 1
                it[xp] = 0
                it[coins] = 0
                it[createdAt] = Instant.now()
                it[lastSeenAt] = Instant.now()
                it[language] = "en"
                it[currentLureId] = Lures.select { Lures.name eq "Пресная мирная" }.single()[Lures.id]
                it[currentRodId] = Rods.select { Rods.code eq "spark" }.single()[Rods.id]
            }.value

            AuthIdentities.deleteWhere {
                (AuthIdentities.provider eq "telegram") and
                    (AuthIdentities.userId eq insertedUserId)
            }
            insertedUserId
        }

        TransactionManager.defaultDatabase?.let { TransactionManager.closeAndUnregister(it) }

        DriverManager.getConnection(env.dbUrl).use { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute("PRAGMA foreign_keys=OFF")
                stmt.execute(
                    """
                    CREATE TABLE users_legacy (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tg_id BIGINT NOT NULL,
                        first_name VARCHAR(100),
                        last_name VARCHAR(100),
                        username VARCHAR(100),
                        nickname VARCHAR(100),
                        language VARCHAR(10) NOT NULL DEFAULT 'en',
                        level INTEGER NOT NULL,
                        xp INTEGER NOT NULL,
                        coins BIGINT NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL,
                        last_seen_at TIMESTAMP,
                        last_daily_at TIMESTAMP,
                        daily_streak INTEGER NOT NULL DEFAULT 0,
                        current_location_id BIGINT REFERENCES ${Locations.tableName}(id),
                        current_lure_id BIGINT REFERENCES ${Lures.tableName}(id),
                        current_rod_id BIGINT REFERENCES ${Rods.tableName}(id),
                        cast_lure_id BIGINT REFERENCES ${Lures.tableName}(id),
                        is_casting BOOLEAN NOT NULL DEFAULT 0,
                        last_cast_at TIMESTAMP,
                        auto_fish_until TIMESTAMP,
                        referred_by BIGINT REFERENCES ${Users.tableName}(id),
                        can_receive_notifications BOOLEAN NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                stmt.execute(
                    """
                    INSERT INTO users_legacy (
                        id,
                        tg_id,
                        first_name,
                        last_name,
                        username,
                        nickname,
                        language,
                        level,
                        xp,
                        coins,
                        created_at,
                        last_seen_at,
                        last_daily_at,
                        daily_streak,
                        current_location_id,
                        current_lure_id,
                        current_rod_id,
                        cast_lure_id,
                        is_casting,
                        last_cast_at,
                        auto_fish_until,
                        referred_by,
                        can_receive_notifications
                    )
                    SELECT
                        id,
                        tg_id,
                        first_name,
                        last_name,
                        username,
                        nickname,
                        language,
                        level,
                        xp,
                        coins,
                        created_at,
                        last_seen_at,
                        last_daily_at,
                        daily_streak,
                        current_location_id,
                        current_lure_id,
                        current_rod_id,
                        cast_lure_id,
                        is_casting,
                        last_cast_at,
                        auto_fish_until,
                        referred_by,
                        can_receive_notifications
                    FROM users
                    """.trimIndent()
                )
                stmt.execute("DROP TABLE users")
                stmt.execute("ALTER TABLE users_legacy RENAME TO users")
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS users_tg_id_unique ON users(tg_id)")
                stmt.execute("PRAGMA foreign_keys=ON")
            }
        }

        DB.init(env)
        val migratedUsersSql = DriverManager.getConnection(env.dbUrl).use { connection ->
            connection.createStatement().use { stmt ->
                stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='users'").use { rs ->
                    if (rs.next()) rs.getString("sql").orEmpty() else ""
                }
            }
        }
        assertFalse(
            migratedUsersSql.contains("tg_id BIGINT NOT NULL", ignoreCase = true),
            migratedUsersSql,
        )
        val mobileUserId = FishingService().createUser(language = "en")

        transaction {
            val identity = AuthIdentities
                .select {
                    (AuthIdentities.provider eq "telegram") and
                        (AuthIdentities.subject eq "4321")
                }
                .singleOrNull()
            assertNotNull(identity)
            assertEquals(userId, identity[AuthIdentities.userId].value)

            val migratedUser = Users.select { Users.id eq userId }.single()
            assertEquals(4321L, migratedUser[Users.tgId])

            val mobileUser = Users.select { Users.id eq mobileUserId }.single()
            assertNull(mobileUser[Users.tgId])
        }
    }
}
