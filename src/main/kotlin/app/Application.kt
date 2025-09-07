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
import util.Metrics

fun main() {
    val env = Env.fromConfig()
    embeddedServer(Netty, port = env.port) {
        install(ContentNegotiation) { json() }
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

        // Static Mini App (served from resources/webapp)
        routing {
            staticResources("/app", "webapp")
            get("/") {
                val qs = call.request.rawQueryParameters.formUrlEncode()
                val target = if (qs.isBlank()) "/app" else "/app?$qs"
                call.respondRedirect(target, permanent = false)
            }
            get("/health") { call.respondText("OK") }
            get("/metrics") { call.respondText(Metrics.dump(), ContentType.Text.Plain) }
        }

        Scheduler.install(this)
    }.start(wait = true)
}
