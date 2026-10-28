# `db` package

The data layer defines Exposed tables and handles connection initialization.

- **`Tables.kt`** — contains table definitions (`Users`, `AuthIdentities`, `PasswordCredentials`, `AuthSessions`, `Lures`, `Rods`, `Locations`, `Catches`, `Tournaments`, `SpecialEvents`, `SpecialEventFish`, `SpecialEventUserProgress`, `SpecialEventClubProgress`, `SpecialEventCatches`, `SpecialEventPrizes`, `Payments`, `Achievements`, `QuestProgress`, `Clubs`, `ClubMembers`, `ClubChatMessages`, `ClubWeeklyContributions`, `ClubWeeklyRewards`, `ClubQuestProgress`, `ClubQuestRewardRecipients`, etc.). `Locations.cast_zone_json` stores optional polygon cast zones for regular and event locations. `Users.tg_id` is now a nullable legacy link for Telegram, while auth providers live in the dedicated auth tables.
- **`DB`** (inside `Tables.kt`) provides `init(env: Env)`, which opens the SQLite/PostgreSQL Hikari connection pool, applies schema creation and SQLite-specific migrations, seeds reference data, and backfills Telegram identities. Database access belongs to the server; the gameplay bot module is an HTTP client.
- **`DbExecution.kt`** provides the bounded coroutine dispatcher and helper functions used by Ktor routes for blocking Exposed work, keeping JDBC waits off Ktor request threads.
- The reference fish seed includes thirteen South American species for November 2026. They are not added to regular location pools; the dated event is configured separately from `docs/events/2026-11-south-america/event.json` through the admin API.

Use these models when adding new columns: services depend on the column names and types defined here.
