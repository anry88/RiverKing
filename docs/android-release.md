# Android Release Guide

RiverKing ships Android in two channels:

- `directRelease` APK for `itch.io`
- `playRelease` AAB for `Google Play`

The rollout order is deliberate: publish and validate the itch.io APK first, then ship the Google Play bundle on the same product identity.

For local Android Studio work, the project deliberately keeps flavor-specific package IDs so debug and ad-hoc release installs do not collide with the canonical store package on the same device:

- `directDebug` / local `directRelease` -> `com.riverking.mobile.direct`
- `playDebug` / local `playRelease` -> `com.riverking.mobile.play`

Only release packaging flows that explicitly enable `RIVERKING_CANONICAL_APPLICATION_ID=true` produce the store package `com.riverking.mobile`.

## Release Contract

- Use one canonical Android package name: `com.riverking.mobile`
- Use one signing lineage for both itch.io APKs and Google Play distribution
- Use one monotonic `versionCode` sequence across both channels
- Keep channel differences limited to:
  - `BuildConfig.DISTRIBUTION_CHANNEL`
  - billing behavior
  - store/referral URLs
  - listing copy and release notes

If the package name or signing key diverges, users cannot upgrade from itch.io to Google Play in place.

If Google Play App Signing is enabled, do not let Play create a different production app-signing identity for RiverKing. The existing RiverKing release keystore must remain compatible with what users receive from Google Play, otherwise upgrades from itch.io installs will break.

## Recommended Git / Release Flow

- `develop` is the integration branch. Point Android builds at the test backend profile and cut frequent tester drops with:

  ```bash
  mobile/android-app/scripts/build-android.sh --profile test qa-release-apks
  ```

- `main` is the store-release branch. Merge `develop` into `main` through a PR only when you are ready to ship, then cut the production artifacts with:

  ```bash
  mobile/android-app/scripts/build-android.sh --profile prod release-artifacts
  ```

- `build-android.sh` now enforces `develop -> test` and `main -> prod` by default. Use `RIVERKING_SKIP_BRANCH_PROFILE_GUARD=true` only when you intentionally need to bypass that guard.
- Keep store version bumps in `mobile/android-app/version.properties` on the release path to `main`, not in every `develop` build.
- Test builds now derive their version automatically from the tracked prod version plus `date + build number`, so frequent QA drops do not require manual version edits.
- `qa-release-apks` keeps the non-canonical `com.riverking.mobile.direct` / `com.riverking.mobile.play` package IDs and uses debug signing, so internal staging builds do not collide with the shipped store app and do not consume the canonical release line.

## GitHub Release Automation

The repository now includes [`.github/workflows/android-release.yml`](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/.github/workflows/android-release.yml).
It also now includes:

- [`.github/workflows/release-pr.yml`](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/.github/workflows/release-pr.yml) to create or update the `develop -> main` draft release PR after every push to `develop`
- [`.github/workflows/pr-labeler.yml`](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/.github/workflows/pr-labeler.yml) to apply scope labels automatically
- [`.github/workflows/pr-required-labels.yml`](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/.github/workflows/pr-required-labels.yml) to fail PRs that do not carry a required change-type label
- [`.github/release.yml`](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/.github/release.yml) to shape GitHub auto-generated release notes

It runs in two modes:

- automatically after a push to `main`
- manually through `workflow_dispatch` when you launch the workflow on the `main` branch

What it does:

- checks out the pushed `main` commit
- reads the prod version from `mobile/android-app/version.properties`
- validates `src/main/resources/android-update-policy.json` against that version
- builds `--profile prod release-artifacts`
- uploads the APK/AAB as workflow artifacts
- creates or updates a **draft** GitHub Release with the built files attached
- release copies use `app-riverking-<version>.apk` and `app-riverking-<version>.aab`

Current version behavior is intentional:

