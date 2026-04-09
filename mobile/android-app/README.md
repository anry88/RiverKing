# RiverKing Android

Nested Android project for the RiverKing mobile client.

## Goals

- Keep Android build logic separate from the backend Gradle build.
- Reuse the shared backend API instead of forking gameplay logic.
- Support `play` and `direct` flavors from the same codebase.

## Current scope

- Shared auth with `Google sign-in` and `login/password`.
- Telegram Mini App remains a separate, Telegram-only surface.
- Android shell now mirrors the TG client much more closely:
  - six-tab layout: fishing, tournaments, ratings, guide, club, shop
  - custom dark game-theme with header stats, language toggle, and badgeable bottom navigation
  - full staged fishing flow over shared `/api/start-cast`, `/api/hook`, `/api/cast`
  - quick-cast removed from the public Android UX
  - catch details dialog plus native Android share sheet backed by `/api/catches/{id}/card`
  - tournaments, ratings, guide, achievements, quests, club, referrals, and shop surfaces running on shared backend contracts
- `direct` keeps the shop visible but disables real-money packs.
- `play` debug builds can exercise the new `play/complete` backend contract with debug purchase ids; production Play verification still needs final hardening before store release.
- Debug builds currently verified for both `direct` and `play` flavors.

## Local setup

Set these Gradle properties when building locally:

- `RIVERKING_API_BASE_URL`
- `RIVERKING_GOOGLE_AUTH_CLIENT_ID`
- `RIVERKING_SIGNING_STORE_FILE`
- `RIVERKING_SIGNING_STORE_PASSWORD`
- `RIVERKING_SIGNING_KEY_ALIAS`
- `RIVERKING_SIGNING_KEY_PASSWORD`

Example:

```bash
./gradlew -p mobile/android-app :app:assembleDirectDebug \
  -PRIVERKING_API_BASE_URL=http://10.0.2.2:8080 \
  -PRIVERKING_GOOGLE_AUTH_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

Build both debug flavors:

```bash
./gradlew -p mobile/android-app :app:assembleDirectDebug :app:assemblePlayDebug
```

Release outputs:

```bash
./gradlew -p mobile/android-app :app:assembleDirectRelease :app:bundlePlayRelease \
  -PRIVERKING_SIGNING_STORE_FILE=/absolute/path/to/release.keystore \
  -PRIVERKING_SIGNING_STORE_PASSWORD=... \
  -PRIVERKING_SIGNING_KEY_ALIAS=... \
  -PRIVERKING_SIGNING_KEY_PASSWORD=...
```

Without signing properties the `release` build type falls back to the debug signing config, which is acceptable only for local verification and not for distribution.
