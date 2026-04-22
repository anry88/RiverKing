package app

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import service.AccountDeletionService

fun Application.publicPagesRoutes(env: Env) {
    val deletionService = AccountDeletionService()
    val publicBaseUrl = env.publicBaseUrl.trimEnd('/')
    val supportBotUrl = "https://t.me/${env.botName}"
    val russianChannelUrl = "https://t.me/riverking_ru"
    val englishChannelUrl = "https://t.me/riverking_en"
    fun publicUrl(path: String): String = "$publicBaseUrl$path"
    val privacyUrl = publicUrl("/privacy")
    val termsUrl = publicUrl("/terms")
    val supportUrl = publicUrl("/support")
    val accountDeletionUrl = publicUrl("/account/delete")
    val accountDeletionRequestUrl = publicUrl("/account/delete/request")

    routing {
        get("/privacy") {
            call.respondText(
                htmlPage(
                    title = "RiverKing Privacy Policy",
                    body = """
                        <h1>RiverKing Privacy Policy</h1>
                        <p>RiverKing stores the account and gameplay data needed to run the game across the Telegram Mini App, bot, and Android client.</p>
                        <ul>
                            <li>Account data: login identity, linked Telegram identity, optional Google identity, session metadata.</li>
                            <li>Gameplay data: nickname, progression, inventory, catches, quests, achievements, clubs, referrals, and purchases.</li>
                            <li>Operational data: request logs, anti-abuse signals, and support requests required to operate the service.</li>
                        </ul>
                        <p>RiverKing does not use in-app ads. Paid Android entitlements in the Google Play build are verified against Google Play before rewards are granted.</p>
                        <p>You can request account deletion in-app or through the public <a href="$accountDeletionUrl">account deletion page</a>.</p>
                    """.trimIndent(),
                ),
                ContentType.Text.Html,
            )
        }

        get("/terms") {
            call.respondText(
                htmlPage(
                    title = "RiverKing Terms of Use",
                    body = """
                        <h1>RiverKing Terms of Use</h1>
                        <p>RiverKing is an online game service. By using the service, you agree not to abuse the game economy, impersonate other players, exploit automated access beyond the provided flows, or interfere with other players’ access.</p>
                        <p>Gameplay balances, reward rules, tournaments, and store offers may change over time. We may suspend or limit access for fraud, abuse, chargeback abuse, or attempts to manipulate game systems.</p>
                        <p>If you no longer want to use the service, you can delete your account in-app or through the public <a href="$accountDeletionUrl">account deletion page</a>.</p>
                    """.trimIndent(),
                ),
                ContentType.Text.Html,
            )
        }

        get("/support") {
            call.respondText(
                htmlPage(
                    title = "RiverKing Support",
                    body = """
                        <h1>RiverKing Support</h1>
                        <p>For gameplay and account help, use the official RiverKing bot or the public account deletion request form.</p>
                        <ul>
                            <li>Support bot: <a href="$supportBotUrl">$supportBotUrl</a></li>
                            <li>Russian Telegram channel: <a href="$russianChannelUrl">$russianChannelUrl</a></li>
                            <li>English Telegram channel: <a href="$englishChannelUrl">$englishChannelUrl</a></li>
                            <li>Privacy policy: <a href="$privacyUrl">RiverKing Privacy Policy</a></li>
                            <li>Terms of use: <a href="$termsUrl">RiverKing Terms of Use</a></li>
                            <li>Account deletion: <a href="$accountDeletionUrl">Account deletion request</a></li>
                        </ul>
                    """.trimIndent(),
                ),
                ContentType.Text.Html,
            )
        }

        get("/account/delete") {
            call.respondText(
                htmlPage(
                    title = "RiverKing Account Deletion",
                    body = """
                        <h1>Delete a RiverKing account</h1>
                        <p>You can delete your account immediately from the Android app profile menu. If you cannot access the app, submit the request form below and we will review it manually.</p>
                        <p>Deleting an account removes the linked gameplay profile, auth sessions, catches, progression, clubs, referrals, and account-linked purchase records from the game backend.</p>
                        <form method="post" action="$accountDeletionRequestUrl">
                            <label>Login or account name</label>
                            <input type="text" name="login" maxlength="100" placeholder="angler.one" />
                            <label>Auth provider</label>
                            <select name="provider">
                                <option value="">Unknown</option>
                                <option value="password">Password</option>
                                <option value="telegram">Telegram</option>
                                <option value="google">Google</option>
                            </select>
                            <label>Contact for follow-up</label>
                            <input type="text" name="contact" maxlength="255" placeholder="@telegram, email, or another reachable contact" required />
                            <label>Extra details</label>
                            <textarea name="note" rows="5" maxlength="2000" placeholder="Optional: linked Telegram username, purchase date, or any information that helps identify the account."></textarea>
                            <button type="submit">Submit deletion request</button>
                        </form>
                        <p class="muted">Need help first? Visit <a href="$supportUrl">RiverKing Support</a>.</p>
                    """.trimIndent(),
                ),
                ContentType.Text.Html,
            )
        }

        post("/account/delete/request") {
            val params = call.receiveParameters()
            val login = params["login"]?.trim()?.takeIf { it.isNotEmpty() }
            val provider = params["provider"]?.trim()?.takeIf { it.isNotEmpty() }
            val contact = params["contact"]?.trim().orEmpty()
            val note = params["note"]?.trim()?.takeIf { it.isNotEmpty() }

            if (contact.isBlank()) {
                return@post call.respondText(
                    htmlPage(
                        title = "RiverKing Account Deletion",
                        body = """
                            <h1>Delete a RiverKing account</h1>
                            <p>Contact information is required so we can verify and process the request.</p>
                            <p><a href="$accountDeletionUrl">Go back to the deletion form</a></p>
                        """.trimIndent(),
                    ),
                    ContentType.Text.Html,
                    HttpStatusCode.BadRequest,
                )
            }

            val requestId = deletionService.createPublicRequest(
                AccountDeletionService.PublicDeletionRequest(
                    requestedLogin = login,
                    authProvider = provider,
                    contact = contact,
                    note = note,
                ),
            )

            if (env.adminTgId > 0L) {
                runCatching {
                    TelegramBot(env.botToken).sendMessage(
                        env.adminTgId,
                        buildString {
                            appendLine("RiverKing account deletion request #$requestId")
                            login?.let { appendLine("login: $it") }
                            provider?.let { appendLine("provider: $it") }
                            appendLine("contact: $contact")
                            note?.let { appendLine("note: $it") }
                        }.trim(),
                    )
                }
            }

            call.respondText(
                htmlPage(
                    title = "RiverKing Account Deletion Requested",
                    body = """
                        <h1>Deletion request submitted</h1>
                        <p>Your request was recorded as #$requestId. Keep your contact available so we can verify ownership if needed.</p>
                        <p>If you still have access to the Android app, you can also delete the account immediately from the profile menu.</p>
                        <p><a href="$supportUrl">Open support links</a></p>
                    """.trimIndent(),
                ),
                ContentType.Text.Html,
            )
        }
    }
}

