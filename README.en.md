# Jellyfin TV (Jellyfin-TV-Client)

Customized build of the official **Jellyfin app for Android TV and Fire TV** ([jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv), base v0.19.10, GPL v2). In addition it shows the personal recommendations of the [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin) plugin on the home screen: recommended movies, recommended series and "Heute im TV für dich" (live TV programs).

German documentation (main): [README.md](README.md)

## Requirements

- Jellyfin 12 with **Local Recommendations 12.5.0 or newer** (`GET /LocalRecs/Recommendations`). Home Screen Sections is not required.
- Android TV / Google TV or Fire TV running Fire OS (Vega OS devices cannot install APKs).

## Notes

- Package ID `de.drdothouse.jellyfintv`, app name "Jellyfin TV" – installs next to the official app.
- The rows are added automatically after "Continue watching" / "Next up" (Settings → Home → "Show recommendations automatically"), or can be placed manually as home sections.
- Builds run in GitHub Actions (`.github/workflows/tv-client-build.yaml`): push to `main` → artifact, tag `v*` → GitHub release. Signed release builds need the secrets `KEYSTORE` (base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Upstream updates: `git fetch upstream --tags && git merge v0.19.x`.
