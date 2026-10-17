# Quallenkino – für Jellyfin (Jellyfin-TV-Client)

Angepasste Version der offiziellen **Jellyfin-App für Android TV und Fire TV** ([jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv)) mit einer Startseite im Kino-Stil und persönlichen Empfehlungen vom Plugin [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin).

## Funktionen

**Startseite**
- Navigation am linken Rand, großer Infobereich mit Logo, Eckdaten und Beschreibung
- Banner mit wechselnden Vorschlägen und den Knöpfen Abspielen / Mehr Infos / Trailer; beim Start ist „Abspielen“ ausgewählt
- **Trailer-Vorschau:** bleibt man ein paar Sekunden auf einem Titel, läuft oben stumm der Trailer (nur Trailer, die auf dem Server liegen)
- Zeilen **Empfohlene Filme / Serien für dich** und **Heute im TV für dich** *
- **Top 10 bei euch:** die meistgesehenen Filme und Serien aller Benutzer der letzten 60 Tage, mit großen Zahlen *
- Genre-Zeilen mit zufälligen Titeln, Abschnitte aus „Home Screen Sections“, feste Reihenfolge mit Ein/Aus-Schaltern
- 24-Stunden-Uhr

**Bewerten und Anfragen**
- **Daumen hoch / runter** („Gefällt mir“ / „Nichts für mich“) auf der Detailseite, im Player (Knopf „Bewerten“) und auf dem Bildschirm „Nächste Folge“. Folgen bewerten ihre Serie. „Gefällt mir“ zählt für die Empfehlungen wie ein Favorit, „Nichts für mich“ wird nie empfohlen und macht ähnliche Titel seltener *
- **Anfragen über Jellyseerr:** Ist auf dem Server das Plugin „Jellyfin Enhanced“ mit Jellyseerr eingerichtet, erscheint links ein „+“ zum Suchen; jeder Treffer hat eine Detailseite (Inhalt, Besetzung, Staffeln, Trailer) mit „Anfragen“ bzw. „Staffeln wählen“. Ohne Suchbegriff zeigt die Seite „Für dich“: passende Titel, die es noch nicht auf dem Server gibt, neuere zuerst – ohne Schlüssel am Fernseher, unter dem eigenen Benutzer. Ohne das Plugin erscheint nichts (optional: eigene Jellyseerr-Adresse und API-Schlüssel)

**Player**
- **Intro und Abspann überspringen:** der Knopf „Überspringen“ füllt sich in 5 Sekunden, dann wird übersprungen; „Zurück“ bricht ab
- **Intelligente Untertitel:** bei Ton in der eigenen Sprache keine Untertitel (außer erzwungene); die Wahl gilt für die nächsten Folgen der Serie
- **Ton & Untertitel in einem Fenster** (zwei Spalten)
- schnellere Suche mit Ergebnissen schon beim Tippen

