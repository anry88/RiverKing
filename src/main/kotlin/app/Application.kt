package app

import db.DB
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import service.FishingService
import util.Metrics
import util.UserMetrics

fun main() {
    val env = Env.fromConfig()
    embeddedServer(Netty, port = env.port) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(CORS) {
            allowMethod(HttpMethod.Get); allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType); allowCredentials = true
            anyHost() // прод: ограничь доменом мини-аппа
        }
        install(DoubleReceive)
        installSessions(env)
        DB.init(env)

        // API for Mini App
        apiRoutes(env)

        // Telegram bot webhook
        botRoutes(env)

        val bot = TelegramBot(env.botToken)
        val fishing = FishingService()
        val log = LoggerFactory.getLogger("App")

        runBlocking {
            try {
                val englishCommands = defaultBotCommands()
                val russianCommands = russianBotCommands()

                bot.setMyCommands(englishCommands)
                bot.setMyCommands(englishCommands, scope = BotCommandScope.AllPrivateChats)
                bot.setMyCommands(englishCommands, scope = BotCommandScope.AllGroupChats)
                bot.setMyCommands(englishCommands, languageCode = "en")
                bot.setMyCommands(
                    englishCommands,
                    languageCode = "en",
                    scope = BotCommandScope.AllPrivateChats,
                )
                bot.setMyCommands(
                    englishCommands,
                    languageCode = "en",
                    scope = BotCommandScope.AllGroupChats,
                )

                bot.setMyCommands(russianCommands, languageCode = "ru")
                bot.setMyCommands(
                    russianCommands,
                    languageCode = "ru",
                    scope = BotCommandScope.AllPrivateChats,
                )
                bot.setMyCommands(
                    russianCommands,
                    languageCode = "ru",
                    scope = BotCommandScope.AllGroupChats,
                )
            } catch (e: Exception) {
                log.error("Failed to set bot commands", e)
            }
        }

        val restored = fishing.restoreCastingLuresOnStartup()
        if (restored > 0) {
            log.info("Restored lost lures for {} casting users on startup", restored)
        }

        // Static Mini App (served from resources/webapp)
        routing {
            staticResources("/app/assets", "webapp/assets")
            get("/app/config.js") {
                val js = """
                    window.APP_CONFIG = {
                        telemetree: {
                            projectId: "${env.telemetreeProjectId}",
                            apiKey: "${env.telemetreeApiKey}"
                        }
                    };
                """.trimIndent()
                call.respondText(js, ContentType.Application.JavaScript)
            }
            staticResources("/app", "webapp", "index.html")
            get("/") {
                val params = call.request.rawQueryParameters
                params["tgId"]?.toLongOrNull()?.let { tgId ->
                    try {
                        val uid = fishing.ensureUserByTgId(tgId)
                        val lang = fishing.userLanguage(uid)
                        val text = if (lang == "ru") {
                            "Для лучшего опыта запускайте игру через кнопку меню \"Open app\"."
                        } else {
                            "For the best experience, launch the game via the menu button labeled \"Open app\"."
                        }
                        bot.sendMessage(tgId, text)
                    } catch (e: Exception) {
                        log.error("start message failed tgId={}", tgId, e)
                    }
                }
                val qs = params.formUrlEncode()
                val target = if (qs.isBlank()) "/app" else "/app?$qs"
                call.respondRedirect(target, permanent = false)
            }
            get("/health") { call.respondText("OK") }
            get("/metrics") {
                UserMetrics.update()
                call.respondText(Metrics.dump(), ContentType.Text.Plain)
            }
        }

        Scheduler.install(this)
    }.start(wait = true)
}

private fun defaultBotCommands(): List<BotCommand> = listOf(
    BotCommand("startapp", "Open the game"),
    BotCommand("cast", "Cast your line"),
    BotCommand("autocast", "Start auto casting"),
    BotCommand("stop_autocast", "Stop auto casting"),
    BotCommand("bait", "Change your bait"),
    BotCommand("rod", "Choose your rod"),
    BotCommand("location", "Change your location"),
    BotCommand("daily", "Claim your daily reward"),
    BotCommand("achievements", "View your achievements"),
    BotCommand("prizes", "Claim tournament prizes"),
    BotCommand("shop", "Buy baits and rods with Stars"),
    BotCommand("coin_shop", "Buy bundles with coins"),
    BotCommand("tournament", "View the tournament leaderboard"),
    BotCommand("daily_rating", "View today's daily rating"),
    BotCommand("stats", "View your fishing stats"),
    BotCommand("language", "Choose your language"),
    BotCommand("nickname", "Change your nickname"),
)

private fun russianBotCommands(): List<BotCommand> = listOf(
    BotCommand("startapp", "Открыть игру"),
    BotCommand("cast", "Сделать заброс"),
    BotCommand("autocast", "Запустить автоловлю"),
    BotCommand("stop_autocast", "Остановить автоловлю"),
    BotCommand("bait", "Сменить приманку"),
    BotCommand("rod", "Выбрать удочку"),
    BotCommand("location", "Сменить локацию"),
    BotCommand("daily", "Получить ежедневную награду"),
    BotCommand("achievements", "Посмотреть достижения"),
    BotCommand("prizes", "Забрать призы турнира"),
    BotCommand("shop", "Купить приманки и удочки за звёзды"),
    BotCommand("coin_shop", "Купить наборы за монеты"),
    BotCommand("tournament", "Смотреть таблицу турнира"),
    BotCommand("daily_rating", "Текущий результат в ежедневном рейтинге"),
    BotCommand("stats", "Статистика уловов"),
    BotCommand("language", "Выбрать язык"),
    BotCommand("nickname", "Сменить ник"),
)
