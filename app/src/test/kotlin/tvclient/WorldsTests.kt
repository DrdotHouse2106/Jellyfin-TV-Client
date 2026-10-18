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
	}

	test("Jede Welt hat einen eigenen Schlüssel") {
		Worlds.definitions.map { it.key }.distinct().size shouldBe Worlds.definitions.size
	}
})
