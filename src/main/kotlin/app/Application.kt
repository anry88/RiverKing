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
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import service.FishingService
import util.Metrics

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

        // Static Mini App (served from resources/webapp)
        routing {
            staticResources("/app", "webapp")
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
            get("/metrics") { call.respondText(Metrics.dump(), ContentType.Text.Plain) }
        }

        Scheduler.install(this)
    }.start(wait = true)
}
