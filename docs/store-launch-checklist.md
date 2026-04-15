# Store Launch Checklist

Practical launch checklist for RiverKing on itch.io first and Google Play second.
Verified against the current repository state on `2026-04-15`.

## Already Prepared In Repo

### Branding assets

- `docs/branding/android-icon-1024.png` — master approved Canva icon
- `docs/branding/google-play-icon-512.png` — Play listing icon
- `docs/branding/play-feature-1024x500.png` — Play feature graphic, RGB, no alpha
- `docs/branding/google-play-developer-header-4096x2304.png` — optional Play developer page header
- `docs/branding/itch-cover-1280x720.png` — wide itch.io hero art
- `docs/branding/itch-cover-630x500.png` — itch.io cover variant for the 315:250 slot
- `mobile/android-app/app/src/main/res/drawable-nodpi/ic_launcher_foreground.png` — Android launcher foreground synced to the approved icon

### Android screenshots

- `docs/screenshots/android-01-daily-reward.png`
- `docs/screenshots/android-02-fishing-home.png`
- `docs/screenshots/android-03-leaders.png`
- `docs/screenshots/android-04-catalog.png`
- `docs/screenshots/android-05-club.png`
- `docs/screenshots/android-06-shop.png`

All current Android screenshots were re-exported as regular RGB PNG files so they are safer to upload to store consoles.

### Release build validation

The repo already passes a local release packaging check:

```bash
./gradlew -p mobile/android-app :app:assembleDirectRelease :app:bundlePlayRelease --console=plain
```

This validates that:

- `directRelease` APK builds
- `playRelease` AAB builds
- the current project can package release artifacts

The repository also now protects the store release path:

- `mobile/android-app/scripts/build-release-artifacts.sh`
- `mobile/android-app/scripts/build-android.sh direct-release-apk`
- `mobile/android-app/scripts/build-android.sh play-release-aab`

These store-targeted commands now fail fast unless the canonical package ID and the release signing variables are configured.

Current local blocker:

- the project owner confirmed that the real release keystore already exists
- this machine still needs the local `RIVERKING_SIGNING_*` values wired in before final signed store artifacts can be produced

## You Need To Do

### 1. Create the accounts

#### itch.io

- Create an itch.io creator account.
- Create a new project page for RiverKing.
- Mark the project as an Android downloadable game.

#### Google Play

- Create a Google Play Console developer account.
- Decide whether you are publishing as a personal developer or an organization.
- Complete identity verification and the payments profile setup inside Google Play Console.

Important:

- Google explicitly documents the `12 testers for 14 days` closed-testing requirement for **new personal developer accounts** before production access.
- If you create a personal account, assume this testing requirement will apply.

### 2. Wire the existing release keystore into the local build

RiverKing already has a release keystore and it must remain the canonical Android signing identity for:

- package name: `com.riverking.mobile`
- same key for itch.io APK and Google Play
- never lose this keystore after release

What still needs to happen on the release machine:

- locate the existing keystore file
- confirm the correct alias inside that keystore
- add the local signing values to `~/.gradle/gradle.properties` or pass them via `-P...`
- back up the keystore before the first public release if that backup policy is not already in place

Expected local properties:

```properties
RIVERKING_SIGNING_STORE_FILE=/absolute/path/to/existing/release.keystore
RIVERKING_SIGNING_STORE_PASSWORD=...
RIVERKING_SIGNING_KEY_ALIAS=...
RIVERKING_SIGNING_KEY_PASSWORD=...
```

Important:

- do not create a second production keystore for the same app
- do not rotate the signing key casually after the first public Android release
- without the same signing lineage, users cannot upgrade cleanly from itch.io to Google Play

### 3. Provide the signing and release values

Before the final production build, these values need to exist:

