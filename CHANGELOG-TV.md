# Änderungen – Quallenkino (Jellyfin-TV-Client)

## 1.11.0 – 2026-10-17

- **Top 10 über alle Benutzer:** Die Zeile „Top 10 bei euch“ nimmt jetzt die Zahlen vom Plugin „Local Recommendations“ (ab 12.6.0) – über alle Benutzer des Servers, auch ohne Administrator-Rechte. Ohne das neue Plugin zählt die App wie bisher selbst.
- **Daumen hoch/runter** auf der Detailseite von Filmen und Serien („Gefällt mir“ / „Nichts für mich“). Gespeichert wird in Jellyfin; das Plugin „Local Recommendations“ (ab 12.6.0) nutzt es für die Empfehlungen: „Gefällt mir“ zählt wie ein Favorit, „Nichts für mich“ wird nie empfohlen und macht ähnliche Titel seltener. Nochmal drücken nimmt die Bewertung zurück.
- **Ton & Untertitel in einem Fenster:** Im Player gibt es nur noch einen Knopf. Er öffnet zwei Spalten – links Tonspuren, rechts Untertitel – mit Haken bei der aktiven Auswahl.

## 1.10.0 – 2026-10-17

- **Anfragen über Jellyseerr** (standardmäßig aus): Unter Einstellungen → Startseite → Anfragen einschalten und Jellyseerr-Adresse sowie API-Schlüssel eintragen. Dann erscheint links in der Navigation ein „+“. Dort zeigt Quallenkino gerade beliebte Titel, man kann suchen und mit OK einen Film oder eine Serie (alle Staffeln) anfragen. Verfügbare und schon angefragte Titel sind markiert. Die Anfrage läuft unter dem passenden Jellyseerr-Benutzer, wenn es einen zum Jellyfin-Konto gibt.

## 1.9.0 – 2026-10-17

- **Top 10 bei euch** (Test): neue Zeile mit den meistgesehenen Filmen und Serien der letzten 60 Tage, mit großen Zahlen neben den Postern. Als Administrator zählen alle Benutzer des Servers, sonst nur das eigene Konto. Abschaltbar unter Einstellungen → Startseite.
- **Suche schneller:** Ergebnisse erscheinen schon nach 0,35 statt 0,6 Sekunden Tipp-Pause; Filme, Serien und Folgen werden angezeigt, sobald sie da sind.
- Die webOS-Version für LG-Fernseher wurde wieder entfernt; das Release enthält nur noch die Android-App.

## 1.8.10 – 2026-10-17

- webOS (LG): Die Serveradresse lässt sich ohne „https://“ eingeben. Vorher hat das Eingabefeld des Fernsehers „Gib eine URL ein“ gemeldet, bevor die App selbst https/http ausprobieren konnte. „OK“ auf der Bildschirmtastatur verbindet jetzt direkt.

## 1.8.9 – 2026-10-17

- Android und webOS haben jetzt dieselbe Versionsnummer und liegen im selben Release: `quallenkino.apk` (Fire TV / Android TV) und `quallenkino-webos.ipk` (LG webOS), dazu die versionierten Dateien und die .aab.

## 1.8.8 – 2026-10-17

- Einstellungen: Vergleiche mit anderen Streaming-Diensten aus den Texten entfernt („Großer Infobereich“ statt „wie …“).

## 1.8.7 – 2026-10-17

- Startseite: Beim Öffnen ist wieder „Abspielen“ im Banner oben ausgewählt statt „Weiterschauen“.

## 1.8.6 – 2026-10-17

- Autostart: Quallenkino wird nach dem Hochfahren mehrmals verzögert geöffnet (nach 3, 12 und 25 Sekunden) und nach dem Aufwachen aus dem Standby ebenso (1,5, 6 und 12 Sekunden), weil Fire OS seinen Startbildschirm kurz danach noch darüberlegt. Läuft Quallenkino bereits im Vordergrund, passiert nichts. Diagnose per `adb logcat -s QuallenkinoAutostart`.
- APK: Release-Asset `quallenkino.apk` im GitHub-Release v1.8.6.

