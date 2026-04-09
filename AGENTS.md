# RiverKing Agent Guide

AI-oriented repository guide for coding assistants and code-review tools.

## Documentation Split

- `README.md` is the human-facing product overview for clients, operators, and technical reviewers.
- `docs/product-overview.md` is the deeper product note covering game loops, engagement systems, and operating model.
- `docs/github-about.md` is the source of truth for GitHub About description, topics, and website.
- `DOCUMENTATION.md` is the engineering architecture overview.
- `src/main/kotlin/**/README.md` files are the low-level code navigation layer for agents working inside a package.

## Repository Map

- `src/main/kotlin/app/`: Ktor bootstrap, API routes, sessions, Telegram auth, bot webhook, scheduler.
- `src/main/kotlin/service/`: gameplay systems, tournaments, shop, referrals, clubs, achievements, quests, payments.
- `src/main/kotlin/db/`: Exposed tables, schema creation, seed and migration helpers.
- `src/main/kotlin/util/`: metrics, RNG helpers, profanity filtering, text sanitization, coin math.
- `src/main/resources/webapp/`: Telegram Mini App frontend and shipped game assets.
- `mobile/android-app/`: nested Android project with its own Gradle setup, mobile auth flows, and shared-backend client shell.
- `src/test/kotlin/`: service and route tests.
- `docs/`: product-facing repository materials and showcase assets.

## Key Runtime Facts

- Runtime stack: Kotlin + Ktor + Netty.
- Persistence: Exposed over SQLite by default, with PostgreSQL-style connection config fields also supported.
- Mini App auth: `POST /api/auth/telegram` verifies Telegram `initData` before creating an app session.
- Mobile auth: password and Google auth create bearer tokens plus refresh sessions for the Android client.
- Play Billing: `POST /api/shop/{id}/play/complete` is expected to verify Google Play purchase tokens before granting Android `play` entitlements.
- Product surfaces: Telegram Mini App frontend, Telegram bot commands/admin flows, and a nested Android client project.
- Scheduler: background jobs handle auto-fishing, stuck-cast cleanup, and prize/reward distribution.
- Observability: `/metrics` exposes Prometheus-style output from `Metrics.kt` and `UserMetrics.kt`.
- Analytics: TG Analytics can be enabled through `TG_ANALYTICS_*` config values.

## First Pass For Any Agent

1. Read [README.md](README.md) for product positioning and public claims.
2. Read [AGENTS.md](AGENTS.md) and [DOCUMENTATION.md](DOCUMENTATION.md) for repo structure and runtime boundaries.
3. Read the package README closest to the area you are about to change:
   - [src/main/kotlin/app/README.md](src/main/kotlin/app/README.md)
   - [src/main/kotlin/service/README.md](src/main/kotlin/service/README.md)
   - [src/main/kotlin/db/README.md](src/main/kotlin/db/README.md)
   - [src/main/kotlin/util/README.md](src/main/kotlin/util/README.md)
4. If the task touches Android, inspect `mobile/android-app/README.md` plus the relevant files under `mobile/android-app/app/src/main/`.
4. If the task touches frontend behavior, inspect both `src/main/resources/webapp/scripts/app.jsx` and the relevant `tabs/*.js` file.
5. If the task touches bot behavior, inspect both `BotRoutes.kt` and `TelegramBot.kt`.

## Common Change Paths

### Webapp UI

You usually need to touch:

- `src/main/resources/webapp/scripts/app.jsx`
- one or more files under `src/main/resources/webapp/scripts/tabs/`
- shared components in `src/main/resources/webapp/scripts/components.js`

### API or session flow

You usually need to touch:

- `src/main/kotlin/app/ApiRoutes.kt`
- `src/main/kotlin/app/Sessions.kt`
- `src/main/kotlin/app/TgWebAppAuth.kt`
- `src/main/kotlin/app/AuthService.kt`
- a service under `src/main/kotlin/service/`

### Android client

You usually need to touch:

- `mobile/android-app/app/src/main/java/com/riverking/mobile/ui/`
- `mobile/android-app/app/src/main/java/com/riverking/mobile/auth/`
- `mobile/android-app/app/build.gradle.kts`
- `mobile/android-app/README.md`

### Game systems

You usually need to touch:

- `src/main/kotlin/service/FishingService.kt`
- a specialized service such as `TournamentService.kt`, `QuestService.kt`, `ClubService.kt`, `AchievementService.kt`, or `ReferralService.kt`
- possibly `src/main/kotlin/db/Tables.kt`

### Bot or admin flows

You usually need to touch:

- `src/main/kotlin/app/BotRoutes.kt`
- `src/main/kotlin/app/TelegramBot.kt`
- relevant services and DB tables for persistence

### Persistence or schema

Read first:

- `src/main/kotlin/db/Tables.kt`
- [src/main/kotlin/db/README.md](src/main/kotlin/db/README.md)

Seed data and schema compatibility matter because the Mini App and bot both depend on the same tables and seeded content.

## Update Rules

- If you change the public product promise, update `README.md`.
- If you change repository positioning for GitHub/About, update `docs/github-about.md`.
- If you change the product scope or operating model, update `docs/product-overview.md`.
- If you change package structure or runtime surfaces, update `AGENTS.md` and the nearest package README.
- Commit important implementation milestones as separate commits so the history clearly shows the progression of the work. Avoid batching multiple major steps into one opaque commit.
- Keep public claims honest: do not describe flows, admin tooling, or analytics that the code does not actually support.
- Treat `docs/screenshots/` as showcase assets. Refresh them when the visible product surface changes in a meaningful way.
