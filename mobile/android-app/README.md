# RiverKing Android

Nested Android project for the RiverKing mobile client.

## Goals

- Keep Android build logic separate from the backend Gradle build.
- Reuse the shared backend API instead of forking gameplay logic.
- Support `play` and `direct` flavors from the same codebase.

## Current scope

- Shared auth with `Telegram sign-in`, `Google sign-in`, and `login/password`.
- Store release artifacts use the canonical Android package name `com.riverking.mobile` and should be signed with the same release key so users can move from itch.io/direct APK installs to Google Play without uninstalling.
- Local Android Studio installs intentionally use flavor-specific package IDs so they do not collide with the canonical store package on the same device:
  - `directDebug` -> `com.riverking.mobile.direct`
  - `playDebug` -> `com.riverking.mobile.play`
- Local non-canonical `release` builds keep those same flavor package IDs and use debug signing, so Android Studio can still launch them without package/signature mismatches.
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

For environment separation, keep shared signing and machine-level settings in `mobile/android-app/gradle.properties` or `~/.gradle/gradle.properties`, and keep server-specific values in local profile files:

- `mobile/android-app/profiles/prod.properties`
- `mobile/android-app/profiles/test.properties`

Tracked templates live at:

- [profiles/prod.example.properties](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/mobile/android-app/profiles/prod.example.properties)
- [profiles/test.example.properties](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/mobile/android-app/profiles/test.example.properties)

Tracked production versioning lives in:

- [version.properties](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/mobile/android-app/version.properties)

Set these Gradle properties when building locally when you need to override the defaults:

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

Common entrypoint:

```bash
mobile/android-app/scripts/build-android.sh [--profile <name>] <target>
```

Recommended workflow:

```bash
mobile/android-app/scripts/build-android.sh --profile test qa-release-apks
mobile/android-app/scripts/build-android.sh --profile prod release-artifacts
mobile/android-app/scripts/build-android.sh --profile test direct-debug-install
mobile/android-app/scripts/build-android.sh --profile prod play-release-aab --stacktrace
mobile/android-app/scripts/build-android.sh direct-debug-install
```

Useful targets:

- `debug-apks` builds both debug APKs.
- `direct-debug-install` builds and installs the `directDebug` APK onto a connected device or emulator.
- `qa-release-apks` builds shareable `directRelease` + `playRelease` APKs for internal QA while keeping the non-canonical `.direct` / `.play` package IDs and debug signing.
- `release-artifacts` builds the canonical itch.io APK plus the canonical Google Play AAB for store release.
- `play-release-aab` builds only the Play bundle when you do not need the itch artifact in the same run.

The script reads the same environment variables as Gradle properties, can load additional values from a named profile file, and prints the final artifact paths after a successful build.

When you build with `--profile <name>`, the script also copies the final artifacts into `mobile/android-app/dist/<name>/` with the profile suffix in the filename, so `prod` and `test` outputs do not get mixed together.

Recommended branch / version flow:

- `develop` -> the script allows only `--profile test` and defaults to it if you omit `--profile`.
- `main` -> the script allows only `--profile prod` and defaults to it if you omit `--profile`.
- Production versioning now lives in `mobile/android-app/version.properties`. Bump that file only when you are preparing a real store release from `main`.
- Test builds derive their version automatically from the tracked prod version plus `date + build number`. The build number comes from `RIVERKING_TEST_BUILD_NUMBER`, `GITHUB_RUN_NUMBER`, or the current git commit count.
- Because `qa-release-apks` stays on `com.riverking.mobile.direct` / `com.riverking.mobile.play`, its internal cadence can move independently from the canonical store package.

Store release targets automatically force `RIVERKING_CANONICAL_APPLICATION_ID=true`, so the shipped itch.io APK and Google Play bundle still use `com.riverking.mobile` with the configured release signing. `qa-release-apks`, Android Studio, and ad-hoc local Gradle runs stay on flavor-specific package IDs and debug signing unless you explicitly pass `-PRIVERKING_CANONICAL_APPLICATION_ID=true`.

If you intentionally need a non-standard combination, use `RIVERKING_SKIP_BRANCH_PROFILE_GUARD=true` as an explicit escape hatch.

GitHub automation:

