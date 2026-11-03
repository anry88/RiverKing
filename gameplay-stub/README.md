# Gameplay bot HTTP client

Independent Gradle application that controls 50 persistent player accounts exclusively through the RiverKing server HTTP API. The module has no backend project dependency, game services, SQL/JDBC drivers, database credentials or Telegram/admin token at runtime.

Server integration tests consume the thin `testApiElements` / `test-api` JAR. The normal executable `jar` still bundles the HTTP client's runtime dependencies for deployment. Do not put that executable JAR on the server test classpath: its embedded libraries lack their original manifest metadata and break Kotlin serialization compilation.

## Server boundary and authorization

The backend owns `service/GameplayBotAccounts.kt`: 25 Russian and 25 English conversational nicknames, `registration_source=bot`, and stable `AuthIdentities(provider=gameplay-stub, subject=v1-01…v1-50)` entries. The protected operator endpoint `POST /api/admin/gameplay-bots/sessions` creates missing profiles or reuses existing ones with all progression intact, then issues ordinary access/refresh sessions. Only the deploy operator calls it; the client receives just those sessions in `accounts.json`.

The client applies the varied nickname catalog in `BotNicknames.kt` through `/api/nickname`: 25 Russian and 25 English handles, short phrases, nicknames and emoji combinations. This setup pass updates all existing profiles before fishing starts and saves successful changes, so restarts do not repeat the renames. Profile IDs, languages and progression stay intact. For example: `🎣 Лёха`, `поплавОК`, `Не клюёт 😅`, `mikey.exe`, `404 Fish Not Found`, `🦆 QuackAttack`.

`POST /api/bot/cast` requires an authenticated player with both the server-owned `gameplay-stub` identity and registration source `bot`. Anonymous callers, ordinary player sessions, admin tokens and spoofed client headers do not grant access. The server executes the Telegram `/cast` behavior: consumes bait, waits 5–30 seconds, applies the normal hook/escape rules and finalizes the catch. The client does not call hook/finalize endpoints or simulate taps. Existing Android, Mini App and Telegram contracts remain unchanged.

All HTTP requests pass through the main server metrics. Casts increment `cast_total`, `hook_total`, `fish_caught_total`, `catch_weight_kg` and `bot_cast_total`. New paths appear in `api_call_total` and request status/duration metrics. Registration gauges show source `bot`.

## Cycle

Strictly one account at a time in the existing alternating Russian/English roster order: finish the full cycle, including the server's 5–30-second cast wait, then immediately move to the next eligible account. After the roster ends, return to its beginning. Failed cycles also move on without an additional pause. Operators can configure an optional pause with `STUB_ACCOUNT_PAUSE_SECONDS`. All 50 existing profiles are reused.

`BotSchedule.kt` assigns fixed shifts by stable slot order, alternating Russian and English profiles before partitioning:

| Positions in alternating roster | Accounts | Daily window (server time) |
| --- | --- | --- |
| 1–5 | 5 | 24/7 |
| 6–20 | 15 | 09:00–17:00 |
| 21–35 | 15 | 16:00–01:00 |
| 36–50 | 15 | 01:00–09:00 |

Start times are inclusive and end times exclusive. The evening window is deliberately nine hours and overlaps the day group from 16:00 to 17:00: 35 accounts are eligible during that hour and 20 otherwise. Membership survives restarts and reordered session files. The worker checks the current time before every account; inactive accounts make no cycle HTTP calls and incur no pause. An already started cycle finishes after its window closes. `--once` visits eligible accounts once. Group membership is logged at startup; cycle logs include the group and the heartbeat includes the active-account count.

HDC's game backend and worker use UTC. `STUB_SCHEDULE_TIME_ZONE` explicitly defaults to UTC; if the game server's time zone changes, update this setting to match it.

1. Read `/api/club`; if absent, search and join an eligible existing club.
2. Claim prizes, referral rewards, daily bait and available achievements through ordinary player endpoints; load quests (their rewards are automatic).
3. Read `/api/me` and move to the next location in server order (active event first, then regular locations). Missing/locked next location wraps to the first accessible location. Expired/locked events fall back through the updated server list.
4. After changing location, check bait compatibility with its fish using `/api/guide` (water type and predator type; event locations support all bait types). Randomly select one compatible bait available in the player's inventory (`qty > 0`), including fresh, salt, and boosted (`+`) variants, and switch to it through `/api/lure/{id}` if different from the currently selected lure. If no compatible bait is in stock, buy the cheapest compatible coin pack affordable with the actual balance through `/api/shop/{id}/coins` and randomly select among the acquired compatible lures; insufficient coins skip the cast. No Stars purchases or free shop grants.
5. Make one `POST /api/bot/cast` request and wait for the server result. Busy casts and unaffordable bait skip the attempt. Catches contribute to normal tournament and club-event standings.

The client refreshes expired access tokens through `/api/auth/refresh` and atomically saves rotated sessions. Mutation requests are not retried after ambiguous transport failures. It reads the server compatibility policy and supplies the required API version header. A file lock in the persistent state directory prevents concurrent clients using the same state.

## Configuration and run

| Variable | Default | Meaning |
| --- | --- | --- |
| `STUB_SERVER_URL` | required | Game server URL; HDC uses `http://app:5005` on the backend network |
| `STUB_ACCOUNT_PAUSE_SECONDS` | `0` | Pause after each completed/failed cycle, 0–3600 seconds; 0 disables the pause |
| `STUB_SCHEDULE_TIME_ZONE` | `UTC` | IANA time zone for daily shifts; must match the game server's time zone |
| `STUB_STATE_DIR` | `/data/gameplay-stub` | `accounts.json`, process lock and heartbeat |

Keep `accounts.json` private: it contains player sessions. No admin token is needed by the running client. If refresh sessions expire, stop the client, remove/securely archive the old session file and provision new sessions via the deployment script; this reuses the same 50 profiles.

```bash
./gradlew :gameplay-stub:test
STUB_SERVER_URL=https://riverking.tg-games.com STUB_STATE_DIR=/path/to/state ./gradlew :gameplay-stub:run
# --once processes the roster once; --health checks the heartbeat.
```

## HDC rollout

Release the backend through the normal `develop -> main` PR flow first, so the two new endpoints are available. Then, from `develop`:

```bash
scripts/deploy-gameplay-stub-hdc.sh
scripts/deploy-gameplay-stub-hdc.sh --stop
```

The deploy script builds the independent HTTP-client image, creates `D:\Apps\RiverKing\state\prod\gameplay-stub`, and, if `accounts.json` does not yet exist, calls the protected server provisioning endpoint using the operator's existing host config. Tokens are written directly to the private state file and are not printed. The runtime container mounts only its state, not the backend config or database. A separate Compose project joins `riverking-prod_default` and calls `http://app:5005`.

`--environment test` uses the test environment. `HDC_REMOTE_HOST`, `HDC_DOCKER_CONTEXT` and `HDC_RIVERKING_ROOT` override host defaults. Logs go to stdout with Docker rotation; a 15-second heartbeat reports completed cycles/catches/errors.

```bash
docker --context hdc ps --filter name=riverking-prod-stub
docker --context hdc logs --tail 100 riverking-prod-stub-gameplay-stub-1
```
