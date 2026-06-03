package app

import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class EnvConfigTest {
    @Test
    fun loadsConfigFromFilesystemPath() {
        val config = createTempFile(prefix = "riverking-config-", suffix = ".properties")
        try {
            config.writeText(
                """
                BOT_TOKEN=TEST
                BOT_NAME=river_king_bot
                PUBLIC_BASE_URL=http://localhost:5005
                DATABASE_URL=jdbc:sqlite:riverking.db
                PORT=5005
                DEV_MODE=true
                """.trimIndent()
            )

            val env = Env.fromConfig(config.toString())

            assertEquals("TEST", env.botToken)
            assertEquals("river_king_bot", env.botName)
            assertEquals("http://localhost:5005", env.publicBaseUrl)
            assertEquals("jdbc:sqlite:riverking.db", env.dbUrl)
            assertEquals(5005, env.port)
            assertEquals(true, env.devMode)
        } finally {
            config.deleteIfExists()
        }
    }
}
