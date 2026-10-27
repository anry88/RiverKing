# Gameplay stub

Standalone Gradle worker that drives 50 persistent RiverKing profiles to compete in tournaments and club events. It calls shared gameplay services against the existing PostgreSQL database. No HTTP server or Telegram messages are started by this module.

## Accounts and behavior

`Roster.kt` contains 25 Russian and 25 English conversational nicknames. Profiles have `registration_source=bot`, no Telegram identity and notifications disabled. Stable `AuthIdentities(provider=gameplay-stub, subject=v1-01…v1-50)` entries provide ownership across restarts; other profiles with source `bot` are not adopted. Profile, inventory and identity creation is atomic. A PostgreSQL advisory lock permits only one active worker per database.

Each account independently repeats:

1. Keep its existing club, or join a club permitted by normal recruitment, capacity and minimum-weight rules.
2. Claim tournament, rating, event, club and referral prizes, the daily bait reward and achievement rewards. Quest coins are granted automatically by normal catches.
3. Advance in `FishingService.locations` order: active event first, then regular locations by unlock weight. If next is missing/locked, return to the first accessible entry. Outside a club the event is locked, so fallback is the first regular location. Expired events disappear from the list.
4. Select compatible owned bait. If none remains, buy the cheapest compatible coin pack affordable with the actual balance through `buyPackageWithCoins`. Skip the cast if unaffordable. No Stars purchases or injected balances; prize and achievement packages follow their normal reward rules.
5. Repeat the bot `/cast` flow: consume bait, wait 5–30 seconds, hook with reaction time 0 and the bot extra escape chance (0.05 with a matching rod, otherwise 0.10), then finalize a successful hook. No client taps or mini-game actions. The standard service records catches, progression, quests and event standings; tournament boards query those same catches.

Fresh active casts are left alone; stale interrupted casts are recovered only for the processed account. One account's failure does not stop the rest. A lost worker lease stops the process.

## Run and test

Initialize the database with the backend first. The worker uses `DB.init(initializeSchema=false)` and does not seed or migrate production data.

```bash
./gradlew :gameplay-stub:test
CONFIG_PATH=/path/to/config.properties ./gradlew :gameplay-stub:run
CONFIG_PATH=/path/to/config.properties ./gradlew :gameplay-stub:run --args='--provision-only'
CONFIG_PATH=/path/to/config.properties ./gradlew :gameplay-stub:run --args='--once'
```

Configuration uses backend `CONFIG_PATH` and `DATABASE_*` settings. Additional settings:

| Variable | Default | Meaning |
| --- | --- | --- |
| `STUB_INTERVAL_SECONDS` | `300` | Delay after each account's cycle; 30–86400 seconds, plus 0–15 seconds jitter |
| `STUB_STAGGER_SECONDS` | `2` | Initial spacing between accounts; 0–60 seconds |
| `STUB_STATE_DIR` | `/data/gameplay-stub` | Heartbeat with aggregate cycle/catch/error counts |

The Docker worker uses two pool connections and one separate advisory-lock connection. Logs go to stdout with Docker rotation. A heartbeat every 15 seconds is checked by the container healthcheck. Stop via Compose; profiles and progression remain in PostgreSQL.

## HDC

From `develop`, deploy the independent worker alongside the existing PostgreSQL backend:

```bash
scripts/deploy-gameplay-stub-hdc.sh
scripts/deploy-gameplay-stub-hdc.sh --stop
```

The script builds `gameplay-stub/Dockerfile` on the `hdc` Docker context, reads the existing `D:\Apps\RiverKing\env\prod.env`, and starts only the `riverking-prod-stub` Compose project on `riverking-prod_default`. It does not rewrite the backend env or replace its image. Heartbeat state lives in `D:\Apps\RiverKing\state\prod\gameplay-stub`. `--environment test` selects the corresponding test layout. `HDC_REMOTE_HOST`, `HDC_DOCKER_CONTEXT` and `HDC_RIVERKING_ROOT` override host defaults.

```bash
docker --context hdc ps --filter name=riverking-prod-stub
docker --context hdc logs --tail 100 riverking-prod-stub-gameplay-stub-1
```

Android, Mini App and Telegram remain compatible: no API payload or Android version-policy changes. Deploying/stopping the module requires no client updates.
