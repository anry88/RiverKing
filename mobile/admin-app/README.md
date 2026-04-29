# RiverKing Admin Android

Internal Jetpack Compose Android app for RiverKing operators.

This project is separate from the public player app in `mobile/android-app`. It is not part of the player `direct` / `play` release flow, but it shares the same backend version guard — admin API calls receive `426 Upgrade Required` when the app's `versionCode` falls below `minSupportedVersionCode` in the update policy. This ensures backend changes that affect admin endpoints (e.g. new event features) are not used with stale clients. Operators add one or more backend server profiles inside the app, then authenticate admin calls with `ADMIN_API_TOKEN`.

Both mobile projects share a single `version.properties` versioning scheme (`RIVERKING_VERSION_CODE` / `RIVERKING_VERSION_NAME`). When either app's `versionCode` is bumped, the other **must** follow to keep the version line monotonic and aligned with `android-update-policy.json`.

## Scope

- Saved server profiles with backend URL and admin API token.
- Dashboard navigation for tournaments, special events, cast zones, discounts, and broadcasts.
- Tournament list loaded from `/api/admin/tournaments` in pages of 10, sorted newest first.
- Tournament creation and editing using backend catalog choices for metric, fish, location, and up to 1000 prize places.
- Tournament deletion with explicit confirmation.
- Shop discount management using selectable shop packages, rods, and subscriptions with current price context.
- Special event management for temporary club-event locations, image upload, fish pools with manual weights, and separate prize lists for weight/count/top-fish leaderboards.
- Cast-zone editing for both regular and special-event locations. Zones are polygon point paths stored against `Locations`; locations without a configured zone continue to use the client fallback region until an operator draws one.
- Broadcast sending through the backend admin API.

## Backend Contract

The backend registers protected routes in `src/main/kotlin/app/AdminApiRoutes.kt`.

Required backend config:

```properties
ADMIN_API_TOKEN=change-me
```

The app sends on every request:

```http
Authorization: Bearer <ADMIN_API_TOKEN>
X-RiverKing-App-Platform: android
X-RiverKing-App-Version-Code: <versionCode>
X-RiverKing-App-Version-Name: <versionName>
```

The version headers follow the player app's convention. The backend uses `X-RiverKing-App-Version-Code` to enforce the update policy — when it is below `minSupportedVersionCode`, the server replies `426 Upgrade Required` and the client shows an upgrade prompt.

Key endpoints:

- `GET /api/admin/catalog`
- `GET /api/admin/tournaments?offset=0&limit=11`
- `POST /api/admin/tournaments`
- `DELETE /api/admin/tournaments/{id}`
- `GET /api/admin/events`
- `POST /api/admin/events`
- `PUT /api/admin/events/{id}`
- `DELETE /api/admin/events/{id}`
- `POST /api/admin/events/image`
- `GET /api/admin/cast-zones`
- `PUT /api/admin/locations/{id}/cast-zone`
- `GET /api/admin/discounts`
- `POST /api/admin/discounts`
- `DELETE /api/admin/discounts/{packageId}`
- `POST /api/admin/broadcast`

Tournament date fields are entered in the UI as `dd.MM.yyyy` and converted to UTC start-of-day epoch milliseconds before the request. Discount dates are entered as `dd.MM.yyyy`; the backend accepts that format and ISO `yyyy-MM-dd` for compatibility.

## Local Setup

Use a local Android SDK and keep `sdk.dir` in `mobile/admin-app/local.properties`.

Build debug Kotlin:

```bash
./gradlew -p mobile/admin-app :app:compileDebugKotlin
```

Build a release APK:

```bash
cd mobile/admin-app
./gradlew :app:assembleRelease
```

Helper script:

```bash
mobile/admin-app/build_admin.sh
```

The unsigned release APK is written under:

```text
mobile/admin-app/app/build/outputs/apk/release/
```

## Project Map

- `app/src/main/java/com/riverking/admin/MainActivity.kt`: Android activity entrypoint.
- `app/src/main/java/com/riverking/admin/ui/AdminApp.kt`: navigation graph.
- `app/src/main/java/com/riverking/admin/ui/LoginAndDashboard.kt`: saved server profiles and dashboard.
- `app/src/main/java/com/riverking/admin/ui/TournamentsScreen.kt`: paged tournament list, create dialog, delete confirmation.
- `app/src/main/java/com/riverking/admin/ui/EventsScreen.kt`: special event list, create/edit dialog, image upload, fish pool, and reward configuration.
- `app/src/main/java/com/riverking/admin/ui/CastZonesScreen.kt`: point-based polygon editor for regular and event location cast zones.
- `app/src/main/java/com/riverking/admin/ui/BroadcastAndDiscounts.kt`: broadcasts and discount management.
- `app/src/main/java/com/riverking/admin/network/AdminApiClient.kt`: Ktor client and admin DTOs.
- `app/src/main/java/com/riverking/admin/ui/theme/RiverTheme.kt`: Compose theme.

## Notes

- Do not expose `ADMIN_API_TOKEN` in public builds, screenshots, or issue logs.
- Keep this admin app documentation separate from `mobile/android-app/README.md`; the player app has different auth, release, billing, and update-policy requirements.
- If an admin UI change adds or changes an admin API contract, update `AdminApiRoutes.kt`, `AdminApiClient.kt`, and this README together.
