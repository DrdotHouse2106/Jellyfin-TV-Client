# Quallenkino – for Jellyfin (Jellyfin-TV-Client)

Customized build of the official **Jellyfin app for Android TV and Fire TV** ([jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv), base v0.19.10, GPL v2) with a cinematic home screen and personal recommendations from the [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin) plugin.

**Server plugins** – none is required; without them the app works normally, just without the related extras:

- [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin) **12.9.0 or newer** (recommended): recommendations with reasons, banner suggestions, Top 10 of all users, thumbs up/down, "on TV today for you", live TV cast (with XMLTV)
- [Jellyfin Enhanced](https://github.com/n00bcodr/Jellyfin-Enhanced) with Jellyseerr set up: requests ("+" in the navigation), person pages
- [Home Screen Sections](https://github.com/IAmParadox27/jellyfin-plugin-home-sections): additional home screen sections from the web UI
- [Intro Skipper](https://github.com/intro-skipper/intro-skipper) (or other media segments): "Skip" button for intro and credits

German documentation (main): [README.md](README.md) · Changes: [CHANGELOG-TV.md](CHANGELOG-TV.md)

**Download (always the latest):** https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases/latest/download/quallenkino.apk

## Features

- **Home screen:** side navigation, large info area, rotating suggestions with Play / More info / Trailer, trailer preview in the banner after three seconds (server-side trailers, sound at 75 %, can be turned off; only while the banner is selected), Left/Right switches suggestions, "Trailer" opens a small in-app window (server or YouTube), recommended movies/series and "on TV today for you" rows*, "why recommended" line (e.g. "Because you watched …", from 12.9.0)*, **Top 10** of all users (last 60 days)*, **Worlds** (Marvel, Disney, Pixar, DreamWorks … with their own page, built from studios and collections), genre rows, fixed row order with toggles, 24-hour clock.
- **Detail page:** background trailer starts automatically (server trailer large, otherwise YouTube in a window), German trailers first, otherwise labelled (e.g. EN, OmU).
- **Ratings:** thumbs up / down on the detail page, in the player ("Rate") and on the "Next up" screen (episodes rate their series). Likes count like favorites for the recommendations, dislikes are never recommended*.
- **Requests via Jellyseerr:** if the server plugin "Jellyfin Enhanced" has Jellyseerr set up, a "+" appears in the navigation to search and request titles under your own user, no API key on the TV. Each result has a detail page with cast (person pages), seasons, trailer and similar titles; without a search term it shows "For you", newer titles first. Without the plugin nothing appears (optional: own Jellyseerr address and API key).
- **Player:** auto skip of intro/credits with a 5-second filling button (Back cancels), smart subtitles (off for audio in your language, remembered per series), audio & subtitles in one two-column window, faster search (Down leaves the search field).
- **Live TV:** cast and director on a programme's detail page and in the guide, from the EPG via Local Recommendations 12.7.1 or newer (XMLTV set up). Select on an actor opens their person page (via Jellyseerr).
- **Other:** "Who's watching?" profile picker, personal screensaver (optionally with teasers or trailers), autostart on power-on (up to Android 9, off by default), full remote control from a phone (e.g. Jellyfin Remote).

\* needs the server plugin Local Recommendations (Top 10 of all users and thumbs since 12.6.0).

## Requirements

- Jellyfin 12. Recommended: **Local Recommendations 12.9.0 or newer** (12.6.0 works without live TV cast and recommendation reasons). Optional: **Jellyfin Enhanced** with Jellyseerr, **Home Screen Sections**.
- Android TV / Google TV or Fire TV running Fire OS (Vega OS devices cannot install APKs).

## Notes

- Package ID `de.drdothouse.jellyfintv`, app name "Quallenkino" (own icon and logo) – installs next to the official app. Not an official Jellyfin product.
- Remote control via Jellyfin commands (e.g. Jellyfin Remote): D-pad, select, back, home, search, settings. Extra commands via `SendString`: `tvclient:app:{package}`, `tvclient:settings:{key}`, `tvclient:open:{area}`.
- Builds run in GitHub Actions (`.github/workflows/tv-client-build.yaml`): push to `main` → artifact; "Run workflow" with "release" → GitHub release `v{tv-client-version.txt}`. Signed release builds need the secrets `KEYSTORE` (base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Upstream updates: `git fetch upstream --tags && git merge v0.19.x`.

## Data sources

Data and images: [TMDB](https://www.themoviedb.org). This product uses the TMDB API but is not endorsed or certified by TMDB.

## Support the project

Development costs time and money (servers, test devices, AI tools). If you like the project, a small donation is very welcome:

[![Donate via PayPal](https://img.shields.io/badge/Donate-PayPal-0070ba?logo=paypal&logoColor=white)](https://paypal.me/DrdotHouse)
