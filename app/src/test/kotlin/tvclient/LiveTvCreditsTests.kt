package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class LiveTvCreditsTests : FunSpec({
	test("Treffer mit Darstellern, Regie und optionalen Feldern") {
		val credits = LiveTvCredits.parse(
			"""{"ProgramId":"abc","Matched":true,"Actors":[{"Name":"Christian Tramitz","Role":"Franz Hubert"},{"Name":"Helmfried von Lüttichau"}],"Directors":["Manuel Weiss"],"Writers":[],"Year":2023,"Episode":"S11 E8/12"}"""
		)!!
		credits.actors shouldContainExactly listOf(
			LiveTvCredits.Actor("Christian Tramitz", "Franz Hubert"),
			LiveTvCredits.Actor("Helmfried von Lüttichau", null),
		)
		credits.directors shouldContainExactly listOf("Manuel Weiss")
		credits.year shouldBe 2023
		credits.episode shouldBe "S11 E8/12"
	}

	test("Kein Treffer oder leere Listen ergeben nichts") {
		LiveTvCredits.parse("""{"ProgramId":"abc","Matched":false,"Actors":[],"Directors":[],"Writers":[]}""") shouldBe null
		LiveTvCredits.parse("""{"ProgramId":"abc","Matched":true,"Actors":[],"Directors":[],"Writers":["X"]}""") shouldBe null
	}

	test("Initialen aus Vor- und Nachname") {
		LiveTvCredits.initials("Christian Tramitz") shouldBe "CT"
		LiveTvCredits.initials("Helmfried von Lüttichau") shouldBe "HL"
		LiveTvCredits.initials("Cher") shouldBe "C"
	}
})
