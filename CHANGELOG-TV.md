# Änderungen – Quallenkino (Jellyfin-TV-Client)

## 1.24.1 – 2026-10-18

- **„Kürzlich hinzugefügt“ einheitlich:** Serien, Staffeln und einzelne neue Folgen erscheinen alle als Hochformat-Poster der Serie. Darunter steht „S3:F5 · Folgentitel“, „Staffel 3“ oder „3 neue Folgen“.
- **Staffelkarten öffnen die Serie:** OK auf eine Staffel außerhalb der Serienseite (z. B. in „Kürzlich hinzugefügt“) öffnet die Serienseite. Von dort geht es zu den Folgen, und BACK führt zurück zur Serie statt zur Startseite.
- **Beschreibung bei Staffeln und Folgen ohne Text:** Der Infobereich zeigt dann die Beschreibung der Serie (häufig bei Folgestaffeln aus AniDB). Über dem Staffelnamen steht der Serienname.

## 1.24.0 – 2026-10-18

- **Heimnetz:** Quallenkino spricht zu Hause direkt mit dem Server statt über die Internet-Adresse – schneller und ohne Umweg über den Internet-Upload. In den Einstellungen unter **„Heimnetz“** lässt sich die lokale Adresse eintragen (z. B. `192.168.1.10`, ohne Port wird 8096 genommen). Ohne Eintrag wird die vom Server gemeldete Adresse versucht, wenn sie im Heimnetz liegt.
- Die Heimadresse wird nur genutzt, wenn sie innerhalb von 1,5 Sekunden antwortet und derselbe Server ist (gleiche Server-Id). Sonst bleibt alles wie bisher. Anmeldung und Konten bleiben unverändert.
- Geprüft wird beim Start, beim Zurückkehren in die App und bei einem Netzwechsel – nie während einer laufenden Wiedergabe.
- Die Einstellungen und „Über“ zeigen, welche Verbindung gerade genutzt wird („Heimnetz“ oder „Internet“).
- Live-TV: OK auf einen Darsteller öffnet jetzt auch dann die Personenseite, wenn es bei TMDB mehrere Personen mit gleichem Namen gibt (z. B. Mark Wahlberg, Kate Mara). Bevorzugt wird die Person aus der eigenen Bibliothek, sonst die bekannteste. Ohne Treffer erscheint „Keine Personenseite gefunden“.
- „Top 10 bei euch“ zählt jetzt die letzten 180 Tage statt 60.

## 1.23.11 – 2026-10-18

- Intelligente Untertitel mit **Originalton**: Es starten die vollen deutschen Untertitel. Vorher kamen nur die erzwungenen, weil der Vorschlag des Servers für den deutschen Standardton berechnet war.

## 1.23.10 – 2026-10-18

- Einstellungsseite: Nach der Auswahl eines Werts bleibt der Fokus auf dem gewählten Knopf (vorher war kein Element mehr fokussiert, die nächste Pfeiltaste sprang in eine falsche Reihe).
- Bildschirmschoner-Zeiten heißen jetzt „1 Min.“, „2,5 Min.“, „5 Min.“, „10 Min.“ (wie in der webOS-App).

## 1.23.9 – 2026-10-18

- **Untertitel beim Start** mit vier Möglichkeiten (wie in der webOS-App): Intelligent (Standard), Immer deutsche, Aus (nur erzwungene Untertitel) oder Wie vom Server.
- Neue Einstellung **„Ton beim Start“**: Deutsch bevorzugen (Standard), Originalton oder Wie vom Server. Eine Tonspur, die du während der Wiedergabe selbst gewählt hast, hat weiterhin Vorrang.
- Die alte Einzeloption „Intelligente Untertitel“ unter Wiedergabe entfällt – die Auswahl liegt jetzt in den Einstellungen auf der ersten Seite.

## 1.23.8 – 2026-10-18