private fun htmlPage(
    title: String,
    body: String,
): String = """
    <!doctype html>
    <html lang="en">
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>$title</title>
        <style>
            :root {
                color-scheme: dark;
                --bg: #0d1117;
                --panel: #151b24;
                --panel-2: #1d2735;
                --text: #e8eef7;
                --muted: #9fb0c4;
                --accent: #6fd3ff;
                --danger: #ff7a7a;
                --border: rgba(159, 176, 196, 0.22);
            }
            * { box-sizing: border-box; }
            body {
                margin: 0;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                background:
                    radial-gradient(circle at top, rgba(111, 211, 255, 0.12), transparent 35%),
                    linear-gradient(180deg, #081017 0%, var(--bg) 100%);
                color: var(--text);
            }
            main {
                width: min(760px, calc(100vw - 32px));
                margin: 40px auto;
                padding: 28px;
                background: linear-gradient(180deg, rgba(29, 39, 53, 0.95), rgba(21, 27, 36, 0.98));
                border: 1px solid var(--border);
                border-radius: 24px;
                box-shadow: 0 24px 80px rgba(0, 0, 0, 0.35);
            }
            h1 { margin-top: 0; font-size: 2rem; }
            p, li { line-height: 1.6; color: var(--text); }
            .muted { color: var(--muted); }
            a { color: var(--accent); }
            ul { padding-left: 20px; }
            form { display: grid; gap: 12px; margin-top: 20px; }
            label { font-weight: 600; }
            input, select, textarea {
                width: 100%;
                border-radius: 14px;
                border: 1px solid var(--border);
                background: var(--panel);
                color: var(--text);
                padding: 12px 14px;
                font: inherit;
            }
            button {
                width: fit-content;
                border: 0;
                border-radius: 999px;
                padding: 12px 18px;
                background: linear-gradient(135deg, var(--accent), #4fa9ff);
                color: #05121a;
                font-weight: 700;
                cursor: pointer;
            }
            .danger { color: var(--danger); }
        </style>
    </head>
    <body>
        <main>
            $body
        </main>
    </body>
    </html>
""".trimIndent()
