package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class HomeRowTests : FunSpec({
	test("Standard-Reihenfolge der Startseite") {
		HomeRow.entries.filter { it.preference.defaultValue } shouldContainExactly listOf(
			HomeRow.RESUME,
			HomeRow.NEXT_UP,
			HomeRow.SEASONAL,
			HomeRow.RECOMMENDED_MOVIES,
			HomeRow.RECOMMENDED_SERIES,
			HomeRow.MOMENTS,
			HomeRow.UPCOMING,
			HomeRow.MOVIE_SERIES,
			HomeRow.RECOMMENDED_LIVE_TV,
			HomeRow.TOP10,
			HomeRow.WORLDS,
			HomeRow.LATEST_MEDIA,
			HomeRow.GENRES,
			HomeRow.LIBRARIES,
		)
	}

	test("Weiterhören, Live-TV-Knöpfe, Aufnahmen, Apps und Gerät sind anfangs aus") {
		val enabled = HomeRow.entries.filter { it.preference.defaultValue }
		enabled shouldNotContain HomeRow.RESUME_AUDIO
		enabled shouldNotContain HomeRow.LIVE_TV
		enabled shouldNotContain HomeRow.ACTIVE_RECORDINGS
		enabled shouldNotContain HomeRow.APPS
		enabled shouldNotContain HomeRow.DEVICE
	}

	test("Jede Zeile hat einen eigenen Schlüssel") {
		HomeRow.entries.map { it.preference.key }.distinct().size shouldBe HomeRow.entries.size
	}

	test("Genre-Zeilen: höchstens 12, keine doppelten") {
		val names = (1..20).map { "Genre $it" } + listOf("Action", "action")
		val picked = GenreRows.pick(names)
		picked.size shouldBe 12
		picked.map { it.lowercase() }.distinct().size shouldBe 12
	}
})
