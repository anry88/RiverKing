# RiverKing architecture

This document explains how the RiverKing backend and adjacent client projects are organized and where to find more details. The structure mirrors the source tree so you can jump from directories to component and function descriptions.

- [Ktor application (`src/main/kotlin/app`)](src/main/kotlin/app/README.md)
- [Domain services (`src/main/kotlin/service`)](src/main/kotlin/service/README.md)
- [Data layer (`src/main/kotlin/db`)](src/main/kotlin/db/README.md)
- [Utilities (`src/main/kotlin/util`)](src/main/kotlin/util/README.md)
- [Android player client (`mobile/android-app`)](mobile/android-app/README.md)
- [Android admin app (`mobile/admin-app`)](mobile/admin-app/README.md)

## Overview

The server starts from `main()` in `Application.kt`: it loads the configuration, configures Ktor plugins, initializes the database, and registers HTTP routes for both the API and the Telegram bot. The `resources/webapp` directory contains the static mini‑app client that is served by the same server.

`mobile/android-app/` is a separate nested Gradle project. It does not participate in the backend build graph, but it consumes the same backend contracts and account model through mobile auth endpoints, the shared gameplay API, and the Android update policy served at `/api/mobile/update`.

`mobile/admin-app/` is a separate internal Gradle project for operators. It stores one or more backend server profiles locally and calls the protected `/api/admin/*` routes with `ADMIN_API_TOKEN` to manage tournaments, special events, shop discounts, and broadcasts. The admin app uses `/api/admin/catalog` to render selectable tournament metrics, fish, event fish, locations, prizes, and discountable shop items instead of relying on hand-entered IDs.

Core business logic lives in the services (`service/`), which talk to the database through Exposed (`db/`) and provide game operations (fishing, regular tournaments, special club events, shop, referrals). Routes in `app/ApiRoutes.kt` rely on these services and serializers to build responses for the Mini App, Android client, or bot. Protected operator routes live in `app/AdminApiRoutes.kt`. The auth/session layer now supports both Telegram cookie sessions and bearer tokens backed by refresh sessions.

Fishing is served as one immersive staged flow across the Mini App and Android client. `/api/start-cast` starts the cast, `/api/hook` now reveals the hooked fish plus a backend-computed tap challenge (`tapGoal`, `durationMs`, and `struggleIntensity`), and `/api/cast` finalizes the catch or escape using the existing success flag. The client scenes use the same challenge values to show fish rarity/weight and to scale the post-hook bobber struggle.

Achievements are tracked through `AchievementService` and exposed via `/api/achievements` plus the `/achievements` bot command. The current catalog includes rarity ladders, traveler and trophy milestones, tournament and daily-rating progress, koi collection, and location-completion achievements with claimable rewards surfaced in both the mini-app and the bot.

In the mini‑app client (`resources/webapp`), the Guide tab now contains an Achievements sub‑tab that shows tier art, localized descriptions, progress (e.g., `3/16`), and claim buttons for unlocked rewards. A red badge on the Guide navigation item highlights unclaimed achievement prizes.

Daily and weekly personal quests are handled by `QuestService`, surfaced in the mini‑app through the Fishing tab quest drawer, and exposed via `/api/quests` plus the `/quests` bot command. The personal quest pool is now filtered by fish that are actually available through the player's unlocked locations, so fish-specific quests do not appear before the relevant fish can be caught. `/api/quests` now also includes a `club` section so the Mini App, bot, and Android client can render a third quest block below weekly quests.

Fishing clubs are managed by `ClubService`, which records membership, validates creation requirements, tracks weekly contributions from daily rating rewards, subtracts active special-event progress when members leave or are kicked, and issues weekly club coin payouts. Shared weekly club quests are handled by `ClubQuestService`, which assigns two quests per club per week, stores pooled club progress in dedicated tables, persists per-member quest contribution rows, and splits completion rewards evenly across the club roster at the moment of completion. Special club events are handled by `SpecialEventService`, which adds temporary event locations for Mini App and Android players, aggregates club total-weight/count leaderboards, keeps personal top-fish history, and distributes event prizes after completion. Club chat uses `/api/club/chat` and the `ClubChatMessages` table for member messages and system events such as joins, role changes, rating prizes, and rare catches. The club page now exposes weekly ratings, event tournament views, weekly quest views, and chat in both the Mini App and Android client, with current/previous switching and a concrete quest selector that reveals each member's contribution for that quest. The mini‑app surfaces the club panel from the Fishing tab, while the quest drawer, `/quests` bot command, and Android quest sheet now all show a dedicated club-quest section or a localized "join a club" CTA when the player is not in a club.

To understand how a specific function works, open the README for the relevant package and follow the links to the detailed files.
