package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.UUID

class WorldsTests : FunSpec({
	val marvel = Worlds.definitions.first { it.key == "marvel" }
	val illumination = Worlds.definitions.first { it.key == "illumination" }

	test("Studios und Sammlungen werden über Wortteile erkannt") {
		Worlds.matches("Marvel Studios", marvel.studios) shouldBe true
		Worlds.matches("Paramount", marvel.studios) shouldBe false
		Worlds.matches("Marvel's The Avengers Filmreihe", marvel.collections) shouldBe true
		Worlds.matches("Ich - Einfach unverbesserlich Filmreihe", illumination.collections) shouldBe true
		Worlds.matches(null, marvel.studios) shouldBe false
	}

	test("Sammlungen in der Reihenfolge der Definition") {
		listOf("Iron Man Filmreihe", "Thor Filmreihe", "Marvel's The Avengers Filmreihe", "Etwas anderes")
			.sortedBy { Worlds.collectionRank(it, marvel.collections) } shouldContainExactly
			listOf("Marvel's The Avengers Filmreihe", "Iron Man Filmreihe", "Thor Filmreihe", "Etwas anderes")
	}

	test("Titel einer Sammlung ohne „Filmreihe“") {
		Worlds.collectionTitle("Iron Man Filmreihe") shouldBe "Iron Man"
		Worlds.collectionTitle("Minions Collection") shouldBe "Minions"
		Worlds.collectionTitle("Filmreihe") shouldBe "Filmreihe"
	}

	test("Ids aus dem Plugin, mit und ohne Bindestriche") {
		val id = UUID.fromString("2a34445f-be38-9aaa-2a27-61e2e0aa0160")
		Worlds.parseIds("""{"Items":["2a34445fbe389aaa2a2761e2e0aa0160"]}""") shouldContainExactly listOf(id)
		Worlds.parseIds("""["2a34445f-be38-9aaa-2a27-61e2e0aa0160"]""") shouldContainExactly listOf(id)
		Worlds.parseIds("""[{"Id":"2a34445fbe389aaa2a2761e2e0aa0160"},"kaputt"]""") shouldContainExactly listOf(id)
	}

	test("Plugin-Welt über den Namen finden") {
		val text = """[{"Id":"w1","Name":"Pixar","ItemCount":7},{"Id":"w2","Name":"Marvel Cinematic Universe","ItemCount":58}]"""
		Worlds.pluginWorldId(text, marvel) shouldBe "w2"
		Worlds.pluginWorldId(text, Worlds.definitions.first { it.key == "pixar" }) shouldBe "w1"
		Worlds.pluginWorldId(text, illumination) shouldBe null
		Worlds.pluginWorldId("""[{"Id":"harry-potter","Name":"Harry Potter / Wizarding World"}]""", Worlds.definitions.first { it.key == "harrypotter" }) shouldBe "harry-potter"
	}

	test("Plugin-Ids mit Bindestrichen passen zu den eigenen Schlüsseln") {
		val text = """[{"Id":"star-wars","Name":"Star Wars"},{"Id":"studio-ghibli","Name":"Studio Ghibli"},{"Id":"jurassic-park","Name":"Jurassic Park"}]"""
		Worlds.pluginWorldId(text, Worlds.definitions.first { it.key == "starwars" }) shouldBe "star-wars"
		Worlds.pluginWorldId(text, Worlds.definitions.first { it.key == "ghibli" }) shouldBe "studio-ghibli"
		Worlds.pluginWorldId(text, Worlds.definitions.first { it.key == "jurassicpark" }) shouldBe "jurassic-park"
		Worlds.normalize("Mittelerde – Die Rückkehr") shouldBe "mittelerdedierueckkehr"
		Worlds.pluginWorldId("""[{"Id":"wizarding-world","Name":"Wizarding World"}]""", Worlds.definitions.first { it.key == "harrypotter" }) shouldBe "wizarding-world"
	}

	test("Logo-Adresse der Plugin-Welt") {
		val text = """[{"Id":"marvel","Name":"Marvel","LogoUrl":"/LocalRecs/Worlds/marvel/Logo"},{"Id":"pixar","Name":"Pixar"}]"""
		Worlds.pluginField(text, marvel, "LogoUrl") shouldBe "/LocalRecs/Worlds/marvel/Logo"
		Worlds.pluginField(text, Worlds.definitions.first { it.key == "pixar" }, "LogoUrl") shouldBe null
	}

	test("Welten aus der Liste des Plugins, auch unbekannte") {
		val text = """[{"Id":"marvel","Name":"Marvel","ItemCount":58,"Studios":["Marvel Studios"],"Collections":["Avengers Filmreihe"]},{"Id":"bud-spencer-terence-hill","Name":"Bud Spencer & Terence Hill","ItemCount":12,"Studios":[],"Collections":["Bud Spencer"]}]"""
		val worlds = Worlds.pluginDefinitions(text)
		worlds.map { it.first.key } shouldContainExactly listOf("marvel", "budspencerterencehill")
		worlds[0].first.color shouldBe marvel.color
		worlds[0].first.studios shouldContainExactly listOf("marvel studios")
		worlds[1].first.name shouldBe "Bud Spencer & Terence Hill"
		worlds[1].second shouldBe 12
		Worlds.pluginWorldId(text, worlds[1].first) shouldBe "bud-spencer-terence-hill"
	}

	test("Jede Welt hat einen eigenen Schlüssel") {
		Worlds.definitions.map { it.key }.distinct().size shouldBe Worlds.definitions.size
	}
})
