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
})
