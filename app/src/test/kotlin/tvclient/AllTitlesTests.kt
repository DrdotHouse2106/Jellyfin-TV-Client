package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AllTitlesTests : FunSpec({
	test("Alle Titel: Reihen zu je 5 Karten (wie webOS)") {
		AllTitles.rows((1..12).toList()).map { it.size } shouldBe listOf(5, 5, 2)
		AllTitles.rows(emptyList<Int>()) shouldBe emptyList()
	}
})