## 1.8.5 – 2026-10-17

- **Autostart beim Einschalten** (Einstellungen → Darstellung & Bedienung, standardmäßig aus): Quallenkino öffnet sich nach einem Neustart des Geräts und beim Aufwachen aus dem Standby. Läuft die App schon, passiert nichts. Funktioniert bis Android 9 (z. B. Fire TV Stick mit Fire OS 7); ab Android 10 verbietet das System automatische Starts. Quallenkino wird nicht als Startbildschirm eingetragen.
- APK zum Installieren: Release-Asset `quallenkino.apk` (bzw. `quallenkino-v1.8.5-release.apk`) im GitHub-Release v1.8.5.

## 1.8.4 – 2026-10-17

- Startseite: Die Zeile „Live-TV“ mit den Knöpfen Aktuelles/Aufnahmen/Zeitplan/Serien ist standardmäßig aus (wieder einschaltbar unter Einstellungen → Startseite). Das TV-Programm bleibt über das Symbol links erreichbar.
- Beschreibungen: wörtliche „\n“ werden nicht mehr angezeigt.

## 1.8.3 – 2026-10-17

- Profilauswahl: Beim Auswählen eines Profils erscheint „Anmeldung als … “, solange auf den Server gewartet wird.

## 1.8.2 – 2026-10-17

- Profilauswahl ohne Profile: Der Hinweis „Wir konnten keine Konten finden“ liegt nicht mehr über der Kachel „Profil hinzufügen“.
- Server hinzufügen: Hinweis, dass nur die Adresse nötig ist – https/http und Port erkennt die App selbst.

## 1.8.1 – 2026-10-16

- Profilauswahl noch näher an Netflix: schwarzer Hintergrund, „Wer schaut gerade?“ mittig, gedecktere Profilfarben.
- Kachel **„Profil hinzufügen“** mit Plus direkt neben den Profilen.
- Statt der grauen Knöpfe ein schlichter Knopf **„Profile verwalten“**: Profile abmelden oder entfernen und den Server wechseln.

## 1.8.0 – 2026-10-16

- **Profilauswahl wie bei Netflix:** „Wer schaut gerade?“ groß und mittig, Profile als große abgerundete Kacheln in festen Farben mit Anfangsbuchstaben (oder Profilbild, falls in Jellyfin gesetzt). Das ausgewählte Profil bekommt einen weißen Rahmen.

## 1.7.3 – 2026-10-16

- Untertitel: Sprachcodes wie „ger“, „deu“ und „de“ werden jetzt als dieselbe Sprache erkannt. Haben Ton und Untertitel dieselbe Sprache (z. B. deutsche Synchro + deutsche Untertitel), startet der Titel ohne Untertitel – auch bei englisch eingestelltem Gerät. Zusätzlich zählt die bevorzugte Tonsprache aus den Jellyfin-Benutzereinstellungen als eigene Sprache.

## 1.7.2 – 2026-10-16

- Navigation links: „Links“ wechselt erst vom ersten Titel einer Zeile in das Seitenmenü, sonst geht es in der Zeile normal nach links.
- Seitenmenü: Startseite, Suche, Einstellungen usw. lassen sich wieder mit OK öffnen (vorher wurde der zuletzt gewählte Titel geöffnet).

## 1.7.1 – 2026-10-16

- Überspringen wie bei Netflix: Der Knopf zeigt nur „Überspringen“ und füllt sich in 5 Sekunden wie ein Balken, dann wird übersprungen. „Zurück“ bricht ab, OK springt sofort.

## 1.7.0 – 2026-10-16

