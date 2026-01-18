# `db` package

The data layer defines Exposed tables and handles connection initialization.

- **`Tables.kt`** — contains table definitions (`Users`, `Lures`, `Rods`, `Locations`, `Catches`, `Tournaments`, `Prizes`, `Payments`, `Achievements`, `QuestProgress`, etc.). It also stores helper mappings for relations between users, prizes, and tournaments.
- **`DB`** (inside `Tables.kt`) provides `init(env: Env)`, which opens the SQLite/PostgreSQL connection, applies migrations via `buildSchema()`, and configures the connection pool.

Use these models when adding new columns: services depend on the column names and types defined here.
