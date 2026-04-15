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
The store release scripts accept signing values from environment variables, `mobile/android-app/gradle.properties`, or `~/.gradle/gradle.properties`.

## Build Commands

From the repository root:

```bash
./gradlew test
./gradlew -p mobile/android-app :app:assembleDirectRelease :app:bundlePlayRelease
mobile/android-app/scripts/build-release-artifacts.sh
```

Expected outputs:

- APK: `mobile/android-app/app/build/outputs/apk/direct/release/`
- AAB: `mobile/android-app/app/build/outputs/bundle/playRelease/`

Command intent:

- raw Gradle `assembleDirectRelease` / `bundlePlayRelease` without `RIVERKING_CANONICAL_APPLICATION_ID=true` stays useful for local packaging validation
- `mobile/android-app/scripts/build-release-artifacts.sh` is the store-targeted path and now fails fast unless the canonical package ID and all `RIVERKING_SIGNING_*` values are configured

## itch.io First Release

Publish the `directRelease` APK on itch.io as the primary public Android download.

### Page Requirements

- Price: free
- Platform: Android
- Upload: `directRelease` APK
- External links:
  - privacy policy
  - support
  - account deletion

### Minimum Page Content

- Short description:
  - `RiverKing is a Telegram-connected fishing RPG for Android with progression, quests, tournaments, clubs, and shared account progress between Telegram and mobile.`
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