- **Schlanke Einstellungen (wie in der webOS-App):** Das Zahnrad in der Navigation öffnet eine übersichtliche Seite mit großen Auswahlknöpfen – Untertitel beim Start, Trailer-Vorschau und deren Ton, Bildschirmschoner (Zeit, Teaser, Ton der Teaser), Anfragen (Jellyseerr), Konto (Profil wechseln, Abmelden, Server und Konten) und Über. Alles andere liegt unter **„Weitere Einstellungen …“** im bisherigen Menü.

## 1.23.7 – 2026-10-18

- Neue Einstellung **„Ton der Teaser“** im Bildschirmschoner: Stumm (Standard), Leise oder Normal – wie in der webOS-App. Vorher richtete sich der Ton nach der Trailer-Vorschau.

## 1.23.6 – 2026-10-18

- Bildschirmschoner springt nicht mehr an, solange ein Trailer im Hintergrund läuft (Detailseite, Banner, Welten, YouTube-Vorschau). Erst nach dem Trailer beginnt die Wartezeit. Erscheint der Schoner doch, hält der Trailer an – es laufen nie zwei Trailer gleichzeitig.

## 1.23.5 – 2026-10-18

- Bildschirmschoner: **OK öffnet die Detailseite** des gerade gezeigten Titels, jede andere Taste beendet den Schoner (wie in der webOS-App). Hinweis „OK: Details · andere Taste: zurück“ unter dem Titel. Gilt für den Schoner in der App und für den System-Bildschirmschoner (Daydream): dort startet OK Quallenkino direkt mit dem Titel.

## 1.23.4 – 2026-10-18

- Programm-Detailseite: Links steht bei „Regie“ der Regisseur aus dem EPG statt „Unbekannt“.

## 1.23.3 – 2026-10-18