- production builds use `RIVERKING_VERSION_NAME` and `RIVERKING_VERSION_CODE` from `mobile/android-app/version.properties`
- the release build uses whatever value is currently tracked in that file
- bump `version.properties` in the release PR before merging into `main` whenever you want the shipped version to change

## Android Update Policy

The backend controls Android app freshness through [src/main/resources/android-update-policy.json](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/src/main/resources/android-update-policy.json). The Android client calls `GET /api/mobile/update` at startup/resume and sends these headers on API calls:

- `X-RiverKing-App-Platform: android`
- `X-RiverKing-App-Channel: direct` or `play`
- `X-RiverKing-App-Version-Code`
- `X-RiverKing-App-Version-Name`

Policy fields:

- `latestVersionCode` / `latestVersionName` describe the newest shipped Android release.
- `minSupportedVersionCode` is the hard floor. If a mobile request is below it, the API returns `426 Upgrade Required` and the Android client shows a blocking update screen.
- `requireVersionHeaders` should stay `false` during normal rollout. Set it to `true` only when a fatal change must also block legacy Android builds that do not send version headers yet.
- `releaseNotes.en` and `releaseNotes.ru` feed the optional/mandatory update UI and generated release context.

Normal release bump:

1. Update `mobile/android-app/version.properties`.
2. Update `src/main/resources/android-update-policy.json` `latestVersionCode`, `latestVersionName`, and release notes.
3. Keep `minSupportedVersionCode` unchanged unless older clients truly cannot keep using the backend.
4. Run `python3 mobile/android-app/scripts/check-update-policy.py`.

Mandatory/fatal release bump:

1. Raise `minSupportedVersionCode` to the first version that can safely use the current backend contract.
2. If the incompatible clients predate Android version headers, set `requireVersionHeaders` to `true`; otherwise keep it `false` and let `minSupportedVersionCode` do the blocking.
3. Add the `android-force-update` label to the release-bound PR, and also use `breaking-change` when the server/client contract is incompatible.
4. Put the forced update reason in the PR release notes and `android-update-policy.json` notes.
5. Publish the new APK/store artifact and install URL first, then deploy the backend policy so stale clients receive a clear upgrade response instead of a dead-end block.

Install targets:

- `direct` opens the itch.io page unless the backend has `RIVERKING_ANDROID_DIRECT_DOWNLOAD_URL`; with that URL it downloads the APK in-app through Android `DownloadManager` and opens the system package installer.
- `play` opens `RIVERKING_PLAY_STORE_URL`, or a Google Play URL built from `GOOGLE_PLAY_PACKAGE_NAME`, falling back to itch.io/support only if no store URL is available.

## GitHub PR Rules

After every push to `develop`, GitHub can now create or update a single draft release PR from `develop` into `main`.

The release PR:

- is titled `Release Android <version>`
- reads `<version>` from `mobile/android-app/version.properties`
- carries the `release` label automatically
- stays reusable as the running release train until you merge or close it

Regular PRs should carry:

- one required change-type label: `feature`, `fix`, `docs`, `ci`, `chore`, `refactor`, `breaking-change`, or `release`
- any scope labels that GitHub applies automatically from changed paths, such as `android`, `backend`, `webapp`, `docs`, or `ci`

To make the rule actually blocking in GitHub, add the `PR Required Labels` workflow as a required status check in your branch protection rule or ruleset for `develop` and `main`.

To let the release PR workflow create PRs with `GITHUB_TOKEN`, enable this repository setting:

- `Settings -> Actions -> General -> Workflow permissions -> Read and write permissions`
- `Allow GitHub Actions to create and approve pull requests`

Without that repository setting, GitHub can run the workflow file but will reject the PR creation request.

## GitHub Release Notes

GitHub release notes now use [`.github/release.yml`](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/.github/release.yml).

Generated categories are:

- `Mandatory Android Updates`
- `Breaking Changes`
- `Features`
- `Fixes`
- `Documentation`
- `CI and Maintenance`
- `Releases`
- `Other Changes`

