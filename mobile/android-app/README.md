# RiverKing Android

Nested Android project for the RiverKing mobile client.

## Goals

- Keep Android build logic separate from the backend Gradle build.
- Reuse the shared backend API instead of forking gameplay logic.
- Support `play` and `direct` flavors from the same codebase.

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
