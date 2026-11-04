package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WorldChronologyTests : FunSpec({
	val definition = Worlds.Definition("star-trek", "Star Trek", 0xFF000000, emptyList(), emptyList())

	test("Chronologie aus der Plugin-Welt lesen, Label darf fehlen") {
		val text = """[{"Id":"star-trek","Name":"Star Trek","Chronology":[
			{"Section":"Hauptzeitlinie"},
			{"ItemId":"c2b157ea3dc84584a30bcb96492eda16","Label":"2256–2258","Seasons":[1,2]},
			{"Section":"Ohne Handlungszeit"},
			{"ItemId":"0f8fad5bd9cb469fa16570867728950e","Label":null,"Seasons":null}]}]"""
		val entries = Worlds.pluginChronology(text, definition)
		entries.size shouldBe 4
		entries[1].caption shouldBe "2256–2258 · Staffel 1–2"
		entries[3].caption shouldBe ""
		val groups = Worlds.chronologyGroups(entries, "Chronologisch")
		groups.map { it.first } shouldBe listOf("Hauptzeitlinie", "Ohne Handlungszeit")
	}

	test("Welt ohne Chronologie") {
		Worlds.pluginChronology("""[{"Id":"star-trek","Name":"Star Trek","Chronology":null}]""", definition) shouldBe emptyList()
	}

	test("Staffeln als Text") {
		Worlds.seasonsText(listOf(3, 4, 5)) shouldBe "Staffel 3–5"
		Worlds.seasonsText(listOf(1, 3)) shouldBe "Staffel 1, 3"
		Worlds.seasonsText(emptyList()) shouldBe null
	}

	test("Fehlende Titel (Plugin 12.60): Karten und Chronologie mit Lücke an der richtigen Stelle") {
		val missing = Worlds.parseMissing(
			"http://srv",
			"""{"Enabled":true,"Items":[{"TmdbId":828023,"MediaType":"movie","Title":"Fehlender Film","PosterUrl":"https://image.tmdb.org/p.jpg","ReleaseText":"Erschienen 2020","ChronologyIndex":2}],
			"Chronology":[{"Index":0,"Section":"Hauptzeitlinie"},{"Index":1,"ItemId":"c2b157ea3dc84584a30bcb96492eda16","Label":"2151"},
			{"Index":2,"Missing":true,"TmdbId":828023,"MediaType":"movie","Label":"um 2160"}],"TotalRecordCount":3}""",
		)!!
		missing.items.single().posterUrl shouldBe "https://image.tmdb.org/p.jpg"
		val groups = Worlds.chronologyGroups(missing.chronology!!, "Chronologisch")
		groups.single().first shouldBe "Hauptzeitlinie"
		groups.single().second.map { it.label } shouldBe listOf("2151", "um 2160")
		groups.single().second.last().missing shouldBe true
		val section = Worlds.missingSection("Noch nicht in der Bibliothek", missing)
		section.gaps!!.single()!!.tmdbId shouldBe 828023
		section.items.single().name shouldBe "Fehlender Film"
		Worlds.parseMissing("http://srv", """{"Enabled":false,"Items":[]}""") shouldBe null
	}
})
