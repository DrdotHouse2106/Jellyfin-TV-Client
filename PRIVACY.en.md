# Privacy policy – Quallenkino

*Last updated: 16 October 2026* · [Deutsche Fassung](PRIVACY.md)

Quallenkino ("the app") is an Android TV / Fire TV app for watching media from **your own Jellyfin server**. It is based on the official Jellyfin Android TV app and is not an official Jellyfin product.

## In short

- The developer **does not receive any data** from you. There are no developer servers, no developer account, **no ads, no tracking and no analytics**.
- The app only connects to the **Jellyfin server you enter yourself**. What happens with data there is decided by that server's operator.

## Data sent to your Jellyfin server

- **Sign-in data** (username/password or Quick Connect code).
- **Device information**: device name, a random device ID, app name and version, supported video formats.
- **Playback**: what is played, progress, played status, favorites.
- **Settings** synced between your Jellyfin apps.
- **List of installed apps** on the TV (name and package name only) so they can be launched from the "Jellyfin Remote" phone app – only while the "Apps" row is enabled (Settings → Home).
- **Crash reports** (technical error data and log), sent only to your Jellyfin server; can be disabled in the app settings.

This data is sent only to the server you entered – never to the developer or third parties.

## Data on the device

Server address, sign-in token (not your password) and settings are stored on the TV and removed when the app is uninstalled. Sign-in tokens and the Jellyseerr API key are excluded from Android backups (cloud backup, device transfer). The token is sent to your server in a request header, not in URLs, and is redacted in logs.

## Network and permissions

- **Internet / network state**: connection to your Jellyfin server.
- **Local server discovery**: during setup the app looks for Jellyfin servers on your local network.
- **Home network address** (optional): if you enter a local server address, the app stores a network fingerprint (hash of gateway, DNS servers and domain) and uses the address only in that network. The fingerprint stays on the device. With the Local Recommendations plugin 12.35 or newer, the server must also prove it knows this device's token before the token is sent to the local address.
- **Searches and viewed titles** (with the Local Recommendations plugin 12.36 or newer): your search terms (after a short typing pause) and the movie and series detail pages you open are sent to your Jellyfin server to improve your recommendations, together with your time offset from UTC for time-of-day suggestions. The server only stores this if the admin enabled it and you did not turn it off – under Settings → "Use searches and viewed titles for recommendations" you can turn it off and delete what was stored. The retention period (default 365 days) is shown there.
- **Updates**: at most once a day the app asks GitHub for a newer version (public release list, no personal data). Can be turned off under Settings → Updates.
- **Microphone** (optional): voice search only; speech recognition is handled by your device's speech service (e.g. Amazon or Google) under their privacy policies.
- **Launching other apps / opening settings**: the app reads only the list of launchable apps, never their content.

## Third parties

No third-party advertising or analytics services. Trailers or external players open the respective app (e.g. YouTube), subject to its privacy policy.

## Children

The app is not directed at children and does not collect data itself.

## Your rights

The developer holds no data about you. For data on a Jellyfin server, contact its operator. Sign out or uninstall the app to remove local data.

## Source code and contact

Open source (GPL v2): <https://github.com/DrdotHouse2106/Jellyfin-TV-Client>  
Privacy questions: <https://github.com/DrdotHouse2106/Jellyfin-TV-Client/issues>
