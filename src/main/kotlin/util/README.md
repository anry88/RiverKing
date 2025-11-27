# `util` package

Utility helpers and metric counters used across services and routes.

- **`Metrics.kt`** — lightweight metrics aggregator: `counter(name, labels)` increments counters, `gauge` records numeric values, `dump()` outputs Prometheus‑compatible text for `/metrics`.
- **`UserMetrics.kt`** — calculates gameplay metrics (online users, active casts, player progress) and forwards them to `Metrics` before serving `/metrics`.
- **`Rng.kt`** — wrappers around `Random` with a fixed seed for tests and derived helpers (`weighted`, `choice`, `chance`).
- **`CoinCalculator.kt`** — computes coin rewards for catches considering weight and rod/location bonuses.
- **`TextSanitizer.kt`** — cleans nicknames and text inputs (`sanitizeName`) from extra characters, masks profanity, and limits length before saving to the database or sending to the bot.
- **`ProfanityFilter.kt`** — loads regex patterns from `resources/profanity.txt` and masks matching substrings with asterisks; update the wordlist file to tweak filtering without code changes.
