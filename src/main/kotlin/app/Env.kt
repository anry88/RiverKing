package app

data class Env(
    val botToken: String = requireNotNull(System.getenv("BOT_TOKEN")) { "BOT_TOKEN required" },
    val webhookSecret: String = System.getenv("WEBHOOK_SECRET") ?: "dev-secret",
    val publicBaseUrl: String = requireNotNull(System.getenv("PUBLIC_BASE_URL")) { "PUBLIC_BASE_URL required" },
    val dbUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/riverking",
    val dbUser: String = System.getenv("DATABASE_USER") ?: "postgres",
    val dbPass: String = System.getenv("DATABASE_PASSWORD") ?: "postgres",
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080,
    val devMode: Boolean = (System.getenv("DEV_MODE") ?: "false").equals("true", ignoreCase = true)
)
