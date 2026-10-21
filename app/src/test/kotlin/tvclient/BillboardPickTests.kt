package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BillboardPickTests : FunSpec({
	val pool = (1..30).toList()

	test("Banner: gleicher Tag und gleiches Profil ergeben dieselbe Auswahl") {
		HomeBillboard.dailyPick(pool, 6, "2026-10-21|u1") shouldBe HomeBillboard.dailyPick(pool, 6, "2026-10-21|u1")
		HomeBillboard.dailyPick(pool, 6, "2026-10-21|u1").toSet() shouldHaveSize 6
	}

	test("Banner: wechselt von Tag zu Tag") {
		HomeBillboard.dailyPick(pool, 6, "2026-10-21|u1") shouldNotBe HomeBillboard.dailyPick(pool, 6, "2026-10-22|u1")
	}

	test("Banner: vordere Empfehlungen kommen öfter dran, aber nicht immer nur die ersten") {
		val counts = IntArray(31)
		for (day in 1..300) HomeBillboard.dailyPick(pool, 4, "day-$day|u1").forEach { counts[it]++ }
		counts[1] shouldBeGreaterThan counts[20]
		(5..30).sumOf { counts[it] } shouldBeGreaterThan 0
	}

	test("Banner: kleine Liste bleibt vollständig") {
		HomeBillboard.dailyPick(listOf(1, 2), 6, "x") shouldContainAll listOf(1, 2)
	}
})
