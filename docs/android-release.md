# Android Release Guide

RiverKing ships Android in two channels:

- `directRelease` APK for `itch.io`
- `playRelease` AAB for `Google Play`

The rollout order is deliberate: publish and validate the itch.io APK first, then ship the Google Play bundle on the same product identity.

## Release Contract

- Use one canonical Android package name: `com.riverking.mobile`
- Use one signing key for both itch.io APKs and Google Play App Signing upload setup
- Use one monotonic `versionCode` sequence across both channels
- Keep channel differences limited to:
  - `BuildConfig.DISTRIBUTION_CHANNEL`
  - billing behavior
  - store/referral URLs
  - listing copy and release notes

If the package name or signing key diverges, users cannot upgrade from itch.io to Google Play in place.

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

## Build Commands

From the repository root:

```bash
./gradlew test
./gradlew -p mobile/android-app :app:assembleDirectRelease
./gradlew -p mobile/android-app :app:bundlePlayRelease
```

Expected outputs:

- APK: `mobile/android-app/app/build/outputs/apk/direct/release/`
- AAB: `mobile/android-app/app/build/outputs/bundle/playRelease/`

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

- fishing home
- achievements / guide
- tournaments
- club overview
- shop / daily reward
- ratings

Prepare one additional cover image sized for the itch.io project page.

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
- feature graphic
- short description
- full description

### Upgrade Path Rule

Do not let the itch.io channel outpace Google Play in `versionCode` once Play becomes the canonical upgrade path. Otherwise a user who installed from itch.io may be unable to upgrade in place from Play.

## QA Checklist

- Fresh install from itch.io APK
- Password login
- Telegram deeplink login/link flow
- Referral/share opens itch.io URL in `direct`
- Privacy, support, and account deletion links open correctly
- In-app delete account removes access token and refresh token usability
- Play Billing works only in `play`
- `playRelease` installs over the same package/signing lineage without uninstall when testing cross-channel upgrade

## Official References

- itch.io getting started: [Your first itch.io page](https://itch.io/docs/creators/getting-started)
- Google Play app setup: [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en)
- Google Play target API requirements: [Target API level requirements](https://developer.android.com/google/play/requirements/target-sdk)
- Google Play Android App Bundle requirement: [Android App Bundles on Google Play](https://support.google.com/googleplay/android-developer/answer/9844279)
