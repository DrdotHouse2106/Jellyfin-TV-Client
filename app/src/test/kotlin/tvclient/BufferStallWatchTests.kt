package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BufferStallWatchTests : FunSpec({
	test("Puffern ohne Fortschritt: nach 20 s Hänger, einmal") {
		val watch = BufferStallWatch()
		watch.check(765_501, 0) shouldBe false
		watch.check(765_501, 10_000) shouldBe false
		watch.check(765_501, 20_000) shouldBe true
		watch.check(765_501, 21_000) shouldBe false
	}

	test("Wachsender Puffer ist kein Hänger") {
		val watch = BufferStallWatch()
		(0..30).forEach { second -> watch.check(765_501L + second * 500, second * 1000L) shouldBe false }
	}
})
