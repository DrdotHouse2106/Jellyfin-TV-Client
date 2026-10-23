package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TvClientSectionsTests : FunSpec({
	test("Filmreihen: Teil zum Weiterschauen und Begründung") {
		val series = TvClientSections.parseMovieSeries(
			"""{"Items":[{"Id":"10","Name":"Der Pate – Filmreihe","Total":3,"Watched":1,"Reason":"Weiter mit Teil 2",
			"Next":{"Id":"c2b157ea3dc84584a30bcb96492eda16","Name":"Der Pate 2","Type":"Movie"}},{"Id":"11","Name":"Ohne Next","Reason":"4 Teile"}],"TotalRecordCount":2}"""
		)
		series.size shouldBe 2
		series[0].reason shouldBe "Weiter mit Teil 2"
		series[0].next?.name shouldBe "Der Pate 2"
		series[1].next shouldBe null
	}

	test("Teile einer Reihe in Reihenfolge") {
		val (name, parts) = TvClientSections.parseParts(
			"""{"Id":"10","Name":"Der Pate – Filmreihe","Parts":[{"Id":"c2b157ea3dc84584a30bcb96492eda16","Name":"Der Pate","Type":"Movie"},{"Id":"0f8fad5bd9cb469fa16570867728950e","Name":"Der Pate 2","Type":"Movie"}]}"""
		)
		name shouldBe "Der Pate – Filmreihe"
		parts.map { it.name } shouldBe listOf("Der Pate", "Der Pate 2")
	}

	test("Momente: Zeilen mit Titel, Karten und Begründungen; leere Zeilen fallen weg") {
		val moments = TvClientSections.parseMoments(
			"""{"Rows":[{"Key":"ontv","Title":"Läuft heute im TV – hier ohne Werbung","Items":[{"Id":"c2b157ea3dc84584a30bcb96492eda16","Name":"Film","Type":"Movie"}],
			"Reasons":{"c2b157ea3dc84584a30bcb96492eda16":"Heute 20:15 auf ZDF – hier ohne Werbung"}},{"Key":"leer","Title":"Leer","Items":[]}]}"""
		)
		moments.size shouldBe 1
		moments[0].title shouldBe "Läuft heute im TV – hier ohne Werbung"
		moments[0].reasons["c2b157ea3dc84584a30bcb96492eda16"] shouldBe "Heute 20:15 auf ZDF – hier ohne Werbung"
	}

	test("Filmreihen-Karte: Untertitel wie im Web") {
		TvClientSections.MovieSeries(id = "1", name = "R", reason = null, next = null, total = 4, watched = 2, nextIndex = 2).caption shouldBe "2 von 4 gesehen · Weiter mit Teil 3"
		TvClientSections.MovieSeries(id = "1", name = "R", reason = "4 Teile", next = null, total = 4).caption shouldBe "4 Teile"
		TvClientSections.MovieSeries(id = "1", name = "R", reason = null, next = null, total = 3, watched = 3).caption shouldBe "Alle 3 Teile gesehen"
	}

	test("Demnächst: Folgen mit Serie, Filme mit Bild relativ zum Server") {
		val entries = TvClientSections.parseUpcoming(
			"http://srv:8096/",
			"""{"Items":[{"Kind":"Episode","Title":"Serie","Subtitle":"Fr · S2 E3","Series":{"Id":"c2b157ea3dc84584a30bcb96492eda16","Name":"Serie","Type":"Series"}},
			{"Kind":"Movie","Title":"Film","Subtitle":"Kinostart 12.11.","Movie":{"TmdbId":42,"Year":2026,"Overview":"Text","BackdropUrl":"LocalRecs/Upcoming/Movies/7/Images/Backdrop"}},
			{"Kind":"Movie","Title":"Ohne Bild","Movie":{"RadarrId":8}}],"TotalRecordCount":3}"""
		)
		entries.size shouldBe 3
		entries[0].series?.name shouldBe "Serie"
		entries[1].imageUrl shouldBe "http://srv:8096/LocalRecs/Upcoming/Movies/7/Images/Backdrop"
		entries[1].year shouldBe 2026
		entries[2].imageUrl shouldBe null
	}

	test("Demnächst: Folgen-Ansicht mit Staffel/Folge, Termin in Ortszeit und Sender") {
		val entries = TvClientSections.parseUpcoming(
			null,
			"""{"Items":[{"Kind":"Episode","ReleaseType":"SeasonPremiere","Title":"Serie","Subtitle":"Staffelstart morgen","Date":"2026-10-24T19:00:00Z",
			"Network":"Paramount+","EpisodeOverview":"Es geht weiter.","SeasonNumber":2,"EpisodeNumber":1,"EpisodeTitle":"Neuanfang","MoreEpisodes":2,
			"Series":{"Id":"c2b157ea3dc84584a30bcb96492eda16","Name":"Serie","Type":"Series"}}]}"""
		)
		val episode = entries.single().episode!!
		episode.line shouldBe "S02E01 · Neuanfang"
		episode.whenText(java.time.ZoneId.of("Europe/Berlin")) shouldBe "Samstag, 24.10.2026 · 21:00 Uhr · Paramount+"
		episode.overview shouldBe "Es geht weiter."
		episode.more shouldBe 2
		TvClientSections.Episode(network = "ZDF").whenText() shouldBe "ZDF"
	}

	test("Saison-Zeile: Titel und Karten direkt aus der Antwort, nur wenn aktiv") {
		val seasonal = TvClientSections.parseSeasonal(
			"""{"Active":true,"Key":"halloween","Title":"Halloween","Items":[{"Id":"c2b157ea3dc84584a30bcb96492eda16","Name":"Film","Type":"Movie"}]}"""
		)
		seasonal?.title shouldBe "Halloween"
		seasonal?.items?.map { it.name } shouldBe listOf("Film")
		TvClientSections.parseSeasonal("""{"Active":false,"Title":"Halloween","Items":[]}""") shouldBe null
		TvClientSections.parseSeasonal("""{"Active":true,"Title":"Halloween","Items":[]}""") shouldBe null
	}

	test("Serienseite: nächste Folge mit MoreText, heute neue Folgen") {
		val info = SeriesUpcoming.parse(
			"""{"Next":{"SeasonNumber":3,"EpisodeNumber":1},"Text":"Staffelstart morgen · 19:00 Uhr · Paramount+ · S03E01 Der Anfang","MoreEpisodes":2,
			"MoreText":"+2 weitere bis 07.11.","NewToday":[{"SeasonNumber":2,"EpisodeNumber":5,"ItemId":"C2B157EA3DC84584A30BCB96492EDA16"}],"NewText":"Neu: S02E05"}"""
		)!!
		info.next shouldBe "Staffelstart morgen · 19:00 Uhr · Paramount+ · S03E01 Der Anfang · +2 weitere bis 07.11."
		info.newText shouldBe "Neu: S02E05"
		info.newIds shouldBe setOf("c2b157ea3dc84584a30bcb96492eda16")
		SeriesUpcoming.parse("") shouldBe null
		SeriesUpcoming.parse("""{"Text":""}""") shouldBe null
	}
})
