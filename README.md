# Quallenkino – für Jellyfin (Jellyfin-TV-Client)

[![Spendier mir einen Kaffee](https://img.shields.io/badge/☕_Spendier_mir_einen_Kaffee-PayPal-0070ba?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/DrdotHouse)

Angepasste Version der offiziellen **Jellyfin-App für Android TV und Fire TV** ([jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv)) mit einer Startseite im Kino-Stil und persönlichen Empfehlungen vom Plugin [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin).

**Plugins auf dem Jellyfin-Server** – keines ist Pflicht, die App läuft auch ohne, nur ohne die jeweiligen Zusatzfunktionen:

| Plugin | Wofür | Ohne Plugin |
|---|---|---|
| [Local Recommendations](https://github.com/DrdotHouse2106/Lokal-Vorschlag-Jellyfin) **ab 12.9.0** (empfohlen) | Empfehlungen „für dich“ mit Begründung, Banner-Vorschläge, Top 10 über alle Benutzer, Daumen hoch/runter, „Heute im TV für dich“, Live-TV-Besetzung (mit XMLTV) | keine Empfehlungs-Zeilen, Top 10 nur aus Sicht der App, Banner zeigt die neuesten Titel |
| [Jellyfin Enhanced](https://github.com/n00bcodr/Jellyfin-Enhanced) mit eingerichtetem Jellyseerr | Anfragen („+“ in der Navigation), Personenseiten, „Für dich“-Vorschläge zum Anfragen | kein „+“ (außer mit eigener Jellyseerr-Adresse und API-Schlüssel) |
| [Intro Skipper](https://github.com/intro-skipper/intro-skipper) (oder andere Mediensegmente) | Knopf „Überspringen“ für Intro und Abspann | kein Überspringen |

## Funktionen

**Startseite**
- Navigation am linken Rand, großer Infobereich mit Logo, Eckdaten und Beschreibung
- Banner mit wechselnden Vorschlägen und den Knöpfen Abspielen / Mehr Infos / Trailer; nach dem Anmelden ist „Abspielen“ ausgewählt. Mit „Links“ und „Rechts“ am Rand der Knöpfe wechselt man zum vorherigen bzw. nächsten Vorschlag
- **Trailer-Vorschau im Banner:** nach drei Sekunden läuft oben der Trailer vom Server (mit Ton bei 75 %, abschaltbar); das Banner wechselt erst nach dem Trailer weiter. Steht die Auswahl in einer Reihe, bleibt es beim Bild
- „Trailer“ öffnet den Trailer in einem kleinen Fenster in der App (vom Server, sonst YouTube)
- Zeilen **Empfohlene Filme / Serien für dich** und **Heute im TV für dich** *
- **Warum empfohlen?** Kurze Begründung im Infobereich und auf der Detailseite, z. B. „Weil du „…“ geschaut hast“ oder „Nächster Teil nach „…““ (Plugin ab 12.9.0) *
- **Filmreihen für dich:** Karten wie im Web mit Bild und Name der Reihe, Fortschrittsbalken und „1 von 3 gesehen · Weiter mit Teil 2“; ein Klick öffnet die Reihen-Seite mit allen Teilen (gesehene mit Haken) und dem Knopf „Weiter mit Teil n“; auf der Detailseite eines Films „Weitere Teile der Reihe“ in Erscheinungsreihenfolge * (ab Plugin 12.45)
- **Momente:** zeitabhängige Zeilen vom Server, z. B. „Läuft heute im TV – hier ohne Werbung“ (Sendungen, die es auch als Datei auf dem Server gibt), „Neu diese Woche“, „Vor dem Kinostart: Hol die Reihe nach“, „Heute vor X Jahren“ – Auswahl und Reihenfolge bestimmt der Server, Begründung im Infobereich * (ab Plugin 12.46)
- **Kommende Folgen auf der Serienseite:** grüner Hinweis „Neu: S02E05“ für heute erschienene Folgen (auf der Karte mit „NEU“ markiert) und blauer Hinweis auf die nächste Folge mit Termin, Sender und Folgentitel – wie im Web * (ab Plugin 12.52, mit Sonarr)
- **Demnächst:** neue Folgen (Sonarr) und Filme (Radarr), die bald kommen, mit Termin als Untertitel; ein Klick auf eine Folge zeigt Staffel/Folge, Tag, Uhrzeit, Sender, Beschreibung und „Zur Serie“ (ab Plugin 12.49), Filme zeigen Titel, Jahr, Termin und Beschreibung * (ab Plugin 12.48)
- **Saison-Zeile:** zur passenden Zeit z. B. „Weihnachtsfilme“, „Halloween“ oder „Ostern“, direkt nach „Als Nächstes“ * (ab Plugin 12.45)
- **Top 10 bei euch:** die meistgesehenen Filme und Serien aller Benutzer der letzten 60 Tage, mit großen Zahlen *
- **Welten:** große Kacheln mit Logo für Marvel, Disney, Pixar, Minions & Co., DreamWorks, DC, Star Wars, Wizarding World, Mittelerde, James Bond, Jurassic Park und Studio Ghibli (Logos und Liste vom Plugin Local Recommendations); jede Welt hat eine eigene Seite mit Filmreihen (z. B. Avengers, Iron Man), Filmen, Serien und Neuem. Zusammengestellt über Studios und Sammlungen, nur Welten mit mindestens 5 Titeln
- Genre-Zeilen mit zufälligen Titeln, feste Reihenfolge mit Ein/Aus-Schaltern
- 24-Stunden-Uhr

**Detailseite**
- **Hintergrund-Trailer:** auf der Detailseite startet der Trailer automatisch im Hintergrund – vom Server groß mit Verlauf, sonst YouTube im Fenster. Deutsche Trailer zuerst, sonst mit Kennzeichnung (z. B. EN, OmU)
- Knopfleiste seitlich scrollbar, damit alle Knöpfe Platz haben

**Bewerten und Anfragen**
- **Daumen hoch / runter** („Gefällt mir“ / „Nichts für mich“) auf der Detailseite, im Player (Knopf „Bewerten“) und auf dem Bildschirm „Nächste Folge“. Folgen bewerten ihre Serie. „Gefällt mir“ zählt für die Empfehlungen wie ein Favorit, „Nichts für mich“ wird nie empfohlen und macht ähnliche Titel seltener *
- **Anfragen über Jellyseerr:** Ist auf dem Server das Plugin „Jellyfin Enhanced“ mit Jellyseerr eingerichtet, erscheint links ein „+“ zum Suchen; jeder Treffer hat eine Detailseite (Inhalt, Besetzung, Staffeln, Trailer, „Das könnte dir auch gefallen“) mit „Anfragen“ bzw. „Staffeln wählen“; OK auf einen Schauspieler öffnet seine Personenseite mit Filmen und Serien. Ohne Suchbegriff zeigt die Seite „Für dich“: passende Titel, die es noch nicht auf dem Server gibt, neuere zuerst – ohne Schlüssel am Fernseher, unter dem eigenen Benutzer. Ohne das Plugin erscheint nichts (optional: eigene Jellyseerr-Adresse und API-Schlüssel)

**Player**
- **Intro und Abspann überspringen:** der Knopf „Überspringen“ füllt sich in 5 Sekunden, dann wird übersprungen; „Zurück“ bricht ab
- **Intelligente Untertitel:** bei Ton in der eigenen Sprache keine Untertitel (außer erzwungene); die Wahl gilt für die nächsten Folgen der Serie
- **Ton & Untertitel in einem Fenster** (zwei Spalten)
- schnellere Suche mit Ergebnissen schon beim Tippen; „Runter“ führt aus dem Suchfeld direkt zu den Ergebnissen

**Live-TV**
- **Besetzung und Regie** auf der Detailseite einer Sendung (Reihe „Besetzung“) und im Programmführer – aus dem EPG über das Plugin „Local Recommendations“ (ab **12.7.1**, mit eingerichtetem XMLTV). OK auf einen Darsteller öffnet seine Personenseite (über Jellyseerr)

**Sonstiges**
- Profilauswahl „Wer schaut gerade?“ mit farbigen Kacheln und „Profile verwalten“ – erscheint bei jedem Start und nach mehr als 10 Minuten im Hintergrund (nicht während einer Wiedergabe). Es wird nie automatisch das letzte Profil angemeldet; die Anmeldung jedes Profils bleibt gespeichert, ein Klick aufs Profilbild genügt (keine PIN). So schaut niemand aus Versehen im Konto eines anderen und die Empfehlungen bleiben persönlich
- Empfehlungen nach Tageszeit und aus Suchen und angesehenen Titeln (Plugin Local Recommendations ab 12.36) – abschaltbar und löschbar unter Einstellungen → „Suchen und angesehene Titel für Empfehlungen nutzen“
- Heimnetz: Mit eingetragener lokaler Server-Adresse (Einstellungen → Heimnetz) spricht die App zu Hause direkt mit dem Server. Die Adresse gilt nur in dem Netz, in dem sie gespeichert wurde – in fremden Netzen bleibt es bei der normalen Adresse
- Profilbild selbst wählen: Figur und Hintergrundfarbe (Katalog und Bild vom Plugin Local Recommendations ab 12.22) – über „Profile verwalten“ oder Einstellungen → Konto
- **Bildschirmschoner** mit persönlichen Empfehlungen statt Zufall (nur Ungesehenes, nichts Angefangenes – wie im Web), auf Wunsch mit **Teasern** (sonst Trailern) vom Server
- **Autostart** beim Einschalten des Geräts (bis Android 9, z. B. Fire TV Stick mit Fire OS 7; standardmäßig aus)
- Steuerung komplett per Handy, z. B. mit [Jellyfin Remote](https://github.com/DrdotHouse2106/Jellyfin-Remote)

\* benötigt das Server-Plugin „Local Recommendations“ (Top 10 über alle Benutzer und Daumen ab **12.6.0**, Live-TV-Besetzung ab **12.7.1**, Begründungen ab **12.9.0**). Ohne Plugin funktioniert die App normal, nur ohne diese Zeilen.

Alles andere ist wie in der offiziellen App: Player, Live-TV, Intro Skipper, Bedienung mit der Fernbedienung.

> English version: [README.en.md](README.en.md) · Original-README: [README.upstream.md](README.upstream.md)

> [!NOTE]
> Basis ist die offizielle App **v0.19.10** (GPL v2). Alle Rechte an der eigentlichen App liegen beim Jellyfin-Team. Die eigenen Änderungen sind mit Kommentaren „Jellyfin-TV-Client“ markiert, neue Dateien liegen in `tvclient/`. Alle Änderungen: [CHANGELOG-TV.md](CHANGELOG-TV.md).

## Download

Immer die neueste Version:

- **Fire TV / Android TV:** https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases/latest/download/quallenkino.apk
- **Amazon Appstore:** Für den Store gibt es ab 1.36.1 eine eigene Fassung `quallenkino-vX-amazon.apk` (im jeweiligen Release) – ohne Selbst-Update und ohne die Berechtigung „Apps installieren“; sie bekommt Updates nur über den Store.

## Voraussetzungen

- Jellyfin-Server 12. Empfohlen: Plugin **Local Recommendations ab 12.9.0** (Empfehlungen mit Begründung, Filmreihen, Top 10, Daumen, Live-TV-Besetzung; ältere Versionen ab 12.6.0 gehen ohne Besetzung und Begründungen). Optional: **Jellyfin Enhanced** mit Jellyseerr (Anfragen). Die früheren Abschnitte aus „Home Screen Sections“ bringt Local Recommendations ab 12.48 selbst mit (Demnächst, Momente …).
- Android TV / Google TV oder **Fire TV mit Fire OS** (Android). Geräte mit *Vega OS* (z. B. einige Fire-TV-Modelle ab 2025) können keine APKs installieren.

Ohne Plugins funktioniert die App ganz normal, nur ohne die jeweiligen Zusatzfunktionen.

## Installation

Die App heißt auf dem Gerät **„Quallenkino“** (eigenes Symbol, eigenes Logo) und hat die eigene Paket-ID `de.drdothouse.jellyfintv`. Sie kann daher **neben** der offiziellen Jellyfin-App installiert sein. In Jellyfin (Dashboard, Jellyfin Remote) erscheint sie als Client „Quallenkino“.

> Name: Für einen Store-Eintrag „Quallenkino – für Jellyfin“. Die Bezeichnung „Jellyfin“ wird nur beschreibend verwendet; die App ist kein offizielles Jellyfin-Produkt.

### Fire TV

1. Einstellungen → Mein Fire TV → Entwickleroptionen → **Apps unbekannter Herkunft** für die App „Downloader“ erlauben (Entwickleroptionen erscheinen nach 7× Klick auf Einstellungen → Mein Fire TV → Info → Gerätename).
2. App **Downloader** aus dem Amazon-Appstore installieren.
3. In Downloader diese Adresse eingeben und installieren (zeigt immer auf die neueste Version):
   `https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases/latest/download/quallenkino.apk`
4. „Quallenkino“ öffnen und mit dem eigenen Jellyfin-Konto anmelden.

### Android TV / Google TV (z. B. waipu.tv 4K Stick, Chromecast, Nvidia Shield)

1. Im Play Store des Sticks die App **Downloader** (von AFTVnews) installieren.
2. Einstellungen → Geräteeinstellungen → Sicherheit & Einschränkungen → **Unbekannte Quellen** → „Downloader“ erlauben.
3. In Downloader diese Adresse eingeben und installieren (zeigt immer auf die neueste Version):
   `https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases/latest/download/quallenkino.apk`

Alternativ per ADB: `adb install quallenkino.apk`.

### Updates

Quallenkino aktualisiert sich selbst – ohne App-Store. Höchstens einmal am Tag sucht die App nach einer neuen Version; ist eine da, fragt sie beim Öffnen bzw. bei der Rückkehr in die App, ob sie installiert werden soll (dieselbe Version höchstens alle 12 Stunden); Android verlangt dabei immer eine Bestätigung – ganz ohne Tastendruck geht ein Update nicht; außerdem erscheint links in der Navigation unter den Einstellungen ein grüner Download-Knopf. Von Hand geht es unter **Einstellungen → Updates → „Nach Updates suchen“**. Bei einer neuen Version erscheinen die Änderungen; „Jetzt installieren“ lädt die APK aus den GitHub-Releases, prüft Prüfsumme und Signatur und startet die Installation. Einstellungen und Anmeldung bleiben erhalten.

Einmalig nötig: Beim ersten Update fragt Quallenkino nach der Erlaubnis **„Unbekannte Apps installieren“** (Fire TV: **„Apps unbekannter Herkunft“**) und öffnet die passende Einstellung. Danach bestätigt man jede Installation mit einem Klick – das verlangt Android.

Alternativ geht ein Update weiterhin von Hand: die neue APK genauso installieren wie beim ersten Mal (solange sie mit demselben Schlüssel signiert ist).

## Startseite

Die Startseite ist im Kino-Stil aufgebaut: **Navigation am linken Rand** (Profil, Suche, Startseite, Anfragen „+“, TV-Programm, Einstellungen – mit „Links“ vom ersten Titel einer Reihe erreichbar), oben ein **großer Infobereich** mit dem Banner bzw. dem ausgewählten Titel mit Logo, Eckdaten und Beschreibung (das Logo nur über einem Hintergrundbild; zeigt das Banner ein Thumb mit eingebautem Titel, entfällt es – wie im Web) (abschaltbar unter Einstellungen → Startseite → Darstellung).

Die Zeilen der Startseite stehen in einer festen Reihenfolge und lassen sich unter **Einstellungen → Startseite** einzeln ein- und ausschalten (gespeichert auf dem Gerät):

1. Weiterschauen
2. Als Nächstes
3. Saison (z. B. Weihnachtsfilme – nur zur passenden Zeit) *
4. Empfohlene Filme für dich *
5. Empfohlene Serien für dich *
6. Momente (Neu diese Woche, Läuft heute im TV, Vor dem Kinostart …) *
7. Demnächst (Sonarr/Radarr) *
8. Filmreihen für dich *
9. Heute im TV für dich * (nur mit Live-TV-Zugriff; Titel und Schalter vom Server wie im Web)
10. Top 10 bei euch *
11. Welten (Marvel, Disney, Pixar …)
12. Live-TV (TV-Programm, Aufnahmen, Läuft gerade; anfangs aus)
13. Kürzlich hinzugefügt
14. Genres (zufällige Titel, bei jedem Öffnen neu gemischt)
15. Weiterhören (anfangs aus)
16. Aktive Aufnahmen (anfangs aus)
17. Apps (anfangs aus)
18. Gerät (anfangs aus)
19. Meine Medien (Bibliotheken; ab 1.36.3 anfangs aus wie im Web – die Bibliotheken sind über die Seitenleiste erreichbar)

\* benötigt das Server-Plugin „Local Recommendations“ (Top 10 über alle Benutzer ab 12.6.0); ohne Plugin bleiben die Zeilen unsichtbar bzw. Top 10 zählt nur, was die App selbst sehen darf. Gesehene Titel verschwinden nach dem Anschauen.

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

## Häufige Probleme

Hürden, auf die wir selbst gestoßen sind – jeweils mit Symptom, Ursache und Lösung.

**Film stockt, Ton nur Stereo, Untertitel ins Bild eingebrannt**
- *Ursache:* „Audioausgabe: Zu Stereo heruntermischen“ oder „Dolby Digital durchreichen: Aus“. Dann darf der Ton nicht direkt laufen, der Server rechnet um. Bild-Untertitel (PGS) lassen sich in den umgerechneten Stream nicht einbetten und werden eingebrannt – dafür muss der Server auch das Video neu kodieren.
- *Lösung:* In den Einstellungen unter Ton „Audioausgabe: Direkt“ und „Dolby Digital durchreichen: An“ (ab 1.30.4 direkt dort; 1.30.5 hat beides einmalig zurückgesetzt).

**HDR-Film ist zu dunkel und blass**
- *Ursache (bis 1.36.8):* Die App spielte HDR direkt ab, sobald der Stick es dekodieren konnte – auch wenn der Fernseher kein HDR kann.
- *Lösung (ab 1.37.0 automatisch):* Die App fragt den Bildschirm, welche HDR-Arten er zeigen kann; den Rest wandelt der Server nach SDR um (dafür muss auf dem Server Tonemapping eingerichtet sein). Von Hand: Schnelleinstellungen → „HDR: Immer umwandeln“.

**Film mit DTS- oder TrueHD-Ton bleibt stumm**
- *Ursache:* Manche Sticks melden DTS-Decoder oder DTS-Durchreichen, geben aber keinen Ton aus (z. B. wenn ihr HDMI-Ausgang fest auf Dolby Digital steht).
- *Lösung (ab 1.30.2 automatisch):* Nimmt der Ausgang DTS an (AV-Receiver), wird durchgereicht; sonst dekodiert die App selbst (eingebautes FFmpeg, 5.1 bleibt erhalten); erst als letzter Ausweg rechnet der Server in Dolby Digital um. Bleibt es trotzdem stumm: Einstellungen → Ton → „DTS und TrueHD durchreichen: Aus“ (dann rechnet immer der Server um).

**Erzwungene Untertitel („Forced“) werden nicht erkannt oder nicht automatisch gewählt**
- *Ursache:* Viele MKV-Dateien nennen die Spur „Forced“, setzen aber das Forced-Flag nicht.
- *Lösung:* Ab 1.30.3 erkennt die App solche Spuren auch am Namen („Forced“, „erzwungen“, „nur fremdsprachige“). Wer es in der Datei korrigieren will: `mkvpropedit datei.mkv --edit track:sN --set flag-forced=1` (N = Nummer der Untertitelspur), danach in Jellyfin die Metadaten des Titels aktualisieren. *Achtung:* Viele Dateien auf einmal zu ändern, löst ein Neueinlesen in Jellyfin und in Sonarr/Radarr aus – besser gestaffelt.

**Schauspielerseite zeigt nur Titel aus der eigenen Bibliothek**
- *Ursache:* Die weiteren Filme und Serien einer Person kommen aus Jellyseerr (Filmografie von TMDb). Fehlt eine Voraussetzung, zeigt die App nur, was auf dem Server liegt.
- *Voraussetzungen:* Quallenkino ab 1.30.1; auf dem Server das Plugin [Jellyfin Enhanced](https://github.com/n00bcodr/Jellyfin-Enhanced) mit eingerichteter Jellyseerr-Anbindung; in der App „Jellyseerr“ eingeschaltet (Einstellungen); die Person hat in Jellyfin eine TMDb-Id (Metadaten aus TMDb – sonst bei der Person „Metadaten aktualisieren“).
- *Ergebnis:* Oben „In deiner Bibliothek“, darunter „Weitere Filme“ und „Weitere Serien“ mit Stand und Anfrage-Knopf.

**Heimnetz-Adresse wird plötzlich nicht mehr genutzt**
- *Ursache:* Hat der Server den Identitätsnachweis einmal erbracht, wird ein späteres „unbekannt“ (z. B. nach Neuinstallation des Plugins) aus Sicherheitsgründen abgelehnt.
- *Lösung:* Einstellungen → Heimnetz → Adresse erneut speichern.

**Wechsel zwischen Amazon-Appstore-Version und direkt installierter APK**
- *Ursache:* Die Fassung aus dem Amazon Appstore ist von Amazon signiert, die APK von GitHub mit dem eigenen Schlüssel. Android behandelt das als verschiedene Herkunft und lehnt die Installation über die jeweils andere ab. Die Store-Fassung bekommt ihre Updates nur über den Store (die Store-Fassung enthält kein Selbst-Update) und kann einige Tage hinter den GitHub-Releases liegen.
- *Lösung:* Für einen Wechsel erst deinstallieren, dann die andere Fassung installieren. **Dabei gehen die Einstellungen der App verloren** (Server, Anmeldung, Heimnetz-Adresse, Ton- und Untertitel-Einstellungen) – danach einmal neu anmelden und einstellen.

**Trailer-Ton läuft weiter, obwohl Quallenkino im Hintergrund ist**
- *Ursache (bis 1.30.7):* Das Banner wechselte auch im Hintergrund weiter (nach HOME oder unter dem System-Bildschirmschoner), und der Trailer des nächsten Titels startete, ohne zu prüfen, ob die App sichtbar ist.
- *Lösung:* Ab 1.30.8 spielen Trailer nur, solange Quallenkino im Vordergrund ist, und setzen beim Zurückkehren fort. Bis zum Update: Quallenkino mit „Zurück“ beenden statt mit HOME verlassen.

**Gerät bleibt auf einer alten Version stehen**
- *Ursache:* Bis 1.30.6 suchte die App nur beim Kaltstart nach Updates – Fernseher-Apps bleiben aber oft tagelang im Speicher. Außerdem braucht jedes Update eine Bestätigung („Jetzt installieren“ und der Android-Installationsdialog). Aus dem Amazon Appstore installierte Fassungen aktualisieren sich nur über den Store.
- *Lösung:* Einmal Einstellungen → Updates → „Nach Updates suchen“ und bestätigen (oder die APK neu installieren). Ab 1.30.7 prüft die App auch bei jeder Rückkehr in die App.

**Update lässt sich nicht installieren**
- *Ursache:* Android fragt einmalig nach der Erlaubnis „Unbekannte Apps installieren“ (Fire TV: „Apps unbekannter Herkunft“) für Quallenkino.
- *Lösung:* Erlaubnis im angezeigten Dialog erteilen. Ist eine Version mit anderem Schlüssel installiert (z. B. selbst gebaut), lehnt Android das Update ab – dann einmal deinstallieren.

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

## Datenquellen

Daten und Bilder: [TMDB](https://www.themoviedb.org). Dieses Produkt nutzt die TMDB-API, ist aber nicht von TMDB unterstützt oder zertifiziert.

## Unterstützen

Die Entwicklung kostet Zeit und Geld (Server, Testgeräte, KI-Werkzeuge). Wenn dir das Projekt gefällt, freue ich mich über eine kleine Spende:

[![Spenden via PayPal](https://img.shields.io/badge/Spenden-PayPal-0070ba?logo=paypal&logoColor=white)](https://paypal.me/DrdotHouse)

### 🎮 Auch eine Zocker-Ader?

Dann ist vielleicht mein anderes Projekt genau das Richtige für dich: **[ZockerDB](https://zockerdb.de)** –
deine Spielesammlung an einem Platz. Spiele, Konsolen und Zubehör erfassen (auch per Barcode-Scan mit
dem Handy), bewerten und verwalten, mit Preisverlauf, Editionen und PAL/USK-Angaben. Direkt auf
[zockerdb.de](https://zockerdb.de) nutzen oder als [ZockerDB Basis](https://github.com/DrdotHouse2106/ZockerDBBasis) auf dem eigenen Server hosten.

## Lizenz

GPL v2, wie die offizielle App (siehe [LICENSE](LICENSE)).
