package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlaybackStallWatchTests : FunSpec({
	test("Hänger: Position steht 8 s trotz Wiedergabe – einmal neu ansetzen") {
		val recovered = mutableListOf<Long>()
		val watch = PlaybackStallWatch { recovered += it }
		// läuft normal
		watch.check(1_000, 0, { "" })
		watch.check(1_500, 500, { "" })
		recovered shouldBe emptyList()
		// steht ab 1 500 ms
		watch.check(1_500, 5_000, { "" })
		recovered shouldBe emptyList()
		watch.check(1_500, 9_000, { "" })
		recovered shouldBe listOf(1_500L)
		// hängt weiter: nur noch melden, kein zweiter Neuansatz
		watch.check(1_500, 20_000, { "" })
		recovered shouldBe listOf(1_500L)
	}
})