- `RIVERKING_SIGNING_STORE_FILE`
- `RIVERKING_SIGNING_STORE_PASSWORD`
- `RIVERKING_SIGNING_KEY_ALIAS`
- `RIVERKING_SIGNING_KEY_PASSWORD`
- `RIVERKING_VERSION_CODE`
- `RIVERKING_VERSION_NAME`
- `RIVERKING_ITCH_PROJECT_URL`
- `RIVERKING_PLAY_STORE_URL`

For this repository, the missing local step is specifically the signing block above. The rest of the variables can be finalized once the store pages exist.

### 4. Fill the public policy URLs

These must resolve publicly before store review:

- privacy policy
- support page
- account deletion page

In this repo the intended public endpoints are:

- `/privacy`
- `/support`
- `/account/delete`

### 5. Publish on itch.io first

Recommended order:

1. Upload the signed `directRelease` APK to itch.io.
2. Fill the project text and links.
3. Verify download/install/update from the APK page.
4. Only after that, move to Google Play.

### 6. Complete the Google Play setup

In Play Console you still need to finish:

- app setup
- data safety
- app access
- content rating
- data deletion
- store listing
- testing track setup

If the account is personal, also complete the required closed test period before requesting production access.

Important for RiverKing:

- when Google Play offers Play App Signing setup, keep the same RiverKing signing lineage
- do not let Play use a different production app-signing key if you want seamless upgrades from itch.io installs
- if needed, add a separate upload key later, but the delivered app-signing identity must stay compatible with the existing RiverKing release keystore

## What I Can Do After You Finish The User-Owned Steps

Once you create the accounts and wire the existing release keystore values, I can continue with:

- building the final signed `directRelease` APK
- building the final signed `playRelease` AAB
- verifying package name, versionCode, and signing consistency
- preparing a final release command that you can rerun safely
- drafting reviewer instructions for Google Play
- reviewing the final store text before submission
- checking the final artifact paths and sizes

## Draft Store Copy

### Google Play title

`RiverKing`

### Google Play short description draft

`Fishing progression RPG with clubs, quests, tournaments, and Telegram sync.`

### Google Play full description draft

`RiverKing is a fishing progression game for Android with shared account progress across mobile and Telegram.

Build your collection across locations, rods, bait, and fish species while advancing through quests, achievements, tournaments, and club competition.

In RiverKing you can:

- fish across multiple locations with different gear setups
- unlock rods, bait, and new fish discoveries
- complete daily and weekly quests
- compete in ratings and tournaments
- join clubs and contribute to shared progression
- continue on the same backend account between Android and Telegram

The Android client is connected to the same RiverKing progression systems used by the Telegram experience, so your account, rewards, and progression stay aligned across both surfaces.`

### itch.io short description draft

`RiverKing is a Telegram-connected fishing RPG for Android with quests, tournaments, clubs, and shared progression.`

### itch.io feature bullets draft

- catch fish across multiple locations
- unlock rods, bait, and collection progress
- complete quests and achievements
- climb leaderboards and tournaments
- join clubs and play on the same account across Android and Telegram

## Reviewer Notes Draft For Google Play

Use this as the starting point for the Play review instructions:

`RiverKing is an online fishing progression game. Reviewers can access the Android client through the standard login flow and then navigate the five-tab shell: fishing, leaders, catalog, club, and shop. The app also supports Telegram account linking for shared progression with the Telegram product surface. Public compliance pages are available for privacy, support, and account deletion.`

## Official References

- Google Play target API policy: <https://developer.android.com/google/play/requirements/target-sdk>
- Google Play preview assets: <https://support.google.com/googleplay/android-developer/answer/1078870>
- Google Play create and set up app: <https://support.google.com/googleplay/android-developer/answer/9859152>
- Google Play App Signing: <https://support.google.com/googleplay/android-developer/answer/9842756>
- Google Play personal account testing requirement: <https://support.google.com/googleplay/android-developer/answer/14151465>
- itch.io creator getting started: <https://itch.io/docs/creators/getting-started>
