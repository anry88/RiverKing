# RiverKing architecture

This document explains how the RiverKing mini‑app server is organized and where to find more details. The structure mirrors the source tree so you can jump from directories to component and function descriptions.

- [Ktor application (`src/main/kotlin/app`)](src/main/kotlin/app/README.md)
- [Domain services (`src/main/kotlin/service`)](src/main/kotlin/service/README.md)
- [Data layer (`src/main/kotlin/db`)](src/main/kotlin/db/README.md)
- [Utilities (`src/main/kotlin/util`)](src/main/kotlin/util/README.md)

## Overview

The server starts from `main()` in `Application.kt`: it loads the configuration, configures Ktor plugins, initializes the database, and registers HTTP routes for both the API and the Telegram bot. The `resources/webapp` directory contains the static mini‑app client that is served by the same server.

Core business logic lives in the services (`service/`), which talk to the database through Exposed (`db/`) and provide game operations (fishing, tournaments, shop, referrals). Routes in `app/ApiRoutes.kt` rely on these services and serializers to build responses for the client or the bot.

To understand how a specific function works, open the README for the relevant package and follow the links to the detailed files.
