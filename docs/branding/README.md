# Branding Assets

This directory contains the committed RiverKing store assets exported from design tools such as Canva and Figma.
The current softer brand pass can also be regenerated locally:

- `docs/branding/generate_branding_assets.py`

Current release files:

- `android-icon-1024.png` — canonical square app icon for store listings
- `itch-cover-1280x720.png` — primary wide artwork for the itch.io page
- `play-feature-1024x500.png` — Google Play feature graphic

Android launcher/splash assets are synced separately from the chosen store icon into:

- `mobile/android-app/app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`
- `mobile/android-app/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `mobile/android-app/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

When branding changes, replace the files in this directory first, then refresh the Android foreground asset sourced from `android-icon-1024.png`.

To rebuild the committed assets from the local generator:

```bash
python3 docs/branding/generate_branding_assets.py
```