- **Intelligente Untertitel:** Läuft der Ton auf Deutsch (z. B. deutsche Synchro), startet der Titel ohne Untertitel – nur erzwungene Untertitel (z. B. für fremdsprachige Stellen) bleiben an. Schaltest du Untertitel bei einer Serie aus oder um, gilt das auch für die nächsten Folgen. Abschaltbar unter Einstellungen → Wiedergabe.
- **Intro & Abspann automatisch überspringen:** Wie bei Netflix läuft ein 5-Sekunden-Countdown („Überspringen in 5 s“). Drückst du nicht „Zurück“, wird übersprungen; mit OK sofort. Gilt für Abschnitte, die in den Wiedergabe-Einstellungen auf „Nachfragen“ stehen; abschaltbar unter Einstellungen → Wiedergabe.

## 1.6.0 – 2026-10-16

- **Billboard wie bei Netflix:** Ganz oben wechseln Vorschläge (aus „Local Recommendations“, sonst die neuesten Filme und Serien) alle paar Sekunden, mit Punkten als Anzeige. Darunter die Knöpfe **Abspielen**, **Mehr Infos** und **Trailer** (lokaler Trailer vom Server, sonst YouTube). Beim Start ist „Abspielen“ ausgewählt; aus der ersten Zeile kommt man mit „Hoch“ zurück.
- **Trailer-Vorschau:** Bleibt man etwa 3 Sekunden auf einem Film oder einer Serie, läuft oben statt des Bildes der Trailer (stumm, abschaltbar; Ton zuschaltbar). Funktioniert mit Trailern, die auf dem Server liegen.
- **Persönlicher Bildschirmschoner:** zeigt deine Empfehlungen und angefangene Filme statt zufälliger Titel (abschaltbar unter Darstellung & Bedienung → Bildschirmschoner).
- **Genre-Zeilen:** bis zu 12 Zeilen wie „Action“, „Komödie“ oder „Anime“ mit zufälligen Titeln – bei jedem Öffnen neu gemischt, zum Weiterscrollen wie bei Netflix.

## 1.5.2 – 2026-10-16

