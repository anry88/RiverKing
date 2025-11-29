# RiverKing architecture

This document explains how the RiverKing mini‑app server is organized and where to find more details. The structure mirrors the source tree so you can jump from directories to component and function descriptions.

- [Ktor application (`src/main/kotlin/app`)](src/main/kotlin/app/README.md)
- [Domain services (`src/main/kotlin/service`)](src/main/kotlin/service/README.md)
- [Data layer (`src/main/kotlin/db`)](src/main/kotlin/db/README.md)
- [Utilities (`src/main/kotlin/util`)](src/main/kotlin/util/README.md)

## Overview

The server starts from `main()` in `Application.kt`: it loads the configuration, configures Ktor plugins, initializes the database, and registers HTTP routes for both the API and the Telegram bot. The `resources/webapp` directory contains the static mini‑app client that is served by the same server.

Core business logic lives in the services (`service/`), which talk to the database through Exposed (`db/`) and provide game operations (fishing, tournaments, shop, referrals). Routes in `app/ApiRoutes.kt` rely on these services and serializers to build responses for the client or the bot.

Achievements are tracked through `AchievementService` and exposed via `/api/achievements` plus the `/achievements` bot command. The first goal, **Коллекционер Кои / Koi Collector**, advances as players catch different koi fish, notifies on new tiers, and allows reward claiming (lure bundles or auto‑fishing time) from the mini‑app and the bot.

In the mini‑app client (`resources/webapp`), the Guide tab now contains an Achievements sub‑tab that shows tier art, localized descriptions, progress (e.g., `3/16`), and claim buttons for unlocked rewards. A red badge on the Guide navigation item highlights unclaimed achievement prizes.

To understand how a specific function works, open the README for the relevant package and follow the links to the detailed files.
