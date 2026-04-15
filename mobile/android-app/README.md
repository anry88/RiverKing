# RiverKing Android

Nested Android project for the RiverKing mobile client.

## Goals

- Keep Android build logic separate from the backend Gradle build.
- Reuse the shared backend API instead of forking gameplay logic.
- Support `play` and `direct` flavors from the same codebase.

## Current scope

- Shared auth with `Telegram sign-in`, `Google sign-in`, and `login/password`.
- `direct` and `play` now share the same canonical Android package name and should be signed with the same release key so users can move from itch.io/direct APK installs to Google Play without uninstalling.
- Existing Android profiles can link a Telegram account and continue on the same backend player profile inside the Mini App/bot.
- Android shell now mirrors the TG client much more closely:
  - five-tab layout: fishing, leaders, catalog, club, shop
  - custom dark game-theme with header stats, language toggle, and badgeable bottom navigation
  - full staged fishing flow over shared `/api/start-cast`, `/api/hook`, `/api/cast`
  - quick-cast removed from the public Android UX
  - catch details dialog plus native Android share sheet backed by `/api/catches/{id}/card`
  - tournaments, ratings, guide, achievements, quests, club, referrals, and shop surfaces running on shared backend contracts
- `direct` keeps the shop visible but disables real-money packs.
- `play` uses real `BillingClient` / `ProductDetails` purchase flow and hands the purchase token to the backend for Google Play verification before entitlement delivery.
- Android Telegram account linking and referral actions live in the profile menu opened from the nickname, not inside the shop tab.
- Android referrals now expose auth-aware invite variants:
  - Telegram-auth users get the Telegram Mini App invite flow.
  - Password/Google-auth users get a store-aware Android invite flow that targets itch.io for `direct` and Google Play for `play`, with an in-app deep link fallback.
- The profile menu also exposes support, privacy policy, in-app account deletion, and the public web account-deletion page used for Play policy compliance.
- **Asset Bundling & Optimization**:
  - Core gameplay assets (rods, fish, locations, shop icons, achievement badges) are bundled locally in the APK to reduce network traffic and enable offline-ready UI.
  - All PNG assets from the webapp are converted to **WebP** format for the Android build to minimize APK size (e.g., 60MB PNG -> ~22MB WebP).
  - Achievement badges are synced from the shared webapp assets into `android_asset/achievements`, so the Achievements tab no longer depends on per-open network fetches.
- Debug builds currently verified for both `direct` and `play` flavors.

## Local setup

Use [gradle.example.properties](gradle.example.properties) as the full template for Android build properties. Copy it to `~/.gradle/gradle.properties` or take the `RIVERKING_*` entries from it and pass them through `-P...` flags. Keep `sdk.dir` in `mobile/android-app/local.properties` on each machine.

Set these Gradle properties when building locally:

- `RIVERKING_API_BASE_URL`
- `RIVERKING_PUBLIC_WEB_URL`
- `RIVERKING_ITCH_PROJECT_URL`
- `RIVERKING_PLAY_STORE_URL`
- `RIVERKING_SUPPORT_URL`
- `RIVERKING_PRIVACY_POLICY_URL`
- `RIVERKING_ACCOUNT_DELETION_URL`
- `RIVERKING_VERSION_CODE`
- `RIVERKING_VERSION_NAME`
- `RIVERKING_GOOGLE_AUTH_CLIENT_ID`
- `RIVERKING_SIGNING_STORE_FILE`
- `RIVERKING_SIGNING_STORE_PASSWORD`
- `RIVERKING_SIGNING_KEY_ALIAS`
- `RIVERKING_SIGNING_KEY_PASSWORD`

The backend also needs Google Play verification configured before `/api/shop/{id}/play/complete` can work against real purchases:

- `GOOGLE_PLAY_PACKAGE_NAME`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_FILE`

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

Build scripts:

```bash
mobile/android-app/scripts/build-direct-debug-apk.sh
mobile/android-app/scripts/build-play-debug-apk.sh
mobile/android-app/scripts/build-direct-release-apk.sh
mobile/android-app/scripts/build-play-release-apk.sh
mobile/android-app/scripts/build-play-release-aab.sh
mobile/android-app/scripts/build-debug-apks.sh
mobile/android-app/scripts/build-release-artifacts.sh
mobile/android-app/scripts/install-direct-debug.sh
mobile/android-app/scripts/install-play-debug.sh
```

Generic entrypoint:

```bash
mobile/android-app/scripts/build-android.sh release-artifacts
mobile/android-app/scripts/build-android.sh play-release-aab --stacktrace
mobile/android-app/scripts/build-android.sh direct-debug-install
```

The scripts read the same environment variables as Gradle properties and print the final artifact paths after a successful build.

For debug installs, the script resolves the target device in this order:

- `RIVERKING_ANDROID_SERIAL`
- `ANDROID_SERIAL`
- `ADB_SERIAL`
- the only connected emulator, if exactly one emulator is attached
- the only connected device, if exactly one device is attached

If more than one Android device is connected and no serial is provided, the install step fails fast instead of silently targeting the wrong device. Debug installs also use `adb install -r -d`, which lets local emulator builds replace a higher-version local build without uninstalling first.

Examples:

```bash
mobile/android-app/scripts/install-direct-debug.sh
ANDROID_SERIAL=emulator-5554 mobile/android-app/scripts/install-play-debug.sh
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

## Distribution notes

- `directRelease` is the APK intended for itch.io and other manual APK installs.
- `playRelease` is the AAB intended for Google Play.
- Both release channels must use the same signing key and a single monotonic `versionCode` sequence.
- Do not let the itch.io/direct release overtake the Google Play version code once Play becomes the canonical upgrade path.
- Release builds default to HTTPS-only networking. Cleartext is enabled only from the debug manifest for local emulator work.

## Store Assets

- Launcher icon resources are generated into `app/src/main/res/drawable-nodpi/` and wired through `mipmap-anydpi-v26/`.
- Listing assets live under `docs/branding/`.
- Emulator-captured Android screenshots live under `docs/screenshots/`.

Regenerate the icon and listing graphics:

```bash
python3 mobile/android-app/scripts/generate_brand_assets.py
```

Current generated outputs:

- `docs/branding/android-icon-1024.png`
- `docs/branding/itch-cover-1280x720.png`
- `docs/branding/play-feature-1024x500.png`

## Asset Management

To minimize APK size while supporting local asset loading, we use WebP conversion.

- Source assets: `src/main/resources/webapp/assets/`
- Target assets: `mobile/android-app/app/src/main/assets/`
- Conversion tool: `mobile/android-app/convert_assets.py` (requires `Pillow`)

If you add or update gameplay assets or achievement badges in the webapp project, re-run the conversion:

```bash
python3 mobile/android-app/convert_assets.py
```
