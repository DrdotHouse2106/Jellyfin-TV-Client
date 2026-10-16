# Änderungen – Quallenkino (Jellyfin-TV-Client)

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
