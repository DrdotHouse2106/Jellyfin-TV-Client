package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ProblemReportsTests : FunSpec({
	val film = 120 * 60 * 1000L

	test("Nach wenigen Sekunden beendet: Abbruch") {
		ProblemReports.isAbort(15_000, film, error = false) shouldBe true
	}

	test("Länger geschaut: kein Abbruch, außer mit Player-Fehler") {
		ProblemReports.isAbort(30 * 60 * 1000L, film, error = false) shouldBe false
		ProblemReports.isAbort(30 * 60 * 1000L, film, error = true) shouldBe true
	}

	test("Trailer und Extras zählen nicht") {
		ProblemReports.isAbort(5_000, 3 * 60 * 1000L, error = false) shouldBe false
		ProblemReports.isAbort(5_000, null, error = false) shouldBe false
	}
})
