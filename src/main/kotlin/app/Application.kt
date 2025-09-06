package app

import db.DB
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*

fun main() {
    val env = Env.fromConfig()
    embeddedServer(Netty, port = env.port) {
        install(ContentNegotiation) { json() }
        install(CORS) {
            allowMethod(HttpMethod.Get); allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType); allowCredentials = true
            anyHost() // прод: ограничь доменом мини-аппа
        }
        installSessions(env.devMode)
        DB.init(env)

        // API for Mini App
        apiRoutes(env)

        // Static Mini App (served from resources/webapp)
        routing {
            staticResources("/app", "webapp")
            get("/") { call.respondRedirect("/app", permanent = false) }
            get("/health") { call.respondText("OK") }
        }

        Scheduler.install(this)
    }.start(wait = true)
}
