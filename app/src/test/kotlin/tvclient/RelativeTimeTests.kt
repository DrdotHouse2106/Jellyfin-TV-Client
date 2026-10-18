package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RelativeTimeTests : FunSpec({
	val minute = 60_000L
	val hour = 60 * minute

	test("1 Stunde 59 Minuten wird zu 2 Stunden") {
		RelativeTime.roundedDiff(hour + 59 * minute) shouldBe 2 * hour
	}

	test("23 Minuten bleiben 23 Minuten") {
		RelativeTime.roundedDiff(23 * minute + 20_000) shouldBe 23 * minute
	}

	test("1 Stunde 20 Minuten wird zu 1 Stunde") {
		RelativeTime.roundedDiff(hour + 20 * minute) shouldBe hour
	}
})
