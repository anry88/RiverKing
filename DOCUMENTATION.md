# RiverKing architecture

This document explains how the RiverKing backend and adjacent client projects are organized and where to find more details. The structure mirrors the source tree so you can jump from directories to component and function descriptions.

- [Ktor application (`src/main/kotlin/app`)](src/main/kotlin/app/README.md)
- [Domain services (`src/main/kotlin/service`)](src/main/kotlin/service/README.md)
- [Data layer (`src/main/kotlin/db`)](src/main/kotlin/db/README.md)
- [Utilities (`src/main/kotlin/util`)](src/main/kotlin/util/README.md)

## Overview

The server starts from `main()` in `Application.kt`: it loads the configuration, configures Ktor plugins, initializes the database, and registers HTTP routes for both the API and the Telegram bot. The `resources/webapp` directory contains the static mini‑app client that is served by the same server.

`mobile/android-app/` is a separate nested Gradle project. It does not participate in the backend build graph, but it consumes the same backend contracts and account model through mobile auth endpoints and the shared gameplay API.

Core business logic lives in the services (`service/`), which talk to the database through Exposed (`db/`) and provide game operations (fishing, tournaments, shop, referrals). Routes in `app/ApiRoutes.kt` rely on these services and serializers to build responses for the Mini App, Android client, or bot. The auth/session layer now supports both Telegram cookie sessions and bearer tokens backed by refresh sessions.

Achievements are tracked through `AchievementService` and exposed via `/api/achievements` plus the `/achievements` bot command. The current catalog includes rarity ladders, traveler and trophy milestones, tournament and daily-rating progress, koi collection, and location-completion achievements with claimable rewards surfaced in both the mini-app and the bot.

In the mini‑app client (`resources/webapp`), the Guide tab now contains an Achievements sub‑tab that shows tier art, localized descriptions, progress (e.g., `3/16`), and claim buttons for unlocked rewards. A red badge on the Guide navigation item highlights unclaimed achievement prizes.

Daily and weekly personal quests are handled by `QuestService`, surfaced in the mini‑app through the Fishing tab quest drawer, and exposed via `/api/quests` plus the `/quests` bot command. The personal quest pool is now filtered by fish that are actually available through the player's unlocked locations, so fish-specific quests do not appear before the relevant fish can be caught. `/api/quests` now also includes a `club` section so the Mini App, bot, and Android client can render a third quest block below weekly quests.

Fishing clubs are managed by `ClubService`, which records membership, validates creation requirements, tracks weekly contributions from daily rating rewards, and issues weekly club coin payouts. Shared weekly club quests are handled by `ClubQuestService`, which assigns two quests per club per week, stores pooled club progress in dedicated tables, and splits completion rewards evenly across the club roster at the moment of completion. The mini‑app surfaces the club panel from the Fishing tab, while the quest drawer, `/quests` bot command, and Android quest sheet now all show a dedicated club-quest section or a localized "join a club" CTA when the player is not in a club.

To understand how a specific function works, open the README for the relevant package and follow the links to the detailed files.
