package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PersonKeyTests : FunSpec({
	test("Typografischer Apostroph passt zum geraden") {
		Jellyseerr.personKey("Elyas M’Barek") shouldBe Jellyseerr.personKey("Elyas M'Barek")
	}

	test("Akzente, Groß/Klein und Leerzeichen spielen keine Rolle") {
		Jellyseerr.personKey("  Zoë   Saldaña ") shouldBe "zoe saldana"
	}

	test("Verschiedene Namen bleiben verschieden") {
		(Jellyseerr.personKey("Jella Haase") == Jellyseerr.personKey("Jella Haas")) shouldBe false
	}
})
