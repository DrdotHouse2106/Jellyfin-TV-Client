package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FrozenVideoWatchTests : FunSpec({
	test("Bild steht, Position läuft: nach 6 s eingefroren, einmal") {
		val watch = FrozenVideoWatch()
		watch.check(0, 11, 0) shouldBe false
		watch.check(3_000, 11, 3_000) shouldBe false
		watch.check(6_000, 11, 6_000) shouldBe true
		watch.check(7_000, 11, 7_000) shouldBe false
	}

	test("Bilder laufen oder Position steht (Pause): nicht eingefroren") {
		val watch = FrozenVideoWatch()
		(0..20).forEach { s -> watch.check(s * 1000L, s * 24L, s * 1000L) shouldBe false }
		val paused = FrozenVideoWatch()
		(0..20).forEach { s -> paused.check(5_000, 100, s * 1000L) shouldBe false }
	}
})
