package app

import java.io.File
import java.util.Properties

data class Env(
    val botToken: String,
    val telegramWebhookSecret: String,
    val publicBaseUrl: String,
    val itchProjectUrl: String,
    val playStoreUrl: String,
    val androidDirectDownloadUrl: String,
    val dbUrl: String,
    val dbUser: String,
    val dbPass: String,
    val dbMaxPoolSize: Int,
    val dbMinIdle: Int,
    val dbConnectionTimeoutMs: Long,
    val dbIdleTimeoutMs: Long,
    val dbMaxLifetimeMs: Long,
    val dbLeakDetectionThresholdMs: Long,
    val dbDispatcherThreads: Int,
    val port: Int,
    val devMode: Boolean,
    val adminTgId: Long,
    val providerToken: String,
    val botName: String,
    val tgAnalyticsToken: String,
    val tgAnalyticsScriptUrl: String,
    val tgAnalyticsAppName: String,
    val authTokenSecret: String,
    val authAccessTokenTtlMinutes: Long,
    val authRefreshTokenTtlDays: Long,
    val adminApiToken: String,
    val googleAuthClientId: String,
    val googlePlayPackageName: String,
    val googlePlayServiceAccountFile: String,
    val eventAssetsDir: String,
    val playDeckGameToken: String,
    val playDeckTestPayments: Boolean,
) {
    companion object {
        private const val DEFAULT_ITCH_PROJECT_URL = "https://anry88.itch.io/river-king"

        fun fromConfig(
            path: String = System.getenv("CONFIG_PATH")?.trim()?.takeIf { it.isNotEmpty() }
                ?: "config.properties"
        ): Env {
            val props = Properties()
            val loaded = loadProperties(path, props)
            if (!loaded && System.getenv("CONFIG_PATH")?.trim()?.isNotEmpty() == true) {
                error("config file $path not found")
            }

            fun configuredValue(vararg names: String): String? {
                names.forEach { name ->
                    props.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                }
                names.forEach { name ->
                    System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                }
                return null
            }
            fun environmentOverrideValue(vararg names: String): String? {
                names.forEach { name ->
                    System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                }
                names.forEach { name ->
                    props.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                }
                return null
            }
            return Env(
                botToken = configuredValue("BOT_TOKEN") ?: error("BOT_TOKEN required"),
                telegramWebhookSecret =
                    configuredValue("TELEGRAM_WEBHOOK_SECRET", "WEBHOOK_SECRET")
                        ?: "dev-secret",
                publicBaseUrl = configuredValue("PUBLIC_BASE_URL") ?: error("PUBLIC_BASE_URL required"),
                itchProjectUrl =
                    configuredValue("RIVERKING_ITCH_PROJECT_URL", "ITCH_PROJECT_URL")
                        ?: DEFAULT_ITCH_PROJECT_URL,
                playStoreUrl = configuredValue("RIVERKING_PLAY_STORE_URL", "PLAY_STORE_URL") ?: "",
                androidDirectDownloadUrl =
                    configuredValue("RIVERKING_ANDROID_DIRECT_DOWNLOAD_URL", "ANDROID_DIRECT_DOWNLOAD_URL")
                        ?: "",
                dbUrl = environmentOverrideValue("DATABASE_URL") ?: "jdbc:sqlite:/data/riverking.db",
                dbUser = environmentOverrideValue("DATABASE_USER") ?: "postgres",
                dbPass = environmentOverrideValue("DATABASE_PASSWORD") ?: "postgres",
                dbMaxPoolSize = environmentOverrideValue("DATABASE_MAX_POOL_SIZE")?.toIntOrNull()?.coerceAtLeast(1) ?: 10,
                dbMinIdle = environmentOverrideValue("DATABASE_MIN_IDLE")?.toIntOrNull()?.coerceAtLeast(0) ?: 2,
                dbConnectionTimeoutMs =
                    environmentOverrideValue("DATABASE_CONNECTION_TIMEOUT_MS")?.toLongOrNull()?.coerceAtLeast(250L) ?: 3000L,
                dbIdleTimeoutMs =
                    environmentOverrideValue("DATABASE_IDLE_TIMEOUT_MS")?.toLongOrNull()?.coerceAtLeast(0L) ?: 600000L,
                dbMaxLifetimeMs =
                    environmentOverrideValue("DATABASE_MAX_LIFETIME_MS")?.toLongOrNull()?.coerceAtLeast(30000L) ?: 1800000L,
                dbLeakDetectionThresholdMs =
                    environmentOverrideValue("DATABASE_LEAK_DETECTION_THRESHOLD_MS")?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                dbDispatcherThreads =
                    environmentOverrideValue("DATABASE_DISPATCHER_THREADS")?.toIntOrNull()?.coerceAtLeast(1) ?: 10,
                port = configuredValue("PORT")?.toIntOrNull() ?: 8080,
                devMode = configuredValue("DEV_MODE")?.equals("true", ignoreCase = true) ?: false,
                adminTgId = configuredValue("ADMIN_TG_ID")?.toLongOrNull() ?: 0L,
                providerToken = configuredValue("PROVIDER_TOKEN") ?: "stars",
                botName = configuredValue("BOT_NAME") ?: error("BOT_NAME required"),
                tgAnalyticsToken = configuredValue("TG_ANALYTICS_TOKEN") ?: "",
                tgAnalyticsScriptUrl = configuredValue("TG_ANALYTICS_SCRIPT_URL") ?: "",
                tgAnalyticsAppName = configuredValue("TG_ANALYTICS_APP_NAME") ?: "",
                authTokenSecret = configuredValue("AUTH_TOKEN_SECRET")
                    ?: configuredValue("BOT_TOKEN")
                    ?: error("AUTH_TOKEN_SECRET required"),
                authAccessTokenTtlMinutes = configuredValue("AUTH_ACCESS_TOKEN_TTL_MINUTES")?.toLongOrNull() ?: 60L,
                authRefreshTokenTtlDays = configuredValue("AUTH_REFRESH_TOKEN_TTL_DAYS")?.toLongOrNull() ?: 30L,
                adminApiToken = configuredValue("ADMIN_API_TOKEN") ?: "",
                googleAuthClientId = configuredValue("GOOGLE_AUTH_CLIENT_ID") ?: "",
                googlePlayPackageName = configuredValue("GOOGLE_PLAY_PACKAGE_NAME") ?: "",
                googlePlayServiceAccountFile = configuredValue("GOOGLE_PLAY_SERVICE_ACCOUNT_FILE") ?: "",
                eventAssetsDir = configuredValue("EVENT_ASSETS_DIR") ?: "data/event-assets",
                playDeckGameToken = configuredValue("PLAYDECK_GAME_TOKEN") ?: "",
                playDeckTestPayments =
                    configuredValue("PLAYDECK_TEST_PAYMENTS")?.equals("true", ignoreCase = true) ?: false,
            )
        }

        private fun loadProperties(path: String, props: Properties): Boolean {
            val file = File(path)
            if (file.isFile) {
                file.inputStream().use { props.load(it) }
                return true
            }

            Env::class.java.classLoader.getResourceAsStream(path)?.use {
                props.load(it)
                return true
            }

            return false
        }
    }
}
