# Quallenkino – for Jellyfin (Jellyfin-TV-Client)

Customized build of the official **Jellyfin app for Android TV and Fire TV** ([jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv), base v0.19.10, GPL v2). In addition it shows the personal recommendations of the [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin) plugin on the home screen: recommended movies, recommended series and "Heute im TV für dich" (live TV programs).

German documentation (main): [README.md](README.md)

## Requirements

- Jellyfin 12 with **Local Recommendations 12.5.0 or newer** (`GET /LocalRecs/Recommendations`). Home Screen Sections is not required.
- Android TV / Google TV or Fire TV running Fire OS (Vega OS devices cannot install APKs).

## Notes

- Package ID `de.drdothouse.jellyfintv`, app name "Quallenkino" (own icon and logo) – installs next to the official app. Not an official Jellyfin product.
- Remote control via Jellyfin commands (e.g. Jellyfin Remote): D-pad, select, back, home, search, settings. Extra commands via `SendString`: `tvclient:app:{package}`, `tvclient:settings:{key}`, `tvclient:open:{area}`.
- Home screen rows "Apps" (installed TV apps) and "Device" (Wi-Fi, Bluetooth, display, sound, settings).
- The rows are added automatically after "Continue watching" / "Next up" (Settings → Home → "Show recommendations automatically"), or can be placed manually as home sections.
- Builds run in GitHub Actions (`.github/workflows/tv-client-build.yaml`): push to `main` → artifact, tag `v*` → GitHub release. Signed release builds need the secrets `KEYSTORE` (base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Upstream updates: `git fetch upstream --tags && git merge v0.19.x`.
