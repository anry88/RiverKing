# Branding Assets

This directory contains the committed RiverKing store assets exported from design tools such as Canva and Figma.
The current asset set is based on the approved Canva icon export and matching raster listing compositions.

Current release files:

- `android-icon-1024.png` — canonical square app icon for store listings
- `itch-cover-1280x720.png` — primary wide artwork for the itch.io page
- `play-feature-1024x500.png` — Google Play feature graphic

Android launcher/splash assets are synced separately from the chosen store icon into:

- `mobile/android-app/app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`
- `mobile/android-app/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `mobile/android-app/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

When branding changes, replace the files in this directory first, then refresh the Android foreground asset sourced from `android-icon-1024.png`.
