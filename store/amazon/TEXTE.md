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

Deine Filme und Serien vom eigenen Jellyfin-Server – mit großer Startseite im Kino-Stil, persönlichen Empfehlungen und Steuerung per Handy.

**Beschreibung**

Quallenkino bringt deinen eigenen Jellyfin-Medienserver auf den Fernseher. Filme, Serien, Musik und Live-TV laufen direkt von deinem Server – ohne Abo, ohne Werbung, ohne Tracking.

Besonderheiten gegenüber anderen Jellyfin-Apps:
• Persönliche Empfehlungen auf der Startseite: „Empfohlene Filme für dich“, „Empfohlene Serien für dich“ und „Heute im TV für dich“ (mit dem Server-Plugin „Local Recommendations“)
• Komplett per Handy steuerbar, z. B. mit „Jellyfin Remote“: Steuerkreuz, Suche, Bibliotheken, TV-Programm
• Startseite im Kino-Stil mit großem Infobereich, wechselnden Vorschlägen und Trailer-Vorschau
• Empfehlungen mit Begründung („Weil du … geschaut hast“)
• „Welten“: Marvel, Disney, Pixar & Co. mit eigener Seite und Filmreihen
• „Top 10 bei euch“: die meistgesehenen Filme und Serien aller Benutzer auf deinem Server
• Daumen hoch/runter auf der Detailseite, im Player und vor der nächsten Folge – fließt in die Empfehlungen ein
• Filme und Serien direkt am Fernseher anfragen (Jellyseerr, wenn auf dem Server eingerichtet)
• Intelligente Untertitel (aus bei deutschem Ton), Ton und Untertitel in einem Fenster, Intro/Abspann automatisch überspringen
• Live-TV mit Besetzung und Regie zur Sendung (wenn der Server sie aus dem EPG liefert)
• Bildschirmschoner mit deinen Empfehlungen, auf Wunsch mit Trailern

Außerdem: Weiterschauen, Als Nächstes, Live-TV mit TV-Programm und Aufnahmen, Untertitel, mehrere Benutzer, Quick Connect.

Voraussetzung: ein Jellyfin-Server (eigener oder von Familie/Freunden). Quallenkino ist eine angepasste Version der offiziellen Jellyfin-App für Android TV und kein offizielles Produkt des Jellyfin-Projekts. Open Source (GPL v2): github.com/DrdotHouse2106/Jellyfin-TV-Client

**Feature-Liste (eine Zeile pro Punkt)**

Startseite im Kino-Stil mit großem Infobereich und Trailer-Vorschau
Persönliche Empfehlungen und „Top 10 bei euch“
Daumen hoch/runter für bessere Empfehlungen
Filme und Serien anfragen (Jellyseerr)
Intelligente Untertitel und Intro/Abspann überspringen
Steuerung per Handy mit Jellyfin Remote
Weiterschauen und Als Nächstes
Live-TV mit TV-Programm und Aufnahmen
Mehrere Benutzer und Quick Connect
Keine Werbung, kein Tracking, Open Source

**Stichwörter**

jellyfin, mediaserver, heimkino, filme, serien, empfehlungen, top 10, jellyseerr, streaming

## English

**Short description**

Movies and shows from your own Jellyfin server – with a cinematic home screen, personal recommendations and phone remote control.

**Description**

Quallenkino brings your own Jellyfin media server to your TV. Movies, shows, music and live TV stream straight from your server – no subscription, no ads, no tracking.

What makes it different:
• Personal recommendations on the home screen (with the server plugin "Local Recommendations")
• Fully controllable from your phone, e.g. with "Jellyfin Remote"
• Cinematic home screen with a large info area, rotating suggestions and trailer preview
• Recommendations with a reason ("Because you watched …")
• "Worlds": Marvel, Disney, Pixar and more with their own page and film series
• "Top 10": the most watched movies and shows of all users on your server
• Thumbs up/down on the detail page, in the player and before the next episode – improves your recommendations
• Request movies and shows right from the TV (Jellyseerr, if set up on the server)
• Smart subtitles (off for audio in your language), audio and subtitles in one window, automatic intro/credits skipping
• Live TV with cast and director for each programme (when the server provides them from the EPG)
• Screensaver with your recommendations, optionally with trailers

Also included: continue watching, next up, live TV with guide and recordings, subtitles, multiple users, Quick Connect.

Requires a Jellyfin server (your own or one from family and friends). Quallenkino is a customized version of the official Jellyfin Android TV app and not an official Jellyfin product. Open source (GPL v2): github.com/DrdotHouse2106/Jellyfin-TV-Client

**Feature bullets**

Cinematic home screen with large info area and trailer preview
Personal recommendations and Top 10
Thumbs up/down for better recommendations
Request movies and shows (Jellyseerr)
Smart subtitles and intro/credits skipping
Control everything from your phone with Jellyfin Remote
Continue watching and next up
Live TV with program guide and recordings
Multiple users and Quick Connect
No ads, no tracking, open source

**Keywords**

jellyfin, media server, home theater, movies, tv shows, recommendations, top 10, jellyseerr, streaming

## Hinweis für die Prüfer (Testing instructions)

The app requires a Jellyfin media server. For testing please use the public Jellyfin demo server:
Server: https://demo.jellyfin.org/stable – User: demo – Password: (empty)
Recommendation rows only appear on servers with the "Local Recommendations" plugin; all other features work with the demo server.