**Sonstiges**
- Profilauswahl „Wer schaut gerade?“ mit farbigen Kacheln und „Profile verwalten“
- **Bildschirmschoner** mit persönlichen Empfehlungen statt Zufall, auf Wunsch mit **Trailern** (stumm)
- **Autostart** beim Einschalten des Geräts (bis Android 9, z. B. Fire TV Stick mit Fire OS 7; standardmäßig aus)
- Steuerung komplett per Handy, z. B. mit [Jellyfin Remote](https://github.com/DrdotHouse2106/Jellyfin-Remote)

\* benötigt das Server-Plugin „Local Recommendations“ (Top 10 über alle Benutzer und Daumen ab **12.6.0**). Ohne Plugin funktioniert die App normal, nur ohne diese Zeilen.

Alles andere ist wie in der offiziellen App: Player, Live-TV, Intro Skipper, Bedienung mit der Fernbedienung.

> English version: [README.en.md](README.en.md) · Original-README: [README.upstream.md](README.upstream.md)

> [!NOTE]
> Basis ist die offizielle App **v0.19.10** (GPL v2). Alle Rechte an der eigentlichen App liegen beim Jellyfin-Team. Die eigenen Änderungen sind mit Kommentaren „Jellyfin-TV-Client“ markiert, neue Dateien liegen in `tvclient/`. Alle Änderungen: [CHANGELOG-TV.md](CHANGELOG-TV.md).

## Download

Immer die neueste Version:

- **Fire TV / Android TV:** https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases/latest/download/quallenkino.apk

## Voraussetzungen

- Jellyfin-Server 12. Empfohlen: Plugin **Local Recommendations ab 12.6.0** (Empfehlungen, Top 10, Daumen). Optional: **Jellyfin Enhanced** mit Jellyseerr (Anfragen), **Home Screen Sections** (weitere Abschnitte).
- Android TV / Google TV oder **Fire TV mit Fire OS** (Android). Geräte mit *Vega OS* (z. B. einige Fire-TV-Modelle ab 2025) können keine APKs installieren.

Ohne Plugins funktioniert die App ganz normal, nur ohne die jeweiligen Zusatzfunktionen.

## Installation

Die App heißt auf dem Gerät **„Quallenkino“** (eigenes Symbol, eigenes Logo) und hat die eigene Paket-ID `de.drdothouse.jellyfintv`. Sie kann daher **neben** der offiziellen Jellyfin-App installiert sein. In Jellyfin (Dashboard, Jellyfin Remote) erscheint sie als Client „Quallenkino“.

> Name: Für einen Store-Eintrag „Quallenkino – für Jellyfin“. Die Bezeichnung „Jellyfin“ wird nur beschreibend verwendet; die App ist kein offizielles Jellyfin-Produkt.

### Fire TV

1. Einstellungen → Mein Fire TV → Entwickleroptionen → **Apps unbekannter Herkunft** für die App „Downloader“ erlauben (Entwickleroptionen erscheinen nach 7× Klick auf Einstellungen → Mein Fire TV → Info → Gerätename).
2. App **Downloader** aus dem Amazon-Appstore installieren.
3. In Downloader die Adresse der APK aus den [Releases](https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases) eingeben (`quallenkino-v…-release.apk`) und installieren.
4. „Quallenkino“ öffnen und mit dem eigenen Jellyfin-Konto anmelden.

### Android TV / Google TV (z. B. waipu.tv 4K Stick, Chromecast, Nvidia Shield)

1. Im Play Store des Sticks die App **Downloader** (von AFTVnews) installieren.
2. Einstellungen → Geräteeinstellungen → Sicherheit & Einschränkungen → **Unbekannte Quellen** → „Downloader“ erlauben.
3. In Downloader diese Adresse eingeben und installieren (zeigt immer auf die neueste Version):
   `https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases/latest/download/quallenkino.apk`

Alternativ per ADB: `adb install quallenkino.apk`.

### Updates

Es gibt keine automatischen Store-Updates. Für ein Update die neue APK genauso installieren – die Einstellungen und die Anmeldung bleiben erhalten (solange die APK mit demselben Schlüssel signiert ist).

## Startseite

Die Startseite ist im Kino-Stil aufgebaut: **Navigation am linken Rand** (Profil, Suche, Startseite, TV-Programm, Einstellungen – mit „Links“ erreichbar), oben ein **großer Infobereich** den ausgewählten Titel mit Logo, Eckdaten und Beschreibung (abschaltbar unter Einstellungen → Startseite → Darstellung).

Die Zeilen der Startseite stehen in einer festen Reihenfolge und lassen sich unter **Einstellungen → Startseite** einzeln ein- und ausschalten (gespeichert auf dem Gerät):

1. Weiterschauen
2. Als Nächstes
3. Empfohlene Filme für dich *
4. Empfohlene Serien für dich *
5. Heute im TV für dich * (nur mit Live-TV-Zugriff)
6. Top 10 bei euch *
7. Live-TV (TV-Programm, Aufnahmen, Läuft gerade; anfangs aus)
8. Kürzlich hinzugefügt
9. Weitere Abschnitte aus dem Web **
10. Genres (zufällige Titel, bei jedem Öffnen neu gemischt)
11. Weiterhören (anfangs aus)
12. Aktive Aufnahmen (anfangs aus)
13. Apps (anfangs aus)
14. Gerät (anfangs aus)
15. Meine Medien (Bibliotheken)

\* benötigt das Server-Plugin „Local Recommendations“ (Top 10 über alle Benutzer ab 12.6.0); ohne Plugin bleiben die Zeilen unsichtbar bzw. Top 10 zählt nur, was die App selbst sehen darf. Gesehene Titel verschwinden nach dem Anschauen.

\*\* benötigt das Plugin „Home Screen Sections“; es werden die Abschnitte gezeigt, die der Benutzer in der Weboberfläche eingeschaltet hat (in dessen Reihenfolge). Abschnitte, die Quallenkino schon selbst zeigt (Weiterschauen, Als Nächstes, Meine Medien, Live-TV, Empfehlungen), werden übersprungen.

Die normalen Startseiten-Einstellungen der Jellyfin-Weboberfläche („Startseitenabschnitte“) gelten für Quallenkino nicht mehr.

## Bedienung per Handy

Die App lässt sich komplett über Jellyfin fernsteuern, z. B. mit **[Jellyfin Remote](https://github.com/DrdotHouse2106/Jellyfin-Remote)** – ohne Einrichtung auf dem Stick. Einfach in Jellyfin Remote das Gerät „Quallenkino“ wählen.

| Befehl aus Jellyfin Remote | Wirkung in der App |
|---|---|
| Hoch / Runter / Links / Rechts, OK | wie das Steuerkreuz der Fernbedienung (überall: Startseite, Details, Player, Dialoge) |
| Zurück | Zurück-Taste |
| Home | Startseite der App |
| Bildschirmmenü | Menü-Taste (im Live-TV: TV-Programm) |
| Suche / Texteingabe | öffnet die Suche mit dem eingegebenen Begriff |
| Einstellungen, TV-Programm | öffnet die Seite direkt |
| Play/Pause, Spulen, Stopp, Lautstärke, Stumm | wie in der offiziellen App |

Hinweise:
- Die Befehle wirken, solange „Quallenkino“ auf dem Fernseher im Vordergrund ist. Startet man darüber eine andere App (z. B. Netflix), wird diese mit der normalen Fernbedienung bedient.
- Jellyfin Remote zeigt für „Quallenkino“ zusätzlich Knöpfe für die Bereiche der App (Bibliotheken, Live-TV, TV-Programm, Aufnahmen, Suche, Einstellungen), die installierten Apps des Fernsehers und dessen Einstellungen (WLAN, Bluetooth, Bild, Ton).
- Die Fire-TV-App bzw. Google-TV-App auf dem Handy funktioniert natürlich weiterhin.

## Selbst bauen (GitHub Actions)

Gebaut wird automatisch mit dem Workflow [`TV-Client / Build`](.github/workflows/tv-client-build.yaml):

- **Push auf `main`** → APK als Artefakt im Workflow-Lauf (Version aus `tv-client-version.txt`).
- **Tag `v*`** (z. B. `v1.0.1`) → zusätzlich ein GitHub-Release mit der APK.
- **Release ohne Tag:** Actions → „TV-Client / Build“ → *Run workflow* → Haken bei „GitHub-Release … anlegen“ → legt das Release `v{Version aus tv-client-version.txt}` an.

### Signatur einrichten (einmalig)

Damit Updates über die installierte App gespielt werden können, muss jede APK mit **demselben Schlüssel** signiert sein. Dafür im Repository unter *Settings → Secrets and variables → Actions* vier Secrets anlegen:

| Secret | Inhalt |
|---|---|
| `KEYSTORE` | Keystore-Datei als Base64 (eine Zeile) |
| `KEYSTORE_PASSWORD` | Passwort des Keystores |
| `KEY_ALIAS` | Alias des Schlüssels |
| `KEY_PASSWORD` | Passwort des Schlüssels |

Eigenen Schlüssel erzeugen (Java nötig):

```
keytool -genkeypair -keystore jellyfin-tv.jks -storetype PKCS12 -alias jellyfintv -keyalg RSA -keysize 4096 -validity 36500
base64 -w0 jellyfin-tv.jks > KEYSTORE.txt
```

Ohne diese Secrets baut der Workflow nur eine Debug-APK („Quallenkino Debug“), deren Signatur sich bei jedem Lauf ändert. **Den Keystore niemals ins Repository hochladen.**

## Update auf eine neue Version der offiziellen App

Das Repository enthält die komplette Git-Historie der offiziellen App, Updates lassen sich daher einfach einspielen:

```
git remote add upstream https://github.com/jellyfin/jellyfin-androidtv.git
git fetch upstream --tags
git merge v0.19.11          # gewünschte offizielle Version
# Konflikte lösen, dann tv-client-version.txt erhöhen, CHANGELOG-TV.md ergänzen, pushen
```

Die eigenen Änderungen sind bewusst klein gehalten:

| Datei | Änderung |
|---|---|
| `app/build.gradle.kts` | Paket-ID, App-Name, Dateiname der APK |
| `constant/HomeSectionType.kt`, `constant/QueryType.kt` | neue Zeilen-Typen |
| `ui/itemhandling/ItemRowAdapter.java` | neue Abfrage `LocalRecommendations` |
| `ui/home/HomeRowsFragment.kt`, `ui/home/HomeFragmentHelper.kt` | Zeilen einbauen |
| `ui/preference/screen/HomePreferencesScreen.kt`, neu: `tvclient/HomeLayout.kt` | Startseite: feste Reihenfolge mit Ein/Aus-Schaltern |
| neu: `LocalRecommendationsHelper.kt`, `HomeFragmentLocalRecommendationsRow.kt`, `GetLocalRecommendationsRequest.kt`, `strings_localrecs.xml` | Abfrage, Zeile, Texte (de/en) |
| neu: `.github/workflows/tv-client-build.yaml`, `tv-client-version.txt` | Build |
| `di/AppModule.kt`, neu: `src/tvclient/res` | Name „Quallenkino“, Symbol, Banner, Logo |
| `data/eventhandling/SocketHandler.kt`, neu: `RemoteNavigation.kt`, `tvclient/*` | Fernsteuerung, Zeilen „Apps“ und „Gerät“ |

## Apps und Gerät

Am Ende der Startseite stehen zwei Zeilen (abschaltbar unter Einstellungen → Startseite):

- **Apps** – alle installierten Apps des Fernsehers (YouTube, Netflix, Mediatheken …) zum direkten Starten.
- **Gerät** – Abkürzungen zu WLAN, Bluetooth & Fernbedienungen, Bild, Ton und allen Einstellungen (was das Gerät nicht anbietet, wird ausgeblendet).

Damit Jellyfin Remote dieselben Apps als Knöpfe zeigen kann, legt die App die Liste beim Öffnen der Startseite in den Anzeige-Einstellungen des Benutzers auf dem Jellyfin-Server ab (`DisplayPreferences` `tvclient-device-{DeviceId}`, Client `tvclient`). Befehle vom Handy kommen als `SendString`:

| Text | Wirkung |
|---|---|
| `tvclient:app:{paketname}` | App starten, z. B. `tvclient:app:com.netflix.ninja` |
| `tvclient:settings:{wifi\|bluetooth\|display\|sound\|all}` | Einstellungen öffnen |
| `tvclient:open:{home\|search\|guide\|recordings\|schedule\|settings}` | Bereich der App öffnen |

## Geplant

- Optionaler **Startbildschirm-Modus** (Launcher) für Android TV / Google TV. Auf Fire TV lässt Amazon keinen fremden Startbildschirm zu.

## Datenschutz

Siehe [Datenschutzerklärung](PRIVACY.md) ([English](PRIVACY.en.md)). Kurz: keine Werbung, kein Tracking, keine Daten an den Entwickler – die App spricht nur mit dem eigenen Jellyfin-Server.

## Lizenz

GPL v2, wie die offizielle App (siehe [LICENSE](LICENSE)).
