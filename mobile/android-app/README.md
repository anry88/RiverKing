# RiverKing Android

Nested Android project for the RiverKing mobile client.

## Goals

- Keep Android build logic separate from the backend Gradle build.
- Reuse the shared backend API instead of forking gameplay logic.
- Support `play` and `direct` flavors from the same codebase.

## Current scope

- Shared auth with `Google sign-in` and `login/password`.
- Telegram Mini App remains a separate, Telegram-only surface.
- Working Android tabs for:
  - fishing with quick-cast flow over shared `/api/start-cast`, `/api/hook`, `/api/cast`
  - tournaments and pending prize claiming
  - ratings over shared leaderboard endpoints
  - guide, achievements, and quests
- Debug builds currently verified for both `direct` and `play` flavors.

## Local setup

Set these Gradle properties when building locally:

- `RIVERKING_API_BASE_URL`
- `RIVERKING_GOOGLE_AUTH_CLIENT_ID`

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
