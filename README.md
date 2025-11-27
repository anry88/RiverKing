# RiverKing

Full MVP codebase: Kotlin/Ktor + Exposed + SQLite, Telegram Mini App.

## Running locally

1. Configure `src/main/resources/config.properties` (at minimum `BOT_TOKEN` and `PUBLIC_BASE_URL`).
   For development you can use dummy values and enable `DEV_MODE=true`:
   ```properties
   BOT_TOKEN=TEST
   PUBLIC_BASE_URL=http://localhost:8080
   DEV_MODE=true
   ```
   Then start the server:
   ```bash
   gradle run
   ```
2. Open [http://localhost:8080/app](http://localhost:8080/app) — the mini app will load (with `DEV_MODE=true` you can run without Telegram).
3. From the bot, open the mini app with a `web_app` button:
   ```
   {
     "keyboard": [[{ "text": "🎣 Play", "web_app": { "url": "https://YOUR_DOMAIN/app?tgId=USER_ID" } }]],
     "resize_keyboard": true
   }
   ```
4. Inside the Telegram mini app, the client sends `initData` → the server verifies the signature and creates a session (`/api/auth/telegram`).
5. Available API endpoints: `/api/me`, `/api/daily`, `/api/location/{id}`, `/api/cast`.

## Nicknames and profanity filter

- On startup the server walks through existing nicknames in the database and masks profane words with asterisks.
- When saving or updating a nickname the server strips control characters, masks profanity from the wordlist, and returns the sanitized value to the UI.
- The wordlist lives in `src/main/resources/profanity.txt`; edit it to add or remove expressions without changing code.

## Tournaments

- Tournament prizes support both lure bundles and in‑game coins. Administrators choose a bundle or coins through inline buttons and set the amount directly from the bot interface.
- Players receive coins directly on balance when the prize includes them.
- In addition to “largest”, “smallest”, and “count” metrics, the “total weight” metric is available for leaderboard calculations.

## Auto casting (bot)

- The `/autocast` command starts automatic fishing for users with an auto‑fishing subscription: the bot reuses the player’s current location, rod, and lure, announces every cast, and repeats casts every three seconds until lures run out or the user stops the loop.
- The bot attempts to message the player privately before starting; when invoked from a group and private messaging is blocked, it asks in the same chat to run `/start` in DMs to grant permission.
- Catch messages during auto casting include a `/stop_autocast` hint, and the bot halts the loop with a notification when no lures remain.
- Gear‑changing commands (lure, location, rod) are disabled during auto casting, and each cast is logged as a `/cast` for metrics.