- Netflix-Ansicht: Die ausgewählte Zeile steht ganz oben, die vorherige Zeile ist nicht mehr angeschnitten zu sehen.
- Kleinere Karten (mehr Titel pro Zeile).
- Beschreibungen ohne Markdown-Links (z. B. „[Name](http://…)“) und HTML-Reste.

## 1.5.1 – 2026-10-16

- Navigation links: alle Symbole (Profil, Suche, Startseite, TV-Programm, Einstellungen) sind mit hoch/runter erreichbar; „Rechts“ führt zurück in die Zeilen.

## 1.5.0 – 2026-10-16

- **Netflix-Look überarbeitet:** Das Bild des ausgewählten Titels steht nur noch oben rechts und läuft nach links und unten in Schwarz aus; der Rest der Startseite ist schwarz. Titel, Eckdaten und Beschreibung stehen links daneben.
- Kleinere Karten im Querformat (Vorschau- bzw. Hintergrundbild) ohne Titel darunter – mehr Titel pro Zeile.
- **Navigation links** wie bei Netflix: Profil, Suche, Startseite, TV-Programm und Einstellungen am linken Rand (mit „Links“ aus der ersten Karte einer Zeile erreichbar); die Leiste oben entfällt in der Netflix-Ansicht, die Uhr bleibt oben rechts.
- **24-Stunden-Uhr:** Uhrzeiten überall als „23:03“ statt „11:03 PM“ (Player „Endet um“, Uhr, TV-Programm), unabhängig von der Systemeinstellung; abschaltbar unter Einstellungen → Startseite → Darstellung.
- **Einstellungen neu geordnet:** Startseite, Wiedergabe, Bibliotheken, Darstellung & Bedienung, Konto & Anmeldung; Selten-Gebrauchtes (erweiterte Wiedergabe, Absturzberichte, Entwickleroptionen) unter „Erweitert“.
- **Player:** dunklere Verläufe oben (Titel, Uhr) und unten (Bedienleiste) – besser lesbar auf hellen Bildern.
- Bei YouTube-artigen Kanälen werden unsinnige Folgennummern (z. B. „S2026:F101698“) nicht mehr angezeigt.

## 1.4.0 – 2026-10-16

- **Netflix-Ansicht:** großer Infobereich oben auf der Startseite mit Logo bzw. Titel, Eckdaten (Jahr, FSK, Laufzeit, Bewertung, Genres) und Beschreibung des ausgewählten Titels; Hintergrundbild links und unten abgedunkelt. Abschaltbar unter Einstellungen → Startseite → Darstellung.
- Zeilen „Apps“ und „Gerät“ sind standardmäßig aus.

## 1.3.1 – 2026-10-16

- **Weitere Abschnitte aus dem Web:** Die Abschnitte des Plugins „Home Screen Sections“, wie in der Weboberfläche eingerichtet (z. B. Genres, Top 10, „Weil du … gesehen hast“, Sammlungen), erscheinen als eigene Zeilen. Abschnitte, die Quallenkino schon selbst hat, werden nicht doppelt gezeigt.
- „Meine Medien“ steht jetzt ganz unten.

## 1.3.0 – 2026-10-16

- **Startseite neu geordnet:** feste Reihenfolge (Weiterschauen, Als Nächstes, Empfehlungen, Heute im TV für dich, Live-TV, Kürzlich hinzugefügt, Meine Medien, Apps, Gerät) mit einem Ein/Aus-Schalter pro Zeile unter Einstellungen → Startseite. Ersetzt die zehn Felder „Startseitenabschnitt 1–10“.

## 1.2.3 – 2026-10-16

- Zeile „Gerät“: Symbole mit Rand statt kachelfüllend.

## 1.2.2 – 2026-10-16

- Startbildschirm: „Willkommen bei Quallenkino!“ und Versionsanzeige „Quallenkino …“ statt „jellyfin-androidtv“.
- Einstellungen → Info zeigt Quallenkino-Version und die zugrunde liegende Jellyfin-Version.

## 1.2.1 – 2026-10-16

- Die Liste der installierten Apps wird nur noch an den eigenen Jellyfin-Server gemeldet, wenn die Zeile „Apps“ eingeschaltet ist.
- Datenschutzerklärung (PRIVACY.md) und Store-Unterlagen.
- Build erzeugt zusätzlich ein App Bundle (.aab) für Google Play.

## 1.2.0 – 2026-10-16

### Neu

- **Neuer Name „Quallenkino“** mit eigenem Symbol, TV-Banner und Logo (Paket-ID bleibt `de.drdothouse.jellyfintv`, Updates funktionieren weiter). In Jellyfin erscheint die App als Client „Quallenkino“.
- Zeile **„Apps“**: installierte Apps des Fernsehers direkt starten.
- Zeile **„Gerät“**: WLAN, Bluetooth & Fernbedienungen, Bild, Ton, alle Einstellungen.
- **Jellyfin Remote** kann Apps starten, Geräte-Einstellungen und Bereiche der App öffnen (`SendString` mit `tvclient:…`); die App meldet ihre App-Liste dafür an den Server.

## 1.1.0 – 2026-10-16

### Neu

- **Fernsteuerung per Handy** (z. B. Jellyfin Remote): Steuerkreuz, OK, Zurück, Bildschirmmenü, Seite hoch/runter, Kanal hoch/runter, Startseite, Suche, Einstellungen und TV-Programm. Text vom Handy öffnet die Suche mit diesem Begriff.

## 1.0.0 – 2026-10-16

Basis: offizielle App v0.19.10

### Neu

- Startseiten-Zeilen **„Empfohlene Filme für dich“**, **„Empfohlene Serien für dich“** und **„Heute im TV für dich“** aus dem Plugin „Local Recommendations“ (ab 12.5.0, `GET /LocalRecs/Recommendations`).
- Einstellung **„Empfehlungen automatisch anzeigen“** (Einstellungen → Startseite); die Zeilen lassen sich auch einzeln als Startseitenabschnitt wählen.
- Ohne Plugin oder ohne Empfehlungen bleiben die Zeilen unsichtbar.
- Eigene Paket-ID `de.drdothouse.jellyfintv` und Name „Jellyfin TV“ (läuft neben der offiziellen App).
- Automatischer Build über GitHub Actions, Releases bei Tags `v*`.
