# RiverKing

RiverKing is a Telegram-first fishing game with a Kotlin/Ktor backend, a shipped Telegram Mini App frontend, Telegram bot flows, progression systems, tournaments, personal and club quests, clubs, referrals, and Stars-based monetization. The repository now also includes an Android client subtree that reuses the same backend and player data model.

It is built as a working product rather than a thin game prototype: the repository already includes session-authenticated Mini App flows, real gameplay systems, persistent progression, background jobs, operational metrics, moderation rules, and admin-side bot tooling.

**What it does**

- Delivers a Telegram Mini App fishing experience with cast, hook, and catch gameplay.
- Tracks progression across locations, rods, lures, fish discovery, achievements, quests, tournaments, and clubs.
- Ships daily and weekly personal quests plus weekly club quests with pooled progress and split coin rewards for current club members.
- Connects the game backend to Telegram bot commands, referral flows, Stars payments, coin purchases, auto-casting, and operational metrics.
- Includes an Android nested project under `mobile/android-app` with shared-backend auth, `play`/`direct` flavors, real Google Play Billing for the `play` flavor, and a parity-focused mobile shell.

**Why it is technically interesting**

- Hybrid Telegram product surface: Mini App frontend plus bot commands and admin flows in one codebase.
- Shared identity foundation: Telegram cookie sessions for the Mini App plus bearer-token mobile auth with Telegram sign-in/linking against the same gameplay API.
- Product-minded backend, not only a game loop: progression, retention systems, economy, moderation, payments, analytics, and scheduling.
- Clear Kotlin layers for Ktor routes, gameplay services, Exposed persistence, shipped frontend assets, and an isolated Android project.

**Stack**

`Kotlin` `Ktor` `Netty` `Exposed` `SQLite` `Telegram Mini App` `Telegram Bot API` `Android` `Jetpack Compose` `TG Analytics` `Gradle`

**Quick links**