The practical flow is:

1. Push release-bound work into `develop`.
2. Let GitHub update the draft `develop -> main` release PR.
3. Keep PR labels accurate so generated release notes stay readable.
4. Merge that PR into `main`.
5. Let `android-release.yml` build artifacts and create the draft GitHub Release with auto-generated notes.
6. Review the draft release body, add a short manual preface if needed, then publish.

Required GitHub repository secrets:

- `RIVERKING_SIGNING_KEYSTORE_BASE64`
- `RIVERKING_SIGNING_STORE_PASSWORD`
- `RIVERKING_SIGNING_KEY_ALIAS`
- `RIVERKING_SIGNING_KEY_PASSWORD`
- `RIVERKING_GOOGLE_AUTH_CLIENT_ID` (optional, only if you want the CI build to embed Google auth)

Required GitHub repository variables for the prod profile:

- `RIVERKING_PROD_API_BASE_URL`
- `RIVERKING_PROD_PUBLIC_WEB_URL`

Optional GitHub repository variables for the prod profile:

- `RIVERKING_PROD_ITCH_PROJECT_URL`
- `RIVERKING_PROD_PLAY_STORE_URL`
- `RIVERKING_PROD_SUPPORT_URL`
- `RIVERKING_PROD_PRIVACY_POLICY_URL`
- `RIVERKING_PROD_ACCOUNT_DELETION_URL`

The workflow writes `mobile/android-app/profiles/prod.properties` on the runner from those GitHub variables before calling `build-android.sh`.
If you prefer, the same names can be stored as repository secrets instead; the workflow uses variables first and secrets as fallback.

Backend deploys should also set these runtime values for Android update/install prompts:

- `RIVERKING_ITCH_PROJECT_URL`
- `RIVERKING_PLAY_STORE_URL`
- `RIVERKING_ANDROID_DIRECT_DOWNLOAD_URL` when the direct APK should install from an in-app download instead of only opening itch.io

Suggested keystore preparation for the secret:

```bash
base64 < /absolute/path/to/release.keystore | tr -d '\n'
```

Use the resulting single-line string as `RIVERKING_SIGNING_KEYSTORE_BASE64`.

## Backend Compliance Surface

The backend now exposes the public pages required for Android distribution:

- `/privacy`
- `/terms`
- `/support`
- `/account/delete`
- `POST /account/delete/request`

The authenticated Android client also supports immediate deletion through:

- `POST /api/account/delete`

Use the public page URLs in itch.io and Google Play listing metadata.

## Android Build Inputs

Set these Gradle properties before producing release artifacts:

- `RIVERKING_API_BASE_URL`
- `RIVERKING_PUBLIC_WEB_URL`
- `RIVERKING_ITCH_PROJECT_URL`
- `RIVERKING_PLAY_STORE_URL`
- `RIVERKING_SUPPORT_URL`
- `RIVERKING_PRIVACY_POLICY_URL`
- `RIVERKING_ACCOUNT_DELETION_URL`
- `RIVERKING_VERSION_CODE`
- `RIVERKING_VERSION_NAME`
- `RIVERKING_SIGNING_STORE_FILE`
- `RIVERKING_SIGNING_STORE_PASSWORD`
- `RIVERKING_SIGNING_KEY_ALIAS`
- `RIVERKING_SIGNING_KEY_PASSWORD`

Use [mobile/android-app/gradle.example.properties](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/mobile/android-app/gradle.example.properties) as the template.
Use [mobile/android-app/version.properties](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/mobile/android-app/version.properties) as the tracked source of truth for production `versionCode` / `versionName`.
The store release scripts accept signing values from environment variables, `mobile/android-app/gradle.properties`, or `~/.gradle/gradle.properties`.
If `RIVERKING_ITCH_PROJECT_URL` is not set, the Android build falls back to `$publicWebUrl/support`, which is acceptable for an internal pre-release APK but not ideal for the public itch.io build.

