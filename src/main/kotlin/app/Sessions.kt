package app

import io.ktor.server.application.*
import io.ktor.server.sessions.*

data class AppSession(val tgId: Long)

/**
 * Configure session cookies. Cookies should only be marked `Secure`
 * when the application is served via HTTPS; otherwise browsers will
 * drop them and authentication will silently fail. Previously this
 * was tied to `devMode`, which broke local HTTP testing when
 * `DEV_MODE` was disabled. Now we inspect [Env.publicBaseUrl] to
 * decide whether HTTPS is expected.
 */
fun Application.installSessions(env: Env) {
    install(Sessions) {
        cookie<AppSession>("sid") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = env.publicBaseUrl.startsWith("https", ignoreCase = true)
            cookie.maxAgeInSeconds = 60L * 60 * 24 * 30
            cookie.extensions["SameSite"] = "None"
        }
    }
}
