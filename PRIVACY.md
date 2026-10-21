# Datenschutzerklärung – Quallenkino

*Stand: 16. Oktober 2026* · [English version](PRIVACY.en.md)

Quallenkino („die App“) ist eine App für Android TV und Fire TV, mit der man Medien von einem **eigenen Jellyfin-Server** ansieht. Sie basiert auf der offiziellen Jellyfin-App für Android TV und ist kein offizielles Produkt des Jellyfin-Projekts.

## Kurz gesagt

- Der Entwickler der App **erhält keine Daten** von dir. Es gibt keine Server des Entwicklers, kein Benutzerkonto beim Entwickler, **keine Werbung, kein Tracking und keine Nutzungsanalyse**.
- Die App verbindet sich nur mit dem **Jellyfin-Server, den du selbst einträgst** (dein eigener Server oder der einer Person, der du vertraust). Was dort mit Daten passiert, bestimmt der Betreiber dieses Servers.

## Welche Daten die App an deinen Jellyfin-Server sendet

Wie jede Jellyfin-App sendet Quallenkino an den eingetragenen Server:

- **Anmeldedaten** (Benutzername und Passwort bzw. Quick-Connect-Code) zum Anmelden.
- **Geräteangaben**: Gerätename, eine zufällige Geräte-ID, App-Name und -Version, unterstützte Videoformate (damit der Server passend abspielen kann).
- **Wiedergabe**: was abgespielt wird, Fortschritt, Pausen, „Gesehen“-Status, Favoriten.
- **Einstellungen**, die zwischen deinen Jellyfin-Apps synchronisiert werden (z. B. Startseitenabschnitte).
- **Liste der installierten Apps** auf dem Fernseher (nur Name und Paketname, z. B. „Netflix“), damit du sie mit der Handy-Fernbedienung „Jellyfin Remote“ starten kannst. Das passiert nur, solange die Zeile „Apps“ eingeschaltet ist (Einstellungen → Startseite).
- **Absturzberichte** (technische Fehlerdaten und Protokoll), falls die App abstürzt. Sie gehen ebenfalls nur an deinen Jellyfin-Server und lassen sich in den Einstellungen der App abschalten.

Diese Daten gehen ausschließlich an den von dir eingetragenen Server, nicht an den Entwickler oder an Dritte.

## Daten auf dem Gerät

Die App speichert auf dem Fernseher die Serveradresse, die Anmeldung (ein Zugangstoken, nicht dein Passwort) und deine Einstellungen. Beim Deinstallieren der App werden sie gelöscht. Zugangstokens und der Jellyseerr-API-Schlüssel sind von der Android-Datensicherung (Cloud-Backup, Gerätewechsel) ausgenommen und verlassen das Gerät nicht. Der Token wird an deinen Server im Anfrage-Header geschickt, nicht in Adressen; in Protokollen wird er geschwärzt.

## Netzwerk und Berechtigungen

- **Internet / Netzwerkstatus**: für die Verbindung zu deinem Jellyfin-Server. Die Berechtigung „WLAN-Status lesen“ braucht die App nicht mehr.
- **Server-Suche im Heimnetz**: Beim Einrichten sucht die App im lokalen Netz nach Jellyfin-Servern (Broadcast im eigenen Netzwerk).
- **Heimnetz-Adresse** (optional): Trägst du eine lokale Server-Adresse ein, merkt sich die App eine Kennung des Netzes (Prüfsumme aus Gateway, DNS-Servern und Domain) und benutzt die Adresse nur in diesem Netz. Die Kennung bleibt auf dem Gerät. Mit dem Plugin Local Recommendations ab 12.35 muss der Server zusätzlich nachweisen, dass er den Token dieses Geräts kennt, bevor der Token an die lokale Adresse geht.
- **Suchen und angesehene Titel** (mit dem Plugin Local Recommendations ab 12.36): Deine Suchbegriffe (nach einer kurzen Tipp-Pause) und die Detailseiten von Filmen und Serien, die du öffnest, gehen an deinen Jellyfin-Server und verbessern dort deine Empfehlungen; dazu die Abweichung deiner Uhrzeit von UTC für Vorschläge nach Tageszeit. Der Server speichert das nur, wenn der Admin es eingeschaltet hat und du es nicht abgeschaltet hast – unter Einstellungen → „Suchen und angesehene Titel für Empfehlungen nutzen“ lässt es sich abschalten und das Gespeicherte löschen. Die Speicherdauer (Standard 365 Tage) zeigt die App dort an.
- **Updates**: Höchstens einmal am Tag fragt die App bei GitHub nach einer neueren Version (öffentliche Release-Liste, ohne persönliche Daten). Abschaltbar unter Einstellungen → Updates.
- **Mikrofon** (optional): nur für die Sprachsuche, wenn du sie nutzt. Die Spracherkennung übernimmt der Sprachdienst deines Geräts (z. B. von Amazon oder Google); dafür gelten dessen Datenschutzbestimmungen.
- **Andere Apps starten / Einstellungen öffnen**: Die App kann installierte Apps und die Geräte-Einstellungen öffnen. Sie liest dafür nur die Liste der startbaren Apps, keine Inhalte anderer Apps.

## Dienste Dritter

Die App enthält keine Dienste Dritter für Werbung oder Analyse. Öffnest du einen Trailer oder einen externen Player, startet die jeweilige App (z. B. YouTube); dafür gelten deren Datenschutzbestimmungen.

## Kinder

Die App richtet sich nicht gezielt an Kinder und erhebt selbst keine Daten. Welche Inhalte sichtbar sind, legt der Betreiber des Jellyfin-Servers über die Benutzerkonten fest.

## Deine Rechte

Da der Entwickler keine Daten über dich hat, kann er auch keine Auskunft geben oder Daten löschen. Für Daten auf einem Jellyfin-Server wende dich an dessen Betreiber. Deine Daten auf dem Fernseher entfernst du, indem du dich abmeldest oder die App deinstallierst.

## Quellcode und Kontakt

Die App ist Open Source (GPL v2): <https://github.com/DrdotHouse2106/Jellyfin-TV-Client>  
Fragen zum Datenschutz: <https://github.com/DrdotHouse2106/Jellyfin-TV-Client/issues>

Änderungen dieser Erklärung werden in diesem Repository veröffentlicht.
