package app

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
) {
    companion object {
        private const val DEFAULT_ITCH_PROJECT_URL = "https://anry88.itch.io/river-king"

        fun fromConfig(path: String = "config.properties"): Env {
            val props = Properties()
            Env::class.java.classLoader.getResourceAsStream(path)?.use { props.load(it) }
                ?: error("config file $path not found")
            fun configuredValue(vararg names: String): String? {
                names.forEach { name ->
                    props.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                }
                names.forEach { name ->
                    System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
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
                dbUrl = configuredValue("DATABASE_URL") ?: "jdbc:sqlite:/data/riverking.db",
                dbUser = configuredValue("DATABASE_USER") ?: "postgres",
                dbPass = configuredValue("DATABASE_PASSWORD") ?: "postgres",
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
            )
        }
    }
}
