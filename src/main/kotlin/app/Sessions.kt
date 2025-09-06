package app

import io.ktor.server.application.*
import io.ktor.server.sessions.*

data class AppSession(val tgId: Long)

fun Application.installSessions(devMode: Boolean) {
    install(Sessions) {
        cookie<AppSession>("sid") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = !devMode // в DEV разрешим http
            cookie.maxAgeInSeconds = 60L * 60 * 24 * 30
        }
    }
}
