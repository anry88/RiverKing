# `app` package

This directory contains the entry points and Ktor HTTP routes. Use the file list below to understand request flow quickly.

## Server and plugins
- **`Application.kt`** — the `main()` function loads `Env`, boots the Netty server, installs plugins (`ContentNegotiation`, `CORS`, `DoubleReceive`, sessions), initializes the database (`DB.init`), and registers routes through `apiRoutes` and `botRoutes`. It also exposes `/metrics` and `/health`, restores lost lures via `FishingService.restoreCastingLuresOnStartup()`, and schedules background jobs via `Scheduler.install()`.
- **`Env.kt`** — reads `config.properties` and builds the environment object: bot token, public URL, port, development mode, and more.
- **`Sessions.kt`** — configures cookie sessions `AppSession` for the mini‑app and `BotSession` for bot webhooks; `installSessions` installs the Ktor plugin.

## Routes and integrations
- **`ApiRoutes.kt`** — `Application.apiRoutes(env)` registers all REST endpoints for the mini‑app. Notable groups:
  - Authentication: `POST /api/auth/telegram` accepts `initData`, verifies the signature with `TgWebAppAuth.verifyAndExtractUser`, and stores `AppSession`.
  - Profile and progress: `GET /api/me` collects player state (lures, rods, locations, recent catches) via `FishingService` and `I18n`.
  - Fishing: `POST /api/cast`, `POST /api/hook`, `POST /api/catch` drive the cast→hook→catch lifecycle using `FishingService.startCast`, `hook`, and `catch`.
  - Daily rewards and shop: `POST /api/daily`, `GET/POST /api/shop` call `FishingService.dailyReward`, `buyPackage`, and related payments (`StarsPaymentService`, `PayService`).
  - Tournaments: `/api/tournament*` endpoints rely on `TournamentService` and `RatingPrizeService` to serve leaderboards and process rewards.
  - Clubs: `/api/club`, `/api/club/create`, `/api/club/search`, `/api/club/{id}/join` expose fishing club membership, creation, and discovery.
  - Referrals: `GET/POST /api/referrals` and `POST /api/referrals/apply` handle links via `ReferralService`.
  - Utility calls: `GET /api/assets/{path...}` serves webapp assets with manual path checks; `GET /api/metrics` updates `UserMetrics`.
- **`BotRoutes.kt`** — registers the Telegram bot webhook. Processes updates, sends responses through `TelegramBot`, and calls game services to handle bot commands.
- **`TelegramBot.kt`** — thin client over the Telegram Bot API for sending messages, invoices, and alerts; used by the webhook and by the redirect from `/`.
- **`TelegramModels.kt`** — DTOs for Telegram WebApp/bot (keyboards, invoices, updates).
- **`TgWebAppAuth.kt`** — verifies `initData` signatures and extracts `TelegramUser` for WebApp authentication.

## Background jobs and presentation
- **`Scheduler.kt`** — configures the Ktor scheduler with periodic tasks: auto‑fishing (`FishingService.autoFishUsers`), clearing stuck casts, issuing tournament/daily rating/club weekly rewards via `PrizeService`.
- **`CatchPresentation.kt`** — builds human‑readable texts and DTOs for catch results so a single source serves both the API and the bot.

### Function navigation
For detailed behavior of individual handlers, see comments and calls inside `ApiRoutes.kt` — all routes are grouped in one place, which makes it easy to search by endpoint name or service function.
