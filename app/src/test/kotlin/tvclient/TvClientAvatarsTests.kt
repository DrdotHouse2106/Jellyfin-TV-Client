package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TvClientAvatarsTests : FunSpec({
	test("Katalog lesen, Bildadressen vollständig") {
		val text = """{
			"Name":"Figuren","License":"Nur privat",
			"Colors":["#e50914","#2e96e0"],
			"Categories":[
				{"Id":"tiere","Name":"Tiere","Icons":[
					{"Id":"tiere/katze","Name":"Katze","Fit":"contain","ImageUrl":"LocalRecs/Avatars/Icons/tiere/katze"},
					{"Id":"tiere/hund","Name":"Hund","Fit":"cover","ImageUrl":"https://cdn/hund.png"}
				]},
				{"Id":"leer","Name":"Leer","Icons":[]}
			],
			"Current":{"Icon":"tiere/hund","Color":"#2e96e0"}
		}"""
		val catalog = TvClientAvatars.parseCatalog(text, "http://srv:8096/")
		catalog.colors shouldBe listOf("#e50914", "#2e96e0")
		catalog.categories.size shouldBe 1
		catalog.categories[0].icons[0].imageUrl shouldBe "http://srv:8096/LocalRecs/Avatars/Icons/tiere/katze"
		catalog.categories[0].icons[1].cover shouldBe true
		catalog.currentIcon shouldBe "tiere/hund"
		catalog.license shouldBe "Nur privat"
	}
})
