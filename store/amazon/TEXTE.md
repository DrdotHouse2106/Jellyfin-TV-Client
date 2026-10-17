# Amazon Appstore – Eintrag „Quallenkino“

## Grafiken (Step 3 „Images and videos“)

| Feld bei Amazon | Datei |
|---|---|
| Tablet assets → Icon 512×512 | `icon-512.png` |
| Tablet assets → Icon 114×114 | `icon-114.png` |
| Fire TV → App icon 1280×720 | `firetv-icon-1280x720.png` |
| Fire TV → Background image 1920×1080 | `background-1920x1080.png` |
| Fire TV → Featured content logo 640×260 (optional) | `featured-logo-640x260.png` |
| Fire TV → Featured content background 1920×720 (optional) | `featured-background-1920x720.png` |
| Fire TV → Screenshots 1920×1080 (3–10) | `screenshots/01-…10-*.png` (Demo-Server, eigene Domain geschwärzt) |

## Grunddaten

| Feld | Wert |
|---|---|
| App title | Quallenkino für Jellyfin (Amazon lehnt den Gedankenstrich „–“ ab; falls auch „ü“ abgelehnt wird: Quallenkino for Jellyfin) |
| App SKU | de.drdothouse.jellyfintv |
| Kategorie | Unterhaltung / Entertainment (bzw. „Video“, falls angeboten) |
| Support-Website | https://github.com/DrdotHouse2106/Jellyfin-TV-Client |
| Datenschutzerklärung | https://github.com/DrdotHouse2106/Jellyfin-TV-Client/blob/main/PRIVACY.md |
| Preis | kostenlos |
| Werbung | keine |
| Zielgeräte | nur Fire TV (keine Tablets/Handys) |

## Deutsch

**Kurzbeschreibung**

Deine Filme und Serien vom eigenen Jellyfin-Server – im Netflix-Look, mit persönlichen Empfehlungen und Steuerung per Handy.

**Beschreibung**

Quallenkino bringt deinen eigenen Jellyfin-Medienserver auf den Fernseher. Filme, Serien, Musik und Live-TV laufen direkt von deinem Server – ohne Abo, ohne Werbung, ohne Tracking.

Besonderheiten gegenüber anderen Jellyfin-Apps:
• Persönliche Empfehlungen auf der Startseite: „Empfohlene Filme für dich“, „Empfohlene Serien für dich“ und „Heute im TV für dich“ (mit dem Server-Plugin „Local Recommendations“)
• Komplett per Handy steuerbar, z. B. mit „Jellyfin Remote“: Steuerkreuz, Suche, Bibliotheken, TV-Programm
• Netflix-artige Startseite mit großem Infobereich, wechselnden Vorschlägen und Trailer-Vorschau
• Intelligente Untertitel (aus bei deutschem Ton) und Intro/Abspann automatisch überspringen

Außerdem: Weiterschauen, Als Nächstes, Live-TV mit TV-Programm und Aufnahmen, Untertitel, mehrere Benutzer, Quick Connect.

Voraussetzung: ein Jellyfin-Server (eigener oder von Familie/Freunden). Quallenkino ist eine angepasste Version der offiziellen Jellyfin-App für Android TV und kein offizielles Produkt des Jellyfin-Projekts. Open Source (GPL v2): github.com/DrdotHouse2106/Jellyfin-TV-Client

**Feature-Liste (eine Zeile pro Punkt)**

Netflix-artige Startseite mit großem Infobereich und Trailer-Vorschau
Persönliche Empfehlungen für Filme und Serien
Intelligente Untertitel und Intro/Abspann überspringen
Steuerung per Handy mit Jellyfin Remote
Weiterschauen und Als Nächstes
Live-TV mit TV-Programm und Aufnahmen
Mehrere Benutzer und Quick Connect
Keine Werbung, kein Tracking, Open Source

**Stichwörter**

jellyfin, mediaserver, heimkino, filme, serien, live tv, empfehlungen, streaming, plex alternative

## English

**Short description**

Movies and shows from your own Jellyfin server – with a Netflix-style home screen, personal recommendations and phone remote control.

**Description**

Quallenkino brings your own Jellyfin media server to your TV. Movies, shows, music and live TV stream straight from your server – no subscription, no ads, no tracking.

What makes it different:
• Personal recommendations on the home screen (with the server plugin "Local Recommendations")
• Fully controllable from your phone, e.g. with "Jellyfin Remote"
• Netflix-style home screen with a large info area, rotating suggestions and trailer preview
• Smart subtitles (off for audio in your language) and automatic intro/credits skipping

Also included: continue watching, next up, live TV with guide and recordings, subtitles, multiple users, Quick Connect.

Requires a Jellyfin server (your own or one from family and friends). Quallenkino is a customized version of the official Jellyfin Android TV app and not an official Jellyfin product. Open source (GPL v2): github.com/DrdotHouse2106/Jellyfin-TV-Client

**Feature bullets**

Netflix-style home screen with large info area and trailer preview
Personal recommendations for movies and shows
Smart subtitles and intro/credits skipping
Control everything from your phone with Jellyfin Remote
Continue watching and next up
Live TV with program guide and recordings
Multiple users and Quick Connect
No ads, no tracking, open source

**Keywords**

jellyfin, media server, home theater, movies, tv shows, live tv, recommendations, streaming

## Hinweis für die Prüfer (Testing instructions)

The app requires a Jellyfin media server. For testing please use the public Jellyfin demo server:
Server: https://demo.jellyfin.org/stable – User: demo – Password: (empty)
Recommendation rows only appear on servers with the "Local Recommendations" plugin; all other features work with the demo server.