For environment separation, keep production and test/staging server URLs in local Android profile files:

- `mobile/android-app/profiles/prod.properties`
- `mobile/android-app/profiles/test.properties`

Tracked starter templates live at:

- [prod.example.properties](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/mobile/android-app/profiles/prod.example.properties)
- [test.example.properties](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/mobile/android-app/profiles/test.example.properties)

Practical versioning policy:

- keep the store `RIVERKING_VERSION_CODE` and `RIVERKING_VERSION_NAME` in `mobile/android-app/version.properties`
- let `build-android.sh --profile test ...` auto-generate the test version unless you have a specific reason to override it
- if CI is building test APKs, let `GITHUB_RUN_NUMBER` feed the test build number automatically

## Build Commands

From the repository root:

```bash
./gradlew test
./gradlew -p mobile/android-app :app:assembleDirectRelease :app:bundlePlayRelease
mobile/android-app/scripts/build-android.sh --profile test qa-release-apks
mobile/android-app/scripts/build-android.sh --profile test direct-debug-install
mobile/android-app/scripts/build-android.sh --profile prod release-artifacts
```

Expected outputs:

- APK: `mobile/android-app/app/build/outputs/apk/direct/release/`
- AAB: `mobile/android-app/app/build/outputs/bundle/playRelease/`

Command intent:

- raw Gradle `assembleDirectRelease` / `bundlePlayRelease` without `RIVERKING_CANONICAL_APPLICATION_ID=true` stays useful for local packaging validation
- `mobile/android-app/scripts/build-android.sh --profile test qa-release-apks` is the simplest internal staging path for `develop` because it gives you shareable APKs against the test backend without touching the canonical store identity
- `mobile/android-app/scripts/build-android.sh --profile prod release-artifacts` is the store-targeted path and fails fast unless the canonical package ID, the `prod` profile, and all `RIVERKING_SIGNING_*` values are configured
- profile builds also copy artifacts into `mobile/android-app/dist/<profile>/` so production and test outputs stay separate

## itch.io First Release

Publish the `directRelease` APK on itch.io as the primary public Android download.

Recommended order:

1. Create the itch.io project page as a private draft.
2. Copy the generated itch.io page URL.
3. Set `RIVERKING_ITCH_PROJECT_URL` to that draft URL.
4. Build the final signed `directRelease` APK.
5. Upload that APK to the existing draft page and only then publish the page.

### Page Requirements

- Price: free
- Platform: Android
- Upload: `directRelease` APK
- External links:
  - privacy policy
  - support
  - account deletion

For the first RiverKing launch on itch.io, keep the page free. If seller settings are not configured yet, do not set a minimum price above `0`, otherwise itch.io will block downloads. If you later add optional donations, `Collected by itch.io, paid later` is the simpler default path.

### Minimum Page Content

- Short description:
  - `RiverKing is a Telegram-connected fishing RPG for Android with progression, quests, tournaments, clubs, and shared account progress between Telegram and mobile.`
- Full page description:
  - see the ready-to-paste itch.io draft in [docs/store-launch-checklist.md](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/store-launch-checklist.md)
- Feature bullets:
  - real catch-and-collect fishing loop
  - rods, lures, locations, and fish discovery
  - achievements, quests, tournaments, and club competition
  - password and Telegram-based mobile account access
- Install note:
  - `Download the APK, allow installs from your browser or file manager, then install the update over the same package/signature when a new build is released.`
- Links:
  - privacy policy
  - support
  - account deletion
- Changelog:
  - include `versionName`, `versionCode`, date, and the user-facing changes

### Recommended Visual Assets

Use or adapt the shipped screenshots under [docs/screenshots](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots):

- [android-01-daily-reward.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/android-01-daily-reward.png)
- [android-02-fishing-home.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/android-02-fishing-home.png)
- [android-03-leaders.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/android-03-leaders.png)
- [android-04-catalog.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/android-04-catalog.png)
- [android-05-club.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/android-05-club.png)
- [android-06-shop.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/android-06-shop.png)

