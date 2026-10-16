# Heimkino TV – für Jellyfin (Jellyfin-TV-Client)

Angepasste Version der offiziellen **Jellyfin-App für Android TV und Fire TV** ([jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv)). Sie zeigt auf dem Fernseher zusätzlich die persönlichen Empfehlungen des Plugins [Lokale Empfehlungen](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin):

- **Empfohlene Filme für dich**
- **Empfohlene Serien für dich** (mit echten Serien-Covern)
- **Heute im TV für dich** (passende Sendungen aus dem EPG, mit Sendezeit und Sender)

Alles andere ist wie in der offiziellen App: Player, Untertitel, Live-TV, Intro Skipper, Steuerung per Fernbedienung.

> English version: [README.en.md](README.en.md) · Original-README: [README.upstream.md](README.upstream.md)

> [!NOTE]
> Basis ist die offizielle App **v0.19.10** (GPL v2). Alle Rechte an der eigentlichen App liegen beim Jellyfin-Team. Dieses Repository enthält nur wenige, klar markierte Änderungen (Kommentare mit „Jellyfin-TV-Client“).

## Voraussetzungen

- Jellyfin-Server 12 mit dem Plugin **Local Recommendations ab Version 12.5.0** (stellt `GET /LocalRecs/Recommendations` bereit). „Home Screen Sections“ ist für diese App **nicht** nötig.
- Android TV / Google TV oder **Fire TV mit Fire OS** (Android). Geräte mit *Vega OS* (z. B. einige Fire-TV-Modelle ab 2025) können keine APKs installieren.

Ohne Plugin funktioniert die App ganz normal, nur ohne die Empfehlungs-Zeilen.

## Installation

Die App heißt auf dem Gerät **„Heimkino TV“** (eigenes Symbol, eigenes Logo) und hat die eigene Paket-ID `de.drdothouse.jellyfintv`. Sie kann daher **neben** der offiziellen Jellyfin-App installiert sein. In Jellyfin (Dashboard, Jellyfin Remote) erscheint sie als Client „Heimkino TV“.

> Name: Für einen Store-Eintrag „Heimkino TV – für Jellyfin“. Die Bezeichnung „Jellyfin“ wird nur beschreibend verwendet; die App ist kein offizielles Jellyfin-Produkt.

### Fire TV

1. Einstellungen → Mein Fire TV → Entwickleroptionen → **Apps unbekannter Herkunft** für die App „Downloader“ erlauben (Entwickleroptionen erscheinen nach 7× Klick auf Einstellungen → Mein Fire TV → Info → Gerätename).
2. App **Downloader** aus dem Amazon-Appstore installieren.
3. In Downloader die Adresse der APK aus den [Releases](https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases) eingeben (`heimkino-tv-v…-release.apk`) und installieren.
4. „Heimkino TV“ öffnen und mit dem eigenen Jellyfin-Konto anmelden.

### Android TV / Google TV

Wie oben über „Downloader“ (Play Store) oder per `adb install heimkino-tv-v…-release.apk`.

### Updates

Es gibt keine automatischen Store-Updates. Für ein Update die neue APK genauso installieren – die Einstellungen und die Anmeldung bleiben erhalten (solange die APK mit demselben Schlüssel signiert ist).

## Empfehlungen auf der Startseite

- Standardmäßig erscheinen die drei Zeilen **automatisch** nach „Weiterschauen“ / „Als Nächstes“.
- Einstellungen → Startseite: **„Empfehlungen automatisch anzeigen“** ein-/ausschalten.
- Wer die Reihenfolge selbst festlegen will: unter „Startseitenabschnitte“ einen der neuen Einträge wählen, z. B. „Empfohlene Filme (Lokale Empfehlungen)“. Sobald einer davon gewählt ist, wird nichts mehr automatisch eingefügt.
- „Heute im TV für dich“ erscheint nur für Benutzer mit Live-TV-Zugriff.
- Gesehene Titel verschwinden nach dem Anschauen aus der Zeile.

## Bedienung per Handy

Die App lässt sich komplett über Jellyfin fernsteuern, z. B. mit **[Jellyfin Remote](https://github.com/DrdotHouse2106/Jellyfin-Remote)** – ohne Einrichtung auf dem Stick. Einfach in Jellyfin Remote das Gerät „Heimkino TV“ wählen.

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
- Die Befehle wirken, solange „Heimkino TV“ auf dem Fernseher im Vordergrund ist. Startet man darüber eine andere App (z. B. Netflix), wird diese mit der normalen Fernbedienung bedient.
- Jellyfin Remote zeigt für „Heimkino TV“ zusätzlich Knöpfe für die Bereiche der App (Bibliotheken, Live-TV, TV-Programm, Aufnahmen, Suche, Einstellungen), die installierten Apps des Fernsehers und dessen Einstellungen (WLAN, Bluetooth, Bild, Ton).
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

Ohne diese Secrets baut der Workflow nur eine Debug-APK („Heimkino TV Debug“), deren Signatur sich bei jedem Lauf ändert. **Den Keystore niemals ins Repository hochladen.**

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
| `preference/UserPreferences.kt`, `ui/preference/screen/HomePreferencesScreen.kt` | Einstellung „Empfehlungen automatisch anzeigen“ |
| neu: `LocalRecommendationsHelper.kt`, `HomeFragmentLocalRecommendationsRow.kt`, `GetLocalRecommendationsRequest.kt`, `strings_localrecs.xml` | Abfrage, Zeile, Texte (de/en) |
| neu: `.github/workflows/tv-client-build.yaml`, `tv-client-version.txt` | Build |
| `di/AppModule.kt`, neu: `src/tvclient/res` | Name „Heimkino TV“, Symbol, Banner, Logo |
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

## Lizenz

GPL v2, wie die offizielle App (siehe [LICENSE](LICENSE)).
