package support

import app.Env
import java.nio.file.Files

fun testEnv(name: String): Env {
    val dbFile = Files.createTempFile("riverking-$name-", ".db").toFile().apply {
        deleteOnExit()
    }

    return Env(
    botToken = "",
    telegramWebhookSecret = "",
    publicBaseUrl = "http://localhost",
    itchProjectUrl = "",
    playStoreUrl = "",
    androidDirectDownloadUrl = "",
    dbUrl = "jdbc:sqlite:${dbFile.absolutePath}",
    dbUser = "",
    dbPass = "",
    port = 0,
    devMode = true,
    adminTgId = 0L,
    providerToken = "",
    botName = "",
    tgAnalyticsToken = "",
    tgAnalyticsScriptUrl = "",
    tgAnalyticsAppName = "",
    authTokenSecret = "test-secret",
    authAccessTokenTtlMinutes = 60L,
    authRefreshTokenTtlDays = 30L,
    googleAuthClientId = "",
    googlePlayPackageName = "",
    googlePlayServiceAccountFile = "",
)
}