For itch.io, use the downscaled copies under [docs/screenshots/itch](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/itch):

- [android-01-daily-reward.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/itch/android-01-daily-reward.png)
- [android-02-fishing-home.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/itch/android-02-fishing-home.png)
- [android-03-leaders.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/itch/android-03-leaders.png)
- [android-04-catalog.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/itch/android-04-catalog.png)
- [android-05-club.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/itch/android-05-club.png)
- [android-06-shop.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/screenshots/itch/android-06-shop.png)

These itch copies preserve the same portrait images but fit under itch.io's `3840x2160` limit. Keep the original taller screenshots for Google Play.

Branding assets for the listing already exist under [docs/branding](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/branding):

- [android-icon-1024.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/branding/android-icon-1024.png)
- [google-play-icon-512.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/branding/google-play-icon-512.png)
- [itch-cover-1280x720.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/branding/itch-cover-1280x720.png)
- [itch-cover-630x500.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/branding/itch-cover-630x500.png)
- [play-feature-1024x500.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/branding/play-feature-1024x500.png)
- [google-play-developer-header-4096x2304.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/branding/google-play-developer-header-4096x2304.png)

Android launcher and startup visuals are sourced separately from the listing assets:

- adaptive icon XML:
  - `mobile/android-app/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - `mobile/android-app/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- foreground artwork:
  - `mobile/android-app/app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`
- splash theme:
  - `mobile/android-app/app/src/main/res/values/themes.xml`
- splash background color:
  - `mobile/android-app/app/src/main/res/values/colors.xml`

## Google Play Follow-Up

Once the itch.io build is stable, ship the `playRelease` bundle.

### Listing / Console Checklist

- Upload `playRelease` AAB
- Reuse the same signing lineage as the itch.io APK
- Complete:
  - Data safety
  - App access
  - Content rating
  - Data deletion
- Provide reviewer instructions for:
  - password login
  - Telegram account linking
  - any gated gameplay or monetization flow

### Store Metadata

- Privacy policy URL
- Support URL
- Account deletion URL
- 4–8 phone screenshots
- [play-feature-1024x500.png](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/branding/play-feature-1024x500.png)
- short description
- full description

Repository launch checklist and drafted store copy live in [docs/store-launch-checklist.md](/Users/hq-k14lcdcq7d/Documents/IdeaProjects/RiverKing/docs/store-launch-checklist.md).

### Upgrade Path Rule

Do not let the itch.io channel outpace Google Play in `versionCode` once Play becomes the canonical upgrade path. Otherwise a user who installed from itch.io may be unable to upgrade in place from Play.

## QA Checklist

- Fresh install from itch.io APK
- Fresh local Android Studio run on emulator with `directDebug`
- Fresh local Android Studio run on physical device with `directDebug`
- Password login
- Telegram deeplink login/link flow
- Referral/share opens itch.io URL in `direct`
- Privacy, support, and account deletion links open correctly
- In-app delete account removes access token and refresh token usability
- Play Billing works only in `play`
- `playRelease` installs over the same package/signing lineage without uninstall when testing cross-channel upgrade

If a physical device still has an older local package such as `com.riverking.mobile.direct.local`, remove it before testing the current IDE target. Otherwise Android Studio can attempt to launch `com.riverking.mobile.direct` while only the stale old package is installed.

## Official References

- itch.io getting started: [Your first itch.io page](https://itch.io/docs/creators/getting-started)
- Google Play app setup: [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en)
- Google Play target API requirements: [Target API level requirements](https://developer.android.com/google/play/requirements/target-sdk)
- Google Play app signing: [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)
- Google Play Android App Bundle requirement: [Android App Bundles on Google Play](https://support.google.com/googleplay/android-developer/answer/9844279)
