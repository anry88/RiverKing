# RiverKing Agent Guide

AI-oriented repository guide for coding assistants and code-review tools.

## Documentation Split

- `README.md` is the human-facing product overview for clients, operators, and technical reviewers.
- `docs/product-overview.md` is the deeper product note covering game loops, engagement systems, and operating model.
- `docs/github-about.md` is the source of truth for GitHub About description, topics, and website.
- `docs/android-release.md` is the Android distribution and compliance checklist for the itch.io-first / Google Play-second rollout.
- `DOCUMENTATION.md` is the engineering architecture overview.
- `mobile/admin-app/README.md` is the internal Android admin app setup and scope note.
- `src/main/kotlin/**/README.md` files are the low-level code navigation layer for agents working inside a package.

## Repository Map

- `src/main/kotlin/app/`: Ktor bootstrap, API routes, sessions, Telegram auth, bot webhook, scheduler.
- `src/main/kotlin/service/`: gameplay systems, tournaments, special events, shop, referrals, clubs, achievements, quests, payments.
- `src/main/kotlin/db/`: Exposed tables, schema creation, seed and migration helpers.
- `src/main/kotlin/util/`: metrics, RNG helpers, profanity filtering, text sanitization, coin math.
- `src/main/resources/webapp/`: Telegram Mini App frontend and shipped game assets.
- `mobile/android-app/`: nested Android project with its own Gradle setup, mobile auth flows, and shared-backend client shell.
- `mobile/admin-app/`: nested internal Android admin project with saved server profiles, admin API access, tournaments, special events, discounts, and broadcasts.
- `src/test/kotlin/`: service and route tests.
- `docs/`: product-facing repository materials and showcase assets.

## Key Runtime Facts

- Runtime stack: Kotlin + Ktor + Netty.
- Persistence: Exposed over SQLite by default, with PostgreSQL-style connection config fields also supported.
- Mini App auth: `POST /api/auth/telegram` verifies Telegram `initData` before creating an app session.
- Mobile auth: password, Google, and Telegram sign-in can create bearer tokens plus refresh sessions for the Android client, and an authenticated Android profile can link a Telegram account for shared progression across mobile and Telegram surfaces.
- Account deletion: `POST /api/account/delete` deletes the authenticated profile, and the backend also serves public compliance pages at `/privacy`, `/terms`, `/support`, and `/account/delete`.
- Play Billing: `POST /api/shop/{id}/play/complete` is expected to verify Google Play purchase tokens before granting Android `play` entitlements.
- Product surfaces: Telegram Mini App frontend, Telegram bot commands/admin flows, a nested Android player client project, and an internal Android admin app.
- Fishing contract: `/api/hook` reveals the hooked fish and returns `challenge.tapGoal`, `challenge.durationMs`, and `challenge.struggleIntensity`; Telegram Mini App and Android both use the always-on immersive fishing scene rather than a user-selectable Pro mode.
- Admin API: `mobile/admin-app` calls protected `/api/admin/*` endpoints with `ADMIN_API_TOKEN`; `/api/admin/catalog` supplies selectable metrics, fish, event fish, locations, tournament/event prizes, and discountable shop items for admin forms.
- Special events: `/api/events/current`, `/api/events/previous`, and `/api/events/{id}` expose Mini App/Android-only club events with temporary event locations, club total-weight/count leaderboards, personal top-fish leaderboards, and `EVENT` prizes. Bot flows do not expose special events.
- Android distribution contract: `direct` and `play` share one canonical `applicationId`, one monotonic `versionCode` line, and should be signed with the same release key so users can move from itch.io APK installs to Google Play without reinstalling.
- Unified mobile versioning: both `mobile/android-app/version.properties` and `mobile/admin-app/version.properties` track the same `RIVERKING_VERSION_CODE` / `RIVERKING_VERSION_NAME` pair. The backend version guard (`426 Upgrade Required` via `minSupportedVersionCode` and `requireVersionHeaders`) applies to both player and admin API routes, so both apps must stay in sync with the update policy.
- Android update policy: the backend serves `GET /api/mobile/update` from `src/main/resources/android-update-policy.json`, and all Android API calls — both player and admin — receive `426 Upgrade Required` when `minSupportedVersionCode` or `requireVersionHeaders` makes the installed build unusable.
- Scheduler: background jobs handle auto-fishing, stuck-cast cleanup, and prize/reward distribution.
- Quests: `GET /api/quests` now returns personal `daily` / `weekly` lists plus a `club` section; club quests are shared across the whole club and are rendered in the bot, Mini App, and Android client.
- Club chat: `/api/club/chat` stores messages and system events in `ClubChatMessages` and is rendered by the Mini App and Android club screens.
- Android Assets: the mobile project bundles core gameplay assets locally as **WebP** files (converted from PNGs via `mobile/android-app/convert_assets.py`) to reduce traffic.
- Observability: `/metrics` exposes Prometheus-style output from `Metrics.kt` and `UserMetrics.kt`.
- Analytics: TG Analytics can be enabled through `TG_ANALYTICS_*` config values.
- GitHub release automation creates or updates a draft `develop -> main` Android release PR after pushes to `develop`, validates the Android update policy, and GitHub Release notes are label-driven through `.github/release.yml`; use `android-force-update` when a release raises `minSupportedVersionCode`.

