# `app` package

This directory contains the entry points and Ktor HTTP routes. Use the file list below to understand request flow quickly.

## Server and plugins
- **`Application.kt`** — the `main()` function loads `Env`, boots the Netty server, installs plugins (`ContentNegotiation`, `CORS`, `DoubleReceive`, sessions), initializes the database (`DB.init`), and registers routes through `apiRoutes` and `botRoutes`. It also exposes `/metrics` and `/health`, restores lost lures via `FishingService.restoreCastingLuresOnStartup()`, and schedules background jobs via `Scheduler.install()`.
- **`Env.kt`** — reads `config.properties` and builds the environment object: bot token, public URL, port, development mode, and more.
- **`Sessions.kt`** — configures the cookie session `AppSession` for the mini‑app, now keyed by shared `userId` rather than Telegram ID; `installSessions` installs the Ktor plugin.

## Routes and integrations
- **`ApiRoutes.kt`** — `Application.apiRoutes(env)` registers all REST endpoints for the mini‑app. Notable groups:
  - Authentication: `POST /api/auth/telegram` accepts `initData`, verifies the signature with `TgWebAppAuth.verifyAndExtractUser`, and stores `AppSession(userId)`. Mobile auth is handled by `POST /api/auth/google`, `POST /api/auth/password/register`, `POST /api/auth/password/login`, `POST /api/auth/refresh`, and `POST /api/auth/logout`.
  - Account lifecycle: `POST /api/account/delete` deletes the authenticated shared RiverKing profile together with linked sessions and user-owned progression data.
  - Telegram/mobile account linking: `POST /api/auth/telegram/mobile/start` plus `GET /api/auth/telegram/mobile/status/{token}` drive Telegram sign-in from Android, while `POST /api/auth/telegram/link/start` plus `GET /api/auth/telegram/link/status/{token}` let an authenticated mobile player link a Telegram account to the same `Users.id`.
  - Profile and progress: `GET /api/me` collects player state (lures, rods, locations, recent catches) via `FishingService` and `I18n`, and now serves both mini‑app sessions and bearer-token mobile clients.
  - Fishing: `POST /api/start-cast`, `POST /api/hook`, and `POST /api/cast` drive the staged cast→hook→catch lifecycle using `FishingService.startCast`, `hook`, and `catch`. `GET /api/catches/{id}` and `GET /api/catches/{id}/card` provide catch details and share-card payloads for the Mini App and Android client.
  - Daily rewards and shop: `POST /api/daily`, `GET/POST /api/shop` call `FishingService.dailyReward`, `buyPackage`, and related payments (`StarsPaymentService`, `PayService`).
  - Play Billing: `POST /api/shop/{id}/play/complete` verifies Android purchase tokens through the Google Play Developer API before delivering the entitlement and recording the payment.
  - Tournaments: `/api/tournament*` endpoints rely on `TournamentService` and `RatingPrizeService` to serve leaderboards and process rewards.
  - Quests: `GET /api/quests` now returns additive `daily`, `weekly`, and `club` sections so all three clients can render personal and club quest blocks from one payload.
  - Clubs: `/api/club`, `/api/club/create`, `/api/club/search`, `/api/club/{id}/join` expose fishing club membership, creation, and discovery.
  - Referrals: `GET/POST /api/referrals` and `POST /api/referrals/apply` handle links via `ReferralService`.
  - Utility calls: `GET /api/assets/{path...}` serves webapp assets with manual path checks; `GET /api/metrics` updates `UserMetrics`.
- API compatibility rule: any response-shape change in this package must be checked against all three consumers that share these routes — Telegram bot flows, the Telegram Mini App, and the Android client. Prefer additive fields over breaking renames or changed semantics.
- **`BotRoutes.kt`** — registers the Telegram bot webhook. Processes updates, sends responses through `TelegramBot`, and calls game services to handle bot commands, including the three-section `/quests` output with club quests.
- **`TelegramBot.kt`** — thin client over the Telegram Bot API for sending messages, invoices, and alerts; used by the webhook and by the redirect from `/`.
- **`TelegramModels.kt`** — DTOs for Telegram WebApp/bot (keyboards, invoices, updates).
- **`PublicPages.kt`** — serves public support/legal/account-deletion pages used by Android release surfaces and Google Play policy links.
- **`AuthService.kt`** — provider-neutral account service for password auth, Google sign-in, refresh-session rotation, and bearer token resolution.
- **`TelegramLinkService.kt`** — issues short-lived Telegram confirmation sessions for Android sign-in/linking, resolves `/start login_*` and `/start link_*` confirmations, and prevents accidental account duplication when Telegram/mobile identities are attached.
- **`AuthTokenCodec.kt`** — issues and validates signed access tokens plus opaque refresh tokens.
- **`TgWebAppAuth.kt`** — verifies `initData` signatures and extracts `TelegramUser` for WebApp authentication.

## Background jobs and presentation
- **`Scheduler.kt`** — configures the Ktor scheduler with periodic tasks: auto‑fishing (`FishingService.autoFishUsers`), clearing stuck casts, issuing tournament/daily rating/club weekly rewards via `PrizeService`.
- **`CatchPresentation.kt`** — builds human‑readable texts and DTOs for catch results so a single source serves both the API and the bot.

### Function navigation
For detailed behavior of individual handlers, see comments and calls inside `ApiRoutes.kt` — all routes are grouped in one place, which makes it easy to search by endpoint name or service function.
