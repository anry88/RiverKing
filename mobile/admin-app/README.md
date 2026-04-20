# RiverKing Admin Android

Internal Jetpack Compose Android app for RiverKing operators.

This project is separate from the public player app in `mobile/android-app`. It is not part of the player `direct` / `play` release flow and does not use the public Android update policy. Operators add one or more backend server profiles inside the app, then authenticate admin calls with `ADMIN_API_TOKEN`.

## Scope

- Saved server profiles with backend URL and admin API token.
- Dashboard navigation for tournaments, discounts, and broadcasts.
- Tournament list loaded from `/api/admin/tournaments` in pages of 10, sorted newest first.
- Tournament creation and editing using backend catalog choices for metric, fish, location, and prizes.
- Tournament deletion with explicit confirmation.
- Shop discount management using selectable shop packages, rods, and subscriptions with current price context.
- Broadcast sending through the backend admin API.

## Backend Contract

The backend registers protected routes in `src/main/kotlin/app/AdminApiRoutes.kt`.

Required backend config:

```properties
ADMIN_API_TOKEN=change-me
```

The app sends:

```http
Authorization: Bearer <ADMIN_API_TOKEN>
```

Key endpoints:

- `GET /api/admin/catalog`
- `GET /api/admin/tournaments?offset=0&limit=11`
- `POST /api/admin/tournaments`
- `DELETE /api/admin/tournaments/{id}`
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
- `app/src/main/java/com/riverking/admin/ui/BroadcastAndDiscounts.kt`: broadcasts and discount management.
- `app/src/main/java/com/riverking/admin/network/AdminApiClient.kt`: Ktor client and admin DTOs.
- `app/src/main/java/com/riverking/admin/ui/theme/RiverTheme.kt`: Compose theme.

## Notes

- Do not expose `ADMIN_API_TOKEN` in public builds, screenshots, or issue logs.
- Keep this admin app documentation separate from `mobile/android-app/README.md`; the player app has different auth, release, billing, and update-policy requirements.
- If an admin UI change adds or changes an admin API contract, update `AdminApiRoutes.kt`, `AdminApiClient.kt`, and this README together.
