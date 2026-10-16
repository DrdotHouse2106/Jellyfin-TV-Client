# Änderungen – Quallenkino (Jellyfin-TV-Client)

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
