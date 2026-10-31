# Quallenkino – for Jellyfin (Jellyfin-TV-Client)

[![Buy me a coffee](https://img.shields.io/badge/☕_Buy_me_a_coffee-PayPal-0070ba?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/DrdotHouse)

Customized build of the official **Jellyfin app for Android TV and Fire TV** ([jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv), base v0.19.10, GPL v2) with a cinematic home screen and personal recommendations from the [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin) plugin.

**Server plugins** – none is required; without them the app works normally, just without the related extras:

- [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin) **12.9.0 or newer** (recommended): recommendations with reasons, banner suggestions, Top 10 of all users, thumbs up/down, "on TV today for you", live TV cast (with XMLTV)
- [Jellyfin Enhanced](https://github.com/n00bcodr/Jellyfin-Enhanced) with Jellyseerr set up: requests ("+" in the navigation), person pages
- [Home Screen Sections](https://github.com/IAmParadox27/jellyfin-plugin-home-sections): additional home screen sections from the web UI
- [Intro Skipper](https://github.com/intro-skipper/intro-skipper) (or other media segments): "Skip" button for intro and credits

German documentation (main): [README.md](README.md) · Changes: [CHANGELOG-TV.md](CHANGELOG-TV.md)

**Download (always the latest):** https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases/latest/download/quallenkino.apk

**Amazon Appstore:** since 1.36.1 there is a separate build `quallenkino-vX-amazon.apk` in each release – without self-update and without the "install apps" permission; it only updates through the store.

## Features

- **Home screen:** side navigation, large info area, rotating suggestions with Play / More info / Trailer, trailer preview in the banner after three seconds (server-side trailers, sound at 75 %, can be turned off; only while the banner is selected), Left/Right switches suggestions, "Trailer" opens a small in-app window (server or YouTube), recommended movies/series and "on TV today for you" rows*, "why recommended" line (e.g. "Because you watched …", from 12.9.0)*, **Top 10** of all users (last 60 days)*, **Worlds** (Marvel, Disney, Pixar, DreamWorks … with their own page, built from studios and collections), genre rows, fixed row order with toggles, 24-hour clock.
- **Detail page:** background trailer starts automatically (server trailer large, otherwise YouTube in a window), German trailers first, otherwise labelled (e.g. EN, OmU).
- **Ratings:** thumbs up / down on the detail page, in the player ("Rate") and on the "Next up" screen (episodes rate their series). Likes count like favorites for the recommendations, dislikes are never recommended*.
- **Requests via Jellyseerr:** if the server plugin "Jellyfin Enhanced" has Jellyseerr set up, a "+" appears in the navigation to search and request titles under your own user, no API key on the TV. Each result has a detail page with cast (person pages), seasons, trailer and similar titles; without a search term it shows "For you", newer titles first. Without the plugin nothing appears (optional: own Jellyseerr address and API key).
- **Player:** auto skip of intro/credits with a 5-second filling button (Back cancels), smart subtitles (off for audio in your language, remembered per series), audio & subtitles in one two-column window, faster search (Down leaves the search field).
- **Live TV:** cast and director on a programme's detail page and in the guide, from the EPG via Local Recommendations 12.7.1 or newer (XMLTV set up). Select on an actor opens their person page (via Jellyseerr).
- **Other:** "Who's watching?" profile picker (shown at every start and after more than 10 minutes in the background, never signs in the last profile automatically; saved sign-ins work with one click, no PIN), personal screensaver (only unwatched, nothing in progress – like Jellyfin Web; optionally with teasers or trailers), autostart on power-on (up to Android 9, off by default), full remote control from a phone (e.g. Jellyfin Remote).

\* needs the server plugin Local Recommendations (Top 10 of all users and thumbs since 12.6.0).

- 1.41.0: ASS/SSA subtitles with all effects via libass (fonts from the file, signs, karaoke); setting "Subtitle effects": Automatic / On / Off; ARM-only APK.
- 1.40.2: ASS/SSA subtitles are shown by the built-in player (no burn-in/transcoding); episodes use the series trailer for the preview.
- 1.40.1: fixes – correct day for "new episode on …", placeholder times, world matching, lost reasons, availability caches.
- New in 1.40.0 (Local Recommendations 12.55+): worlds can be sorted chronologically by story time (e.g. Star Trek), with sections and the story year under each card.
- New in 1.39.0: trailer preview also when the selection rests on a movie or series in a row (setting "Trailer preview in rows").
- New in 1.38.0 (Local Recommendations 12.54+): discover rows "In cinemas soon", "New this year – movies/series", like Jellyfin Web.
- 1.37.1: screensaver and trailer preview use only real trailers (no other extras), clips under 15 s are skipped.
- 1.37.0: HDR is only played directly if the TV can show it (otherwise the server tone-maps to SDR); setting "HDR": Automatic / Always convert / Always direct.
- 1.36.8: OK in the screensaver over an active player only closes the screensaver (no more hang).
- 1.36.7: no screensaver while playback is paused (like Jellyfin Web); stalled playback is detected, restarted once at the same position and logged to the server's client log.
- 1.36.6: air times in the device time zone, "Next: S03E06 on Sun 01.11." in the Upcoming dialog (Local Recommendations 12.52.2+).
- 1.36.5: moments rows show title and reason under each card, like Jellyfin Web.
- 1.36.4: series page without empty future episodes (next episode as a hint instead), German date/time and 24-hour clocks everywhere, dark "Upcoming" dialog with time zone, consistent "FSK 12" ratings.
- 1.36.3: Back returns focus to the last chosen card on person, world and movie series pages; the "My media" row is off by default (like Jellyfin Web).
- 1.36.2: screensaver shows only unwatched titles; "Resume" on the details page fetches the current position right before starting.
- 1.36.1: separate Amazon Appstore build without self-update.
- New in 1.36.0 (Local Recommendations 12.52+): series page shows episodes released today ("New: S02E05", marked "NEW" on their card) and the next episode with date, network and title, like Jellyfin Web.
- 1.35.2: the seasonal row (e.g. "Halloween") works on Android again, same title and position as in Jellyfin Web.
- 1.35.1: no duplicate title in the banner – over a thumb (which usually contains the title) the logo/title is hidden, like Jellyfin Web.
- New in 1.35.0 (Local Recommendations 12.49+): web-style "Upcoming" – an episode opens a view with season/episode, day, time, network, overview and "Go to series".
- New in 1.34.0 (Local Recommendations 12.48+): "Upcoming" row (Sonarr/Radarr); recommendation row titles and the live TV row switch come from the server like in Jellyfin Web. The Home Screen Sections row was removed.
- New in 1.33.0: web-style movie series cards (series image, progress, "x of y watched · continue with part n") opening a series page with all parts and a "Continue with part n" button.
- New in 1.32.0 (Local Recommendations 12.46+): time-based "moments" rows from the server (on TV today – here without ads, new this week, before the cinema release, years ago today …).
- New in 1.31.0 (Local Recommendations 12.45+): "Movie series for you" with the next part to watch, "More parts of the series" on a movie's detail page, and a seasonal row (e.g. Christmas movies) when active.

## Requirements

- Jellyfin 12. Recommended: **Local Recommendations 12.9.0 or newer** (12.6.0 works without live TV cast and recommendation reasons). Optional: **Jellyfin Enhanced** with Jellyseerr, **Home Screen Sections**.
- Android TV / Google TV or Fire TV running Fire OS (Vega OS devices cannot install APKs).

## Notes

- Package ID `de.drdothouse.jellyfintv`, app name "Quallenkino" (own icon and logo) – installs next to the official app. Not an official Jellyfin product.
- Remote control via Jellyfin commands (e.g. Jellyfin Remote): D-pad, select, back, home, search, settings. Extra commands via `SendString`: `tvclient:app:{package}`, `tvclient:settings:{key}`, `tvclient:open:{area}`.
- Builds run in GitHub Actions (`.github/workflows/tv-client-build.yaml`): push to `main` → artifact; "Run workflow" with "release" → GitHub release `v{tv-client-version.txt}`. Signed release builds need the secrets `KEYSTORE` (base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Upstream updates: `git fetch upstream --tags && git merge v0.19.x`.

## Troubleshooting

- **Stuttering, stereo-only audio, burned-in subtitles:** "Audio output: Downmix to stereo" or "Bitstream Dolby Digital: off" forces the server to convert the audio; picture subtitles (PGS) are then burned in and the video re-encoded. Set Audio → "Direct" and Dolby Digital "On" (shown in the settings since 1.30.4).
- **No sound with DTS/TrueHD:** since 1.30.2 the app passes DTS through if the output accepts it, otherwise decodes it itself (FFmpeg, keeps 5.1); the server converts only as a last resort. Still silent: Settings → Audio → "Pass through DTS and TrueHD: Off".
- **Forced subtitles not detected:** many MKVs only name the track "Forced" without the flag. Since 1.30.3 the name is enough. To fix the file: `mkvpropedit file.mkv --edit track:sN --set flag-forced=1`, then refresh the item in Jellyfin (stagger bulk changes – they trigger rescans in Jellyfin and Sonarr/Radarr).
- **Actor page shows only titles from your library:** the full filmography comes from Jellyseerr (TMDb). Requires Quallenkino 1.30.1 or newer, the [Jellyfin Enhanced](https://github.com/n00bcodr/Jellyfin-Enhanced) plugin with Jellyseerr configured, "Jellyseerr" enabled in the app settings, and a TMDb id on the person in Jellyfin (refresh the person's metadata if missing).
- **Home address no longer used:** after a plugin reinstall the identity check is rejected for safety – save the address again under Settings → Home network.
- **Switching between the Amazon Appstore version and the GitHub APK:** Amazon signs its version with its own key, so Android refuses to install one over the other. Uninstall first – the app's settings (server, sign-in, home address, audio/subtitle settings) are lost. The store version only updates through the store (the store build contains no self-update).
- **Trailer audio keeps playing in the background (up to 1.30.7):** the billboard kept rotating after HOME and the next trailer started anyway. Fixed in 1.30.8 – trailers only play while the app is in the foreground.
- **Device stuck on an old version:** up to 1.30.6 the app only checked on a cold start, and every update needs a confirmation. Use Settings → Updates → "Check for updates" once; from 1.30.7 the app also checks whenever you return to it.
- **Update won't install:** allow "Install unknown apps" for Quallenkino once; an APK signed with a different key requires uninstalling first.

More details (German): [README.md](README.md#häufige-probleme).

## Data sources

Data and images: [TMDB](https://www.themoviedb.org). This product uses the TMDB API but is not endorsed or certified by TMDB.

## Support the project

Development costs time and money (servers, test devices, AI tools). If you like the project, a small donation is very welcome:

[![Donate via PayPal](https://img.shields.io/badge/Donate-PayPal-0070ba?logo=paypal&logoColor=white)](https://paypal.me/DrdotHouse)

### 🎮 Got a gamer streak too?

Then my other project might be just right for you: **[ZockerDB](https://zockerdb.de)** – your game
collection in one place. Track games, consoles and accessories (including barcode scanning with your
phone), rate and manage them, with price history, editions and PAL/USK details. Use it directly at
[zockerdb.de](https://zockerdb.de) or self-host [ZockerDB Basis](https://github.com/DrdotHouse2106/ZockerDBBasis) on your own server.