## First Pass For Any Agent

1. Read [README.md](README.md) for product positioning and public claims.
2. Read [AGENTS.md](AGENTS.md) and [DOCUMENTATION.md](DOCUMENTATION.md) for repo structure and runtime boundaries.
3. Read the package README closest to the area you are about to change:
   - [src/main/kotlin/app/README.md](src/main/kotlin/app/README.md)
   - [src/main/kotlin/service/README.md](src/main/kotlin/service/README.md)
   - [src/main/kotlin/db/README.md](src/main/kotlin/db/README.md)
   - [src/main/kotlin/util/README.md](src/main/kotlin/util/README.md)
4. If the task touches the Android player client, inspect `mobile/android-app/README.md` plus the relevant files under `mobile/android-app/app/src/main/`.
5. If the task touches the internal Android admin app, inspect `mobile/admin-app/README.md` plus the relevant files under `mobile/admin-app/app/src/main/`.
6. If the task touches Android release/distribution, also inspect `docs/android-release.md`.
7. If the task touches frontend behavior, inspect both `src/main/resources/webapp/scripts/app.jsx` and the relevant `tabs/*.js` file.
8. If the task touches bot behavior, inspect both `BotRoutes.kt` and `TelegramBot.kt`.

## Token Discipline

- Optimize token usage aggressively: think first, then act.
- Prefer the minimum necessary reads, searches, edits, and tool calls that can confidently solve the task.
- Avoid speculative rewrites, repetitive retries, and exploratory changes when the next correct step can be reasoned out in advance.

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

Whenever you change a server API contract, treat compatibility verification as mandatory for all client surfaces:

- Telegram bot commands and callbacks
- Telegram Mini App frontend
- Android mobile client

Prefer backward-compatible additions to JSON over renaming or repurposing existing fields, and verify every affected client path before finishing the change.

### Android client

You usually need to touch:

- `mobile/android-app/app/src/main/java/com/riverking/mobile/ui/`
- `mobile/android-app/app/src/main/java/com/riverking/mobile/auth/`
- `mobile/android-app/app/build.gradle.kts`
- `mobile/android-app/README.md`

### Android admin app

You usually need to touch:

- `mobile/admin-app/app/src/main/java/com/riverking/admin/ui/`
- `mobile/admin-app/app/src/main/java/com/riverking/admin/network/AdminApiClient.kt`
- `src/main/kotlin/app/AdminApiRoutes.kt`
- `mobile/admin-app/README.md`

### Game systems

You usually need to touch:

- `src/main/kotlin/service/FishingService.kt`
- a specialized service such as `TournamentService.kt`, `QuestService.kt`, `ClubQuestService.kt`, `ClubService.kt`, `AchievementService.kt`, or `ReferralService.kt`
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
- If you change a server API contract, verify compatibility across Telegram bot flows, the Telegram Mini App, and the Android client before closing the task. Prefer additive response changes over breaking field changes.
- If you change backend or Android code that can affect the Android client, explicitly check whether the new code remains compatible with the previous released Android version. If the old Android client cannot safely keep using the backend, update `src/main/resources/android-update-policy.json` in the same block of work: raise `minSupportedVersionCode`, set `requireVersionHeaders` when legacy clients without version headers must also be blocked, and write player-facing release notes. Do this immediately rather than leaving the decision to a later human release step.
- When `minSupportedVersionCode` or `latestVersionCode` is raised in `android-update-policy.json`, update **both** `mobile/android-app/version.properties` and `mobile/admin-app/version.properties` to match. Both mobile apps share one monotonic version line and are subject to the same backend version guard.
- If you change GitHub release automation or PR labeling rules, update `docs/android-release.md`, the relevant `.github/workflows/*.yml`, and `.github/release.yml` / `.github/labeler.yml` as needed so the documented release flow stays current.
- After each completed block of work, create a separate commit if there are changes to save so the history remains complete and incremental. Do not batch multiple finished tasks into one opaque commit.
- Keep public claims honest: do not describe flows, admin tooling, or analytics that the code does not actually support.
- Treat `docs/screenshots/` as showcase assets. Refresh them when the visible product surface changes in a meaningful way.
