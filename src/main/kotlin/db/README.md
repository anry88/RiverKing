# `db` package

The data layer defines Exposed tables and handles connection initialization.

- **`Tables.kt`** — contains table definitions (`Users`, `AuthIdentities`, `PasswordCredentials`, `AuthSessions`, `Lures`, `Rods`, `Locations`, `Catches`, `Tournaments`, `SpecialEvents`, `SpecialEventFish`, `SpecialEventUserProgress`, `SpecialEventClubProgress`, `SpecialEventCatches`, `SpecialEventPrizes`, `Payments`, `Achievements`, `QuestProgress`, `Clubs`, `ClubMembers`, `ClubChatMessages`, `ClubWeeklyContributions`, `ClubWeeklyRewards`, `ClubQuestProgress`, `ClubQuestRewardRecipients`, etc.). `Locations.cast_zone_json` stores optional polygon cast zones for regular and event locations. `Users.tg_id` is now a nullable legacy link for Telegram, while auth providers live in the dedicated auth tables.
- **`DB`** (inside `Tables.kt`) provides `init(env: Env)`, which opens the SQLite/PostgreSQL Hikari connection pool, applies schema creation and SQLite-specific migrations, seeds reference data, and backfills Telegram identities.
- **`DbExecution.kt`** provides the bounded coroutine dispatcher and helper functions used by Ktor routes for blocking Exposed work, keeping JDBC waits off Ktor request threads.

Use these models when adding new columns: services depend on the column names and types defined here.
