package app

import java.util.Properties

data class Env(
    val botToken: String,
    val telegramWebhookSecret: String,
    val publicBaseUrl: String,
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
    val googleAuthClientId: String,
) {
    companion object {
        fun fromConfig(path: String = "config.properties"): Env {
            val props = Properties()
            Env::class.java.classLoader.getResourceAsStream(path)?.use { props.load(it) }
                ?: error("config file $path not found")
            return Env(
                botToken = props.getProperty("BOT_TOKEN") ?: error("BOT_TOKEN required"),
                telegramWebhookSecret =
                    props.getProperty("TELEGRAM_WEBHOOK_SECRET")
                        ?: props.getProperty("WEBHOOK_SECRET")
                        ?: "dev-secret",
                publicBaseUrl = props.getProperty("PUBLIC_BASE_URL") ?: error("PUBLIC_BASE_URL required"),
                dbUrl = props.getProperty("DATABASE_URL") ?: "jdbc:sqlite:/data/riverking.db",
                dbUser = props.getProperty("DATABASE_USER") ?: "postgres",
                dbPass = props.getProperty("DATABASE_PASSWORD") ?: "postgres",
                port = props.getProperty("PORT")?.toIntOrNull() ?: 8080,
                devMode = props.getProperty("DEV_MODE")?.equals("true", ignoreCase = true) ?: false,
                adminTgId = props.getProperty("ADMIN_TG_ID")?.toLongOrNull() ?: 0L,
                providerToken = props.getProperty("PROVIDER_TOKEN") ?: "stars",
                botName = props.getProperty("BOT_NAME") ?: error("BOT_NAME required"),
                tgAnalyticsToken = props.getProperty("TG_ANALYTICS_TOKEN") ?: "",
                tgAnalyticsScriptUrl = props.getProperty("TG_ANALYTICS_SCRIPT_URL") ?: "",
                tgAnalyticsAppName = props.getProperty("TG_ANALYTICS_APP_NAME") ?: "",
                authTokenSecret = props.getProperty("AUTH_TOKEN_SECRET")
                    ?: props.getProperty("BOT_TOKEN")
                    ?: error("AUTH_TOKEN_SECRET required"),
                authAccessTokenTtlMinutes = props.getProperty("AUTH_ACCESS_TOKEN_TTL_MINUTES")?.toLongOrNull() ?: 60L,
                authRefreshTokenTtlDays = props.getProperty("AUTH_REFRESH_TOKEN_TTL_DAYS")?.toLongOrNull() ?: 30L,
                googleAuthClientId = props.getProperty("GOOGLE_AUTH_CLIENT_ID") ?: "",
            )
        }
    }
}
