# Quallenkino für webOS (LG-Fernseher)

Quallenkino für LG-Fernseher mit webOS. Die App ist eine schlanke Hülle um die Weboberfläche deines Jellyfin-Servers. Sie basiert auf [jellyfin-webos](https://github.com/jellyfin/jellyfin-webos) und hat Quallenkino-Name, Icon und deutsche Texte.

- **App-ID:** `de.drdothouse.quallenkino`
- **Download (immer die neueste Version):** https://github.com/DrdotHouse2106/Jellyfin-TV-Client/releases/latest/download/quallenkino-webos.ipk
- **Version:** dieselbe wie die Android-App; beide liegen im selben Release `vX.Y.Z`

## Was die webOS-Version kann

- Verbindung zu deinem Jellyfin-Server. Es reicht die Adresse, z. B. `jellyfin.beispiel.de` oder `192.168.1.10`. https/http und die Ports 8096/8920 werden automatisch ausprobiert.
- Jellyfin-Server im Heimnetz werden automatisch gefunden.
- Auf Wunsch verbindet sich die App beim Start automatisch.
- Alles, was die Jellyfin-Weboberfläche deines Servers bietet: Filme, Serien, Musik, Live-TV, Wiedergabe mit der LG-Fernbedienung.
- Empfehlungs-Zeilen aus „Local Recommendations“ und „Home Screen Sections“ erscheinen automatisch, wenn die Plugins auf dem Server installiert sind. Die Oberfläche kommt vom Server.

## Was sie nicht kann

Die Extras der Android-/Fire-TV-Version gibt es hier **nicht**, weil die Oberfläche von der Jellyfin-Weboberfläche des Servers kommt:

- keine Startseite im Kino-Stil mit großem Infobereich, wechselnden Vorschlägen und Seitenleiste
- keine Trailer-Vorschau
- keine intelligenten Untertitel (Untertitel-Einstellungen gelten wie in Jellyfin selbst)
- kein Countdown zum Überspringen von Intro/Abspann (Überspringen geht über die Jellyfin-Einstellungen)
- keine Profilauswahl im Quallenkino-Stil
- keine Steuerung über „Jellyfin Remote“-Menübefehle und kein Autostart

## Installieren (Developer Mode)

1. Auf dem LG-Fernseher die App **„Developer Mode“** aus dem LG Content Store installieren, mit einem LG-Konto anmelden und den Entwicklermodus einschalten. Der Fernseher startet dabei neu.
2. Am Computer die [webOS-CLI](https://webostv.developer.lge.com/develop/tools/cli-installation) installieren: `npm install -g @webosose/ares-cli`
3. Den Fernseher einrichten: `ares-setup-device` (IP-Adresse des Fernsehers, Passphrase aus der Developer-Mode-App)
4. Installieren: `ares-install --device <name> quallenkino-webos.ipk`
5. Starten: `ares-launch --device <name> de.drdothouse.quallenkino`

Der Entwicklermodus läuft nach einiger Zeit ab und muss in der Developer-Mode-App verlängert werden. Sonst wird die App vom Fernseher entfernt.

## Selbst bauen

```
cd webos
npm install -g @webosose/ares-cli
ares-package --no-minify --outdir build/ services frontend
```

Veröffentlicht wird die .ipk vom Workflow „TV-Client / Build“ zusammen mit der Android-App im Release `vX.Y.Z`. Die Version kommt dabei aus `tv-client-version.txt` und wird beim Bauen in `frontend/appinfo.json` übernommen. Der Workflow „webOS / Build“ baut bei Änderungen in `webos/` nur zur Probe.

## Herkunft und Lizenz

Dieser Ordner basiert auf [jellyfin/jellyfin-webos](https://github.com/jellyfin/jellyfin-webos) (Stand Commit `ab47940`) und steht wie das Original unter der **Mozilla Public License 2.0** (siehe `LICENSE`). Die Mitwirkenden des Originals stehen in `CONTRIBUTORS.md`. Geändert wurden App-Name, App-ID, Icons, Texte (Deutsch), Farben und die automatische Erkennung von https/http und Port. Das Quallenkino-Projekt ist kein offizielles Produkt des Jellyfin-Projekts.
