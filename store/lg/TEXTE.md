# LG Content Store – Eintrag „Quallenkino“ (webOS)

Texte zum Kopieren für die LG Seller Lounge (seller.lgappstv.com). Die App liegt in `webos/`, die .ipk im GitHub-Release `vX.Y.Z` (zusammen mit der Android-App).

| Feld | Inhalt |
|---|---|
| App-Name | Quallenkino |
| App-ID | de.drdothouse.quallenkino |
| Version | wie die Android-App (siehe tv-client-version.txt) |
| Kategorie | Entertainment (Unterhaltung) |
| Preis | kostenlos |
| Altersfreigabe | ab 0 (die App selbst zeigt keine Inhalte; Inhalte kommen vom eigenen Server) |
| Sprachen | Deutsch, Englisch |
| Icons | `webos/frontend/assets/icon-80.png`, `icon-130.png`, `webos/frontend/submission-icon.png` |
| Hintergrund/Splash | `webos/frontend/assets/splash.png` (1920×1080) |
| Screenshots | 1920×1080 vom LG-Fernseher mit dem Jellyfin-Demo-Server (noch aufzunehmen) |
| Datenschutzerklärung | https://github.com/DrdotHouse2106/Jellyfin-TV-Client/blob/main/PRIVACY.md |
| Quellcode | https://github.com/DrdotHouse2106/Jellyfin-TV-Client/tree/main/webos |

## Deutsch

**Kurzbeschreibung**

Deine Filme und Serien vom eigenen Jellyfin-Server auf dem LG-Fernseher.

**Beschreibung**

Quallenkino bringt deinen eigenen Jellyfin-Medienserver auf den LG-Fernseher. Filme, Serien, Musik und Live-TV laufen direkt von deinem Server – ohne Abo, ohne Werbung, ohne Tracking.

• Einfach verbinden: Adresse eingeben, z. B. jellyfin.beispiel.de – https/http und Port erkennt die App selbst
• Server im Heimnetz werden automatisch gefunden
• Auf Wunsch automatisch verbinden beim Start
• Bedienung mit der LG-Fernbedienung
• Persönliche Empfehlungs-Zeilen, wenn dein Server sie anbietet (z. B. mit dem Plugin „Local Recommendations“)

Voraussetzung: ein Jellyfin-Server (eigener oder von Familie/Freunden). Quallenkino basiert auf der Open-Source-App jellyfin-webos und ist kein offizielles Produkt des Jellyfin-Projekts. Open Source (MPL 2.0).

**Stichwörter**

jellyfin, mediaserver, heimkino, filme, serien, streaming, eigener server

## English

**Short description**

Movies and shows from your own Jellyfin server on your LG TV.

**Description**

Quallenkino brings your own Jellyfin media server to your LG TV. Movies, shows, music and live TV stream straight from your server – no subscription, no ads, no tracking.

• Easy setup: just enter the address, e.g. jellyfin.example.com – https/http and port are detected automatically
• Servers on your home network are found automatically
• Optional automatic connection on launch
• Works with the LG remote
• Personal recommendation rows if your server provides them (e.g. with the "Local Recommendations" plugin)

Requires a Jellyfin server (your own or one from family and friends). Quallenkino is based on the open source app jellyfin-webos and is not an official Jellyfin product. Open source (MPL 2.0).

**Keywords**

jellyfin, media server, home theater, movies, tv shows, streaming, self-hosted

## Hinweis für die Prüfer (Test instructions)

The app requires a Jellyfin media server. For testing please use the public Jellyfin demo server:
Address: demo.jellyfin.org/stable – User: demo – Password: (empty)