- [`.github/workflows/android-release.yml`](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/.github/workflows/android-release.yml) builds the prod APK/AAB after a push to `main`, so the workflow always executes from the `main` branch definition
- the same workflow also supports manual `workflow_dispatch` on `main`
- it uploads the release files as workflow artifacts and creates or updates a draft GitHub Release
- in CI, the workflow generates `mobile/android-app/profiles/prod.properties` from GitHub repository variables/secrets, so the local ignored `prod.properties` file is not required on the runner

Artifact naming:

- profile copies under `mobile/android-app/dist/<profile>/` now use `app-riverking-<version>.<ext>`
- the APK/AAB are distinguished by extension: `.apk` for `direct`, `.aab` for `play`
- if a target produces two files with the same extension, the copies keep `-direct` / `-play` suffixes to avoid collisions

For Android Studio, keep the active Build Variant on `directDebug` or `playDebug` when using the regular `Run` action. Local `release` variants remain useful for packaging validation, but they still install under the flavor package IDs rather than the canonical store package.

If a device still has an older local package from the previous package-id scheme, remove it once before retesting from Android Studio. The stale example encountered during migration was `com.riverking.mobile.direct.local`.

For debug installs, the script resolves the target device in this order:

- `RIVERKING_ANDROID_SERIAL`
- `ANDROID_SERIAL`
- `ADB_SERIAL`
- the only connected emulator, if exactly one emulator is attached
- the only connected device, if exactly one device is attached

If more than one Android device is connected and no serial is provided, the install step fails fast instead of silently targeting the wrong device. Debug installs also use `adb install -r -d`, which lets local emulator builds replace a higher-version local build without uninstalling first.

Examples:

```bash
mobile/android-app/scripts/build-android.sh direct-debug-install
ANDROID_SERIAL=emulator-5554 mobile/android-app/scripts/build-android.sh install-play-debug
```

Release outputs:

```bash
./gradlew -p mobile/android-app :app:assembleDirectRelease :app:bundlePlayRelease \
  -PRIVERKING_SIGNING_STORE_FILE=/absolute/path/to/release.keystore \
  -PRIVERKING_SIGNING_STORE_PASSWORD=... \
  -PRIVERKING_SIGNING_KEY_ALIAS=... \
  -PRIVERKING_SIGNING_KEY_PASSWORD=...
```

For store-targeted builds, prefer:

```bash
mobile/android-app/scripts/build-android.sh --profile prod release-artifacts
```

The store release path now fails fast unless:

- `RIVERKING_CANONICAL_APPLICATION_ID=true`
- all `RIVERKING_SIGNING_*` values are present in env or standard Gradle property files

Raw Gradle `release` tasks without the canonical package flag remain acceptable for local packaging verification only, not for distribution.

## Distribution notes

- `directRelease` is the APK intended for itch.io and other manual APK installs.
- `playRelease` is the AAB intended for Google Play.
- Both release channels must use the same signing key and a single monotonic `versionCode` sequence.
- If Play App Signing is enabled, keep the existing RiverKing release keystore compatible with the Play-delivered app-signing identity so itch.io installs can upgrade cleanly.
- Do not let the itch.io/direct release overtake the Google Play version code once Play becomes the canonical upgrade path.
- Release builds default to HTTPS-only networking. Cleartext is enabled only from the debug manifest for local emulator work.

## Store Assets

- Launcher icons are wired through adaptive icon resources:
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
  - `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`
- Startup splash styling is defined in:
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/res/values/colors.xml`
- Listing assets live under `docs/branding/`.
- Emulator-captured Android screenshots live under `docs/screenshots/`.

- `docs/branding/android-icon-1024.png`
- `docs/branding/itch-cover-1280x720.png`
- `docs/branding/play-feature-1024x500.png`

Current store assets are managed manually from the approved design exports. When the icon changes, update `docs/branding/android-icon-1024.png` and refresh the Android foreground asset used by the adaptive icon/splash resources.

## Asset Management

To minimize APK size while supporting local asset loading, we use WebP conversion.

- Source assets: `src/main/resources/webapp/assets/`
- Target assets: `mobile/android-app/app/src/main/assets/`
- Conversion tool: `mobile/android-app/convert_assets.py` (requires `Pillow`)

If you add or update gameplay assets or achievement badges in the webapp project, re-run the conversion:

```bash
python3 mobile/android-app/convert_assets.py
```