- Live-TV-Besetzung: Darsteller mit Apostroph oder Akzent im Namen öffnen ihre Personenseite (EPG „M’Barek“ = TMDB „M'Barek“).

## 1.23.2 – 2026-10-18

- Startseite: Der Infobereich zeigt auch in den unteren Reihen eine Beschreibung. Liefert eine Reihe sie nicht mit (z. B. Web-Abschnitte), lädt die App sie beim Fokus nach und merkt sie sich.

## 1.23.1 – 2026-10-18

- Programmführer: Im Popup höchstens vier Darsteller, damit „Wechsle zu Kanal“ und das Herz sichtbar bleiben. Wörtliche „\n“ aus dem EPG werden zu Zeilenumbrüchen.
- Trailer-Vorschau (Banner, Welt, Detailseite): Das Video läuft jetzt in einer TextureView. Dadurch liegen die schwarzen Verläufe auch über dem Video, und Logo und Text bleiben lesbar.
- Welt-Seite: Beim Scrollen laufen die Reihen nicht mehr unter den Kopf.

## 1.23.0 – 2026-10-18

- **Schnellerer Start der Startseite:** Alle Zeilen werden gleichzeitig vorbereitet, also Top 10, Welten, Web-Abschnitte und Genres. Bisher lief das nacheinander, und bis dahin war nur das Banner zu sehen.
- Das Banner vom letzten Start wird pro Profil gespeichert und erscheint nach der Profilauswahl sofort. Die frischen Vorschläge werden im Hintergrund geladen und unauffällig übernommen.

## 1.22.6 – 2026-10-18

- Welt-Seite: „Filme“, „Serien“ und „Neu dazugekommen“ erscheinen wieder. Vom Plugin kommen nur die Ids, die Titel lädt die App selbst über Jellyfin. Die Reihen haben mehr Platz, die Überschrift bleibt sichtbar.
- Startseite: Steht die Auswahl auf einer Welt-Kachel, zeigt der Infobereich die Welt: großes Logo, Name, Anzahl der Titel und Hintergrundbild. Die Banner-Knöpfe „Abspielen/Mehr Infos/Trailer“ erscheinen nur noch, solange die Auswahl im Banner ist, in den Reihen werden sie ausgeblendet. Liefert das Plugin einen Titel mit Trailer („TrailerItemId“), läuft nach drei Sekunden dessen Trailer.
- Programmführer: „Regie“ und „Mit“ stehen jetzt vor der Beschreibung. Bei langen Beschreibungen wurden sie bisher abgeschnitten.

## 1.22.5 – 2026-10-18

- Welten: „Neu dazugekommen“ zeigt die zuletzt auf dem Server hinzugefügten Titel der Welt direkt vom Plugin (ab 12.15.1).

## 1.22.4 – 2026-10-18

- Welten: Mit dem Plugin Local Recommendations übernimmt die App dessen Liste, also Reihenfolge, Namen, Anzahl, Logos und Bilder. Neue Welten wie „Bud Spencer & Terence Hill“, „Star Trek“, „Alien & Predator“ oder „Horror-Klassiker“ erscheinen dadurch ohne App-Update. Die Reihen „Filme“, „Serien“ und „Neu dazugekommen“ kommen ab Plugin 12.15 direkt vom Plugin, so wie es die Titel zuordnet: auch über TMDB-Ids, mit Ausschlüssen, also z. B. Loki bei Marvel, aber Indiana Jones nicht bei Star Wars. Ohne Plugin gilt weiter die eigene Liste.

## 1.22.3 – 2026-10-18

- Welt-Seite: Logo und Name stehen fest oben, nur die Reihen darunter scrollen. Vorher schob der Fokus auf die erste Reihe den Namen aus dem Bild.
- Welten-Kacheln: neutral dunkel – Hintergrundbild eines Titels der Welt, schwarz abgedunkelt, ohne farbigen Schleier. Die Farbe der Welt erscheint nur als Rand der ausgewählten Kachel und als schmaler Strich unter dem Namen auf der Welt-Seite.
- Welten-Logos: Logos erscheinen in Originalfarbe (z. B. der rote Marvel-Kasten oder die goldenen Reihen-Logos von fanart.tv). Nur fast schwarze Logos werden weiß eingefärbt.
- Welten: „Harry Potter“ heißt jetzt „Wizarding World“ (mit Phantastische Tierwesen), passend zum Plugin.
- Live-TV-Besetzung: Protokoll in logcat (`adb logcat -s Quallenkino`) mit Adresse, HTTP-Status und Anzahl der Darsteller – zur Fehlersuche im Programmführer.

## 1.22.2 – 2026-10-18

- Startseite: Fokus nach dem Anmelden auf „Abspielen“ – die Ursache war, dass beim Start zuerst die Zeilen den Fokus bekamen und ihn wegen der Sperre nicht mehr abgaben (nur „Hoch“ kam heraus).
- Live-TV: Besetzung und Regie werden direkt beim Plugin abgefragt, ohne vorherige Status-Prüfung (sie blieb im Programmführer leer).
- Welt-Seite: Der Name der Welt steht immer oben (auch unter einem Logo).
- Welten: Logos kommen vom eigenen Server (Plugin Local Recommendations, Feld „LogoUrl“ aus dem Welten-Repo) – in der Kachel und oben auf der Welt-Seite, weiß eingefärbt. Ohne Logo steht der Name da.
- Programmführer: „In 2 Stunden“ wird gerundet statt abgeschnitten (1 h 59 min → „In 2 Stunden“).

## 1.22.1 – 2026-10-18

- Startseite: Nach dem Anmelden hat „Abspielen“ im Banner wirklich den Fokus. Vorher hielt sich die Oberfläche für fokussiert, obwohl Android den Fokus nirgends hatte.
- Startseite: Nach „Zurück“ (z. B. von einer Welt oder aus der Suche) ist der Infobereich nicht mehr leer.
- Suche (auch Anfragen-Suche): „Zurück“ im Suchfeld führt zu den Ergebnissen. Das erste „Zurück“ schließt die Tastatur, das zweite springt zu den Treffern.
- Programmführer: „In 2 Stunden“ stimmt wieder („In 23 Minuten“). Die Startzeit wurde als UTC statt als Ortszeit gerechnet (Fehler der offiziellen App).
- Welten: Kacheln mit abgedunkeltem Hintergrundbild eines Titels der Welt (vom Plugin bzw. aus einer Sammlung), darüber Logo oder Name. Die Welt-Seite zeigt immer den Namen, wenn kein Logo lädt.

## 1.22.0 – 2026-10-18

- **Welten:** Neue Reihe „Welten“ auf der Startseite mit großen Kacheln für Marvel, Disney, Pixar, Minions & Co. (Illumination), DreamWorks, DC, Star Wars, Wizarding World, Mittelerde, James Bond, Jurassic Park und Studio Ghibli – mit dem Logo des Studios, falls der Server eines hat. OK öffnet die Welt-Seite mit eigenem Hintergrund und den Reihen „Neu dazugekommen“, einer Reihe pro Filmreihe (z. B. „Avengers“, „Iron Man“), „Filme“ und „Serien aus der Welt von …“. Die Titel kommen über die Studios und passende Sammlungen; Welten mit weniger als 5 Titeln erscheinen nicht. Abschaltbar unter Einstellungen → Startseite.
- „Für dich in dieser Welt“ vom Plugin Local Recommendations ab 12.10 (sonst ohne diese Reihe).
- Startseite: Nach dem Anmelden bleibt der Infobereich beim Banner, auch wenn die Zeilen beim Laden schon eine Karte auswählen. Die Liste der Zeilen kann den Fokus beim Start nicht mehr ohne sichtbare Auswahl übernehmen; der Schutz hält bis zu 20 Sekunden.
- Startseite: Steht die Auswahl in einer Reihe, zeigt der Infobereich immer die ausgewählte Karte.
- Banner: Der Wechsel zwischen „Abspielen“, „Mehr Infos“ und „Trailer“ startet weder den Trailer neu noch schaltet er den Vorschlag weiter.
- **Trailer läuft nahtlos weiter:** Wer im Banner „Mehr Infos“ öffnet, sieht den Trailer auf der Detailseite an derselben Stelle weiterlaufen, und nach „Zurück“ wieder im Banner (Trailer vom Server).

## 1.21.0 – 2026-10-18

- **Warum empfohlen?** Bei empfohlenen Titeln steht im Infobereich der Startseite und auf der Detailseite (vor den Genres) eine kurze Begründung, z. B. „Weil du „Minions & Monster“ geschaut hast“ oder „Nächster Teil nach „Leberkäsjunkie““. Benötigt Local Recommendations ab 12.9.0; ohne Begründung erscheint nichts.

## 1.20.0 – 2026-10-17

- **Live-TV: Besetzung und Regie.** Die Detailseite einer Sendung zeigt die Reihe „Besetzung“ mit Name und Rolle, die Regie steht in der Überschrift. Im Programmführer stehen „Regie“ und „Mit“ unter der Beschreibung. Die Daten kommen aus dem EPG über das Plugin Local Recommendations (ab 12.7.1, XMLTV eingerichtet). Ohne Daten bleibt die Reihe weg.
- OK auf einen Darsteller öffnet seine Personenseite, wenn Jellyseerr genau eine Person mit diesem Namen findet.

## 1.19.4 – 2026-10-17

- Startseite: Die Trailer-Vorschau läuft nur noch, solange das Banner ausgewählt ist. Steht die Auswahl in einer Reihe (auch nach „Zurück“ von einem Titel), bleibt es beim Bild.
- Startseite: Nach „Zurück“ bleiben Auswahl und angezeigter Titel, wo sie waren – das Banner holt sich den Fokus nur beim ersten Öffnen.
- Suche: „Runter“ führt aus dem Suchfeld zu den Ergebnissen, auch ohne die Suchen-Taste der Tastatur. Schließt man die Tastatur mit „Zurück“, springt die Auswahl zu den Ergebnissen.
- Top 10: Die Rang-Ziffern stehen links neben dem Plakat und sind besser lesbar.
- Detailseite: Die Knopfleiste lässt sich seitlich scrollen, statt den letzten Knopf („Weitere“) zu stauchen. Beschriftungen werden nicht mehr mitten im Wort getrennt.

## 1.19.3 – 2026-10-17

- Startseite: Nach dem Anmelden bleibt die Auswahl wirklich auf „Abspielen“ im Banner – die Zeilen darunter sind bis zum ersten Tastendruck gesperrt und können sich den Fokus nicht mehr holen.
- Banner: Die Knopfzeile hat immer drei Knöpfe („Trailer“ ist ohne Trailer gedimmt). Dadurch springt die Auswahl beim Durchklicken mit „Rechts“ nicht mehr kurz in die Navigation.
- Personenseite/Detailseite: Nach „Zurück“ ist der erste Titel einer Reihe nicht mehr links abgeschnitten.

## 1.19.2 – 2026-10-17

- Personenseite und Anfragen-Detailseite: Mit „Runter“ in eine Reihe (Filme, Serien, Besetzung, „Das könnte dir auch gefallen“) wird der erste Titel ausgewählt, beim Zurückkommen der zuletzt gewählte. Die Reihen scrollen nur so weit wie nötig, statt den ausgewählten Titel in die Mitte zu schieben.

## 1.19.1 – 2026-10-17

- Startseite: Nach dem Anmelden bzw. Öffnen bleibt die Auswahl im Banner auf „Abspielen“, auch wenn die Zeilen danach noch laden (bis man eine Taste drückt).
- Banner: „Rechts“ zum nächsten Vorschlag funktioniert auch, wenn der neue Titel weniger Knöpfe hat (z. B. ohne „Trailer“) – die Auswahl springt nicht mehr in die Navigation.

## 1.19.0 – 2026-10-17

- **Personenseite:** OK auf einen Schauspieler in der Besetzung (Anfragen-Detailseite) öffnet seine Seite mit Foto, Geburtstag und -ort, Biografie (OK klappt sie aus) und den Reihen „Filme“ und „Serien“ – neueste zuerst, mit Status (Verfügbar/Angefragt). OK auf einen Titel öffnet dessen Detailseite.
- Startseite: Nach „Rechts“ im Banner bleibt die Auswahl im Banner (vorher sprang sie in die Navigation). „Rechts“ aus der Navigation führt zurück ins Banner.

## 1.18.1 – 2026-10-17

- Anfragen – Detailseite: Bei langen Titeln ist die Seite beim Öffnen nicht mehr verschoben (Titel höchstens zwei Zeilen, bei Überlänge kleiner; solange die Knöpfe ausgewählt sind, bleibt die Seite ganz oben). Dadurch startet der Trailer oben rechts wieder automatisch.
- Der Trailer oben rechts blendet sich erst aus, wenn man mit „Runter“ zu Inhalt, Staffeln, Besetzung oder Empfehlungen geht, und kommt bei den Knöpfen wieder.
- Nach einer Anfrage steht „Angefragt“ nur noch einmal (in der Eckdaten-Zeile); der Fokus geht auf den Trailer-Knopf.
- Detailseiten: Läuft der Trailer automatisch oben rechts, wird der Knopf „Trailer abspielen“ ausgeblendet. Er erscheint wieder, wenn die Vorschau aus ist oder nicht laufen kann (z. B. YouTube-Fehler, sehr alte Geräte).
- YouTube-Trailer: **deutsche Trailer zuerst** (Sprache „de“ oder „Deutsch/German/Offiziell/Kinostart“ im Namen), Trailer vor Teaser; englische nur, wenn es keine deutschen gibt.
- Anfragen – Detailseite: Die Infospalte ist schmaler, damit die Knöpfe nicht unter den Trailer oben rechts reichen.
- Nicht-deutsche YouTube-Trailer laufen weiterhin automatisch, bekommen aber unter dem Player eine Kennzeichnung: „EN“, „OmU“ (Untertitel bzw. japanisch/koreanisch/chinesisch) oder das Sprachkürzel.
- Anfragen – Raster: Titel bis zu zwei Zeilen mit „…“, das Jahr immer darunter; nur das Plakat wird beim Auswählen größer. Unten mehr Abstand, damit Titel und Jahr der ausgewählten Reihe ganz zu sehen sind.
- Anfragen: Beim Öffnen und beim Zurückkommen von einer Detailseite blitzt die Tastatur nicht mehr kurz auf.

## 1.18.0 – 2026-10-17

- **Teaser im Bildschirmschoner:** Ist „Teaser und Trailer im Bildschirmschoner“ eingeschaltet, nimmt Quallenkino zuerst einen **Teaser** vom Server (kurz, kaum Spoiler) und nur ohne Teaser den Trailer. Teaser werden am Namen („Teaser“) oder an der Länge (bis 100 Sekunden) erkannt – auch unter den Extras, z. B. von Trailarr heruntergeladene.

## 1.17.1 – 2026-10-17

- Anfragen: Beim Öffnen steht die Auswahl auf dem ersten „Für dich“-Titel statt im Suchfeld – die Tastatur geht erst auf, wenn man mit „Hoch“ ins Suchfeld wechselt.
- Anfragen: Ausgewählte Karten werden oben nicht mehr abgeschnitten und behalten runde Ecken samt Rahmen (auch in „Das könnte dir auch gefallen“).
- Anfragen – Detailseite: Nach dem Anfragen verrutscht die Seite nicht mehr. Der Status steht in der Eckdaten-Zeile, an der Stelle des Knopfs steht „Angefragt“. Die Inhaltsangabe lässt rechts Platz für den Trailer; beim Herunterscrollen wird der YouTube-Trailer ausgeblendet, damit nichts darunter rutscht.

## 1.17.0 – 2026-10-17

- **Trailer im Hintergrund der Detailseiten:** Auf der Detailseite eines Films oder einer Serie (Jellyfin und Anfragen) startet nach 2 Sekunden der Trailer oben rechts – „im Hintergrund, aber präsent“. Trailer vom Server ersetzen das Hintergrundbild und laufen weich aus; YouTube-Trailer erscheinen als sauberes Rechteck oben rechts (ohne etwas darüberzulegen). Ton wie eingestellt (Standard an, 75 %). Knöpfe und Fokus bleiben unverändert; beim Verlassen der Seite, beim großen Trailer-Fenster oder bei „Staffeln wählen“ stoppt die Vorschau. Abschaltbar mit „Trailer-Vorschau“ (Einstellungen → Startseite).
- Trailer im Banner und in der Vorschau blenden sanft ein und aus.
- Anfragen: Suchbegriffe mit Jahreszahl („Superman 2025“, „Blade Runner (1982)“) werden verstanden.

## 1.16.0 – 2026-10-17

- **Anfragen – „Für dich“:** Öffnet man das „+“, stehen dort gleich Filme und Serien, die zu deinen Vorlieben passen und die es noch nicht auf dem Server gibt – **neuere zuerst**. Grundlage sind Titel mit „Gefällt mir“, Favoriten und zuletzt Gesehenes (ohne „Nichts für mich“); passende Vorschläge liefert Jellyseerr. Suchen geht wie bisher.

## 1.15.0 – 2026-10-17

- **Trailer im kleinen Fenster:** Der Trailer-Knopf (Banner, Detailseite, Anfragen) öffnet ein Fenster mitten in der App statt einer anderen App. Trailer vom Server (z. B. Trailarr) laufen dort werbefrei; YouTube-Trailer über den offiziellen YouTube-Player (kann Werbung zeigen). „Zurück“ schließt das Fenster. Lässt sich ein Video nicht einbetten oder ist die WebView zu alt, öffnet sich die YouTube-App direkt (ohne Auswahl „Öffnen mit …“).
- **Trailer mit Ton:** Trailer laufen jetzt standardmäßig mit Ton, bei 75 % der eingestellten Lautstärke (Filme und Serien bleiben bei 100 %). Ausschalten: Einstellungen → Startseite → „Trailer-Vorschau mit Ton“. Gilt auch für das Trailer-Fenster und den Bildschirmschoner.
- **Banner:** Läuft im Banner ein Trailer, wird nicht mehr nach 9 Sekunden umgeschaltet, sondern erst am Ende des Trailers. Mit „Rechts“ auf dem letzten Knopf kommt sofort der nächste Vorschlag, mit „Links“ auf „Abspielen“ der vorherige (beim ersten Vorschlag geht „Links“ wie bisher in die Navigation).

## 1.14.2 – 2026-10-17

- Anfragen – Detailseite: „In Jellyfin öffnen“ findet den Titel auch, wenn Jellyseerr eine veraltete ID gespeichert hat (z. B. nach dem Umbenennen eines Ordners) – über Titelsuche und TMDB-ID.
- Mit „Runter“ lässt sich die ganze Seite durchgehen: Inhaltsangabe, Staffeln, Besetzung und „Das könnte dir auch gefallen“ sind auswählbar und scrollen mit.
- Trailer: Liegt der Trailer auf dem Server (z. B. von Trailarr), läuft er direkt in Quallenkino; sonst öffnet sich YouTube direkt statt der Auswahl „Öffnen mit …“.
- Besetzung ohne Foto zeigt Initialen.

## 1.14.1 – 2026-10-17

- Anfragen – Detailseite: unten neue Zeile **„Das könnte dir auch gefallen“** mit passenden Titeln aus Jellyseerr; OK öffnet deren Detailseite.

## 1.14.0 – 2026-10-17

- **Anfragen – Detailseite:** OK auf einem Suchtreffer öffnet jetzt eine Detailseite mit Bild, Plakat, Titel und Originaltitel, Jahr, FSK, Laufzeit bzw. Staffelanzahl, Bewertung, Genres, Regie/Idee, vollständiger Inhaltsangabe, Besetzung und bei Serien den Staffeln mit Status. Knöpfe: „Anfragen“ (Serien: „Alle Staffeln anfragen“ oder „Staffeln wählen“), „In Jellyfin öffnen“ (wenn schon vorhanden) und „Trailer“ (YouTube).
- **Anfragen – Bedienung:** Mit „Runter“ oder der Suchen-Taste geht es aus dem Suchfeld zu den Treffern, die Tastatur schließt sich. OK (Steuerkreuz-Mitte) auf einer Karte funktioniert. Nach „Zurück“ von der Detailseite steht der Fokus wieder auf der zuletzt gewählten Karte.
- Treffer ohne Plakat zeigen den Titel statt eines leeren Felds.

## 1.13.0 – 2026-10-17

- **Bewerten im Player:** neuer Knopf „Bewerten“ (Daumen) – „Gefällt mir“, „Nichts für mich“ oder keine Bewertung. Folgen bewerten ihre Serie.
- **Bewerten vor der nächsten Folge:** Auf dem Bildschirm „Nächste Folge“ stehen zwei Daumen-Knöpfe für die Serie (wählt man sie an, stoppt der Countdown).
- **Trailer im Bildschirmschoner** (Einstellungen → Darstellung & Bedienung → Bildschirmschoner, standardmäßig aus): Titel mit Trailer auf dem Server zeigen den Trailer stumm über dem Bild, danach geht es weiter.
- Bildschirmschoner: Das kurze Quallenkino-Logo am Anfang (2 Sekunden) entfällt; das erste Bild kommt, sobald es geladen ist.
- README, Amazon- und Google-Texte auf den aktuellen Stand gebracht.

## 1.12.0 – 2026-10-17

- **Anfragen über „Jellyfin Enhanced“:** Ist auf dem Server das Plugin „Jellyfin Enhanced“ mit Jellyseerr eingerichtet und dein Konto verknüpft, erscheint links in der Navigation automatisch ein „+“ – ohne Adresse oder API-Schlüssel am Fernseher. Die Anfrage läuft über den Jellyfin-Server unter deinem Benutzer und mit deinen Jellyseerr-Rechten. Auf Servern ohne das Plugin erscheint nichts.
- Titel, die schon in der Bibliothek sind, öffnen mit OK direkt die Detailseite in Jellyfin.
- Klare Meldungen bei „schon angefragt / Limit erreicht“ und „keine Berechtigung“.
- Eigene Jellyseerr-Adresse und API-Schlüssel bleiben als Notlösung ohne Plugin möglich (Einstellungen → Startseite → Anfragen).

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