- Mini App: [t.me/river_king_bot/app](https://t.me/river_king_bot/app)
- Bot: [t.me/river_king_bot](https://t.me/river_king_bot)
- Product overview: [docs/product-overview.md](docs/product-overview.md)
- Android release guide: [docs/android-release.md](docs/android-release.md)
- Architecture: [DOCUMENTATION.md](DOCUMENTATION.md)
- Agent guide: [AGENTS.md](AGENTS.md)

| Fishing | Tournaments |
| --- | --- |
| ![RiverKing fishing screen](docs/screenshots/01-fishing-home.png) | ![RiverKing tournament screen](docs/screenshots/03-tournaments-current.png) |
| Achievements | Clubs |
| ![RiverKing achievements screen](docs/screenshots/02-guide-achievements.png) | ![RiverKing club screen](docs/screenshots/04-club-overview.png) |

Human-facing repository docs live in this file and in [docs/product-overview.md](docs/product-overview.md). AI-oriented repository context lives in [AGENTS.md](AGENTS.md), [DOCUMENTATION.md](DOCUMENTATION.md), and the package READMEs under `src/main/kotlin/**`.

## What I Built

- A Telegram Mini App with a fishing-first gameplay loop and a shipped asset-driven frontend.
- Ktor API routes for Telegram auth, profile state, fishing actions, guide data, ratings, tournaments, clubs, quests, shop, referrals, and prizes.
- Provider-neutral auth tables and routes for Telegram, Google sign-in, password auth, refresh sessions, and shared bearer access.
- Product systems for progression, achievements, quests, tournament prize logic, club competition, referral rewards, and in-game economy.
- Telegram bot integrations for commands, auto-casting, prize flows, payment-support flows, and admin operations.
- Exposed-backed persistence, startup restoration logic, background schedulers, TG Analytics integration, and Prometheus-style metrics.
- A nested Android project with its own Gradle setup, Telegram/password/Google auth flows, nickname gate, shared-API main shell, and real Google Play Billing verification for the `play` flavor.
- Public Android-compliance surfaces for privacy, support, terms, and account deletion, plus an authenticated account deletion endpoint for mobile users.

## Architecture

```mermaid
flowchart LR
    A["Telegram client"] --> B["Mini App frontend"]
    A --> C["Telegram bot / admin flows"]
    I["Android app"] --> D
    B --> D["Ktor API"]
    C --> D
    D --> E["Gameplay services"]
    D --> J["Account / session layer"]
    E --> F["Exposed + SQLite"]
    D --> G["Metrics / analytics"]
    E --> H["Schedulers / reward distribution"]
```

- `src/main/kotlin/app/` owns bootstrap, sessions, Telegram auth, HTTP routes, bot webhook handling, and schedulers.
- `src/main/kotlin/service/` contains gameplay systems such as fishing, tournaments, quests, clubs, referrals, achievements, and shop/payment logic.
- `src/main/kotlin/db/` defines tables, schema creation, seed data, and data migrations.
- `src/main/resources/webapp/` contains the shipped Mini App frontend and visual assets.
- `mobile/android-app/` contains the nested Android client project and its separate Gradle build.

## Core Systems

- `Fishing loop`: cast -> hook -> catch with timing, catch presentation, and recent catch history.
- `Progression`: locations, rods, lures, fish discovery, unlocks, and recommendation logic.
- `Retention`: daily rewards, location-aware quest pools, achievements, daily ratings, and tournament participation.
- `Social loops`: clubs, member roles, weekly contribution boards, shared club quests, and club chat feed.
- `Economy`: Stars purchases, coin purchases, referral rewards, and prize distribution.
- `Operations`: profanity filtering, metrics, TG Analytics hooks, startup recovery, and scheduled background jobs.
- `Bot automation`: command flows, auto-casting, admin tournament tooling, and payment-support operations.

## Documentation Map

- [docs/product-overview.md](docs/product-overview.md): product-facing overview of game loops, economy, and operating model.
- [DOCUMENTATION.md](DOCUMENTATION.md): engineering architecture and package map.
- [AGENTS.md](AGENTS.md): repo-level guide for coding agents and review tools.
- Package READMEs:
  - [src/main/kotlin/app/README.md](src/main/kotlin/app/README.md)
  - [src/main/kotlin/service/README.md](src/main/kotlin/service/README.md)
  - [src/main/kotlin/db/README.md](src/main/kotlin/db/README.md)
  - [src/main/kotlin/util/README.md](src/main/kotlin/util/README.md)

## Demo / Screenshots

### Fishing home

![RiverKing fishing home](docs/screenshots/01-fishing-home.png)

### Guide / achievements

![RiverKing achievements](docs/screenshots/02-guide-achievements.png)

### Current tournament

![RiverKing tournaments](docs/screenshots/03-tournaments-current.png)

### Club overview

![RiverKing club overview](docs/screenshots/04-club-overview.png)

### Shop and daily reward

![RiverKing shop and daily reward](docs/screenshots/05-shop-and-daily.png)

### Ratings

![RiverKing ratings](docs/screenshots/06-ratings.png)

### Bot flow

![RiverKing bot flow](docs/screenshots/07-bot-flow.png)

Admin-side tooling exists in the codebase and bot flows, but it is intentionally excluded from the public screenshot set.

## Setup

### Prerequisites

- JDK 17+
- Telegram bot token for real Telegram integration
- A writable SQLite path or another configured database target
- Android SDK, if you want to build the nested Android client

### Quick start

1. Copy the example config into the runtime location:

   ```bash
   cp config.example.properties src/main/resources/config.properties
   ```

2. For local development, at minimum keep these values:

   ```properties
   BOT_TOKEN=TEST
   BOT_NAME=river_king_bot
   PUBLIC_BASE_URL=http://localhost:8080
   DEV_MODE=true
   ```

3. Start the app:

   ```bash
   ./gradlew run
   ```

4. Open the Mini App locally:

   [http://localhost:8080/app](http://localhost:8080/app)

With `DEV_MODE=true`, the Mini App can boot without a real Telegram session and the API falls back to the local development user.
With `DEV_MODE=false`, opening `/app` in a regular browser keeps the Mini App blocked until valid Telegram `initData` is present.

To work on the Android client, use the nested project described in [mobile/android-app/README.md](mobile/android-app/README.md).

## Configuration

Use [config.example.properties](config.example.properties) as the starting point. Current keys used by the app:

- `BOT_TOKEN`
- `BOT_NAME`
- `PUBLIC_BASE_URL`
- `DATABASE_URL`
- `DATABASE_USER`
- `DATABASE_PASSWORD`
- `PORT`
- `DEV_MODE`
- `ADMIN_TG_ID`
- `PROVIDER_TOKEN`
- `TELEGRAM_WEBHOOK_SECRET`
- `AUTH_TOKEN_SECRET`
- `AUTH_ACCESS_TOKEN_TTL_MINUTES`
- `AUTH_REFRESH_TOKEN_TTL_DAYS`
- `GOOGLE_AUTH_CLIENT_ID`
- `GOOGLE_PLAY_PACKAGE_NAME`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_FILE`
- `TG_ANALYTICS_TOKEN`
- `TG_ANALYTICS_SCRIPT_URL`
- `TG_ANALYTICS_APP_NAME`

## Checks

Build the application classes:

```bash
./gradlew classes
```

Run tests:

```bash
./gradlew test
```
