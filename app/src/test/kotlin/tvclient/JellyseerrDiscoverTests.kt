package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class JellyseerrDiscoverTests : FunSpec({
	fun media(id: Int, year: String, type: String = "movie", status: Jellyseerr.Status = Jellyseerr.Status.NONE) =
		Jellyseerr.Media(id, type, "T$id", year, null, null, status, null, "$year-01-01")

	test("Neuere Titel zuerst, bei gleichem Jahr die öfter empfohlenen") {
		val result = JellyseerrDiscover.rank(
			listOf(media(1, "2010"), media(2, "2024"), media(3, "2024"), media(3, "2024"), media(4, "2018")),
			emptySet(),
		)
		result.map { it.id } shouldContainExactly listOf(3, 2, 4, 1)
	}

	test("Titel aus der Bibliothek und vorhandene fallen weg") {
		val result = JellyseerrDiscover.rank(
			listOf(media(1, "2020"), media(2, "2021", status = Jellyseerr.Status.AVAILABLE), media(5, "2022", type = "tv")),
			setOf("movie-1"),
		)
		result.map { it.id } shouldContainExactly listOf(5)
	}
})

class JellyseerrSearchTests : io.kotest.core.spec.style.FunSpec({
	test("Jahreszahl am Ende wird abgetrennt") {
		Jellyseerr.splitYear("Superman 2025") shouldBe ("Superman" to "2025")
		Jellyseerr.splitYear("Blade Runner (1982)") shouldBe ("Blade Runner" to "1982")
		Jellyseerr.splitYear("1917") shouldBe ("1917" to null)
		Jellyseerr.splitYear("Lucky Luke") shouldBe ("Lucky Luke" to null)
	}
})

class JellyseerrVideoTests : io.kotest.core.spec.style.FunSpec({
	test("Deutscher Trailer vor englischem, Trailer vor Teaser") {
		val en = Jellyseerr.Video("en", "Trailer", "Official Trailer", "en")
		val deTeaser = Jellyseerr.Video("dt", "Teaser", "Teaser Deutsch", null)
		val deTrailer = Jellyseerr.Video("de", "Trailer", "Trailer German Deutsch", null)
		Jellyseerr.pickVideo(listOf(en, deTeaser, deTrailer)) shouldBe deTrailer
		Jellyseerr.pickVideo(listOf(en, deTeaser)) shouldBe deTeaser
		Jellyseerr.pickVideo(listOf(en, Jellyseerr.Video("c", "Clip", "Clip", "de"))) shouldBe en
	}
})

class JellyseerrLanguageLabelTests : io.kotest.core.spec.style.FunSpec({
	test("Kennzeichnung nicht-deutscher Trailer") {
		Jellyseerr.languageLabel(Jellyseerr.Video("a", "Trailer", "Trailer German Deutsch", null), "en") shouldBe null
		Jellyseerr.languageLabel(Jellyseerr.Video("b", "Trailer", "Official Trailer [Subtitled]", "en"), "ja") shouldBe "OmU"
		Jellyseerr.languageLabel(Jellyseerr.Video("c", "Trailer", "Official Trailer", "en"), "en") shouldBe "EN"
		Jellyseerr.languageLabel(Jellyseerr.Video("d", "Trailer", "Bande-annonce", "fr"), "fr") shouldBe "FR"
		Jellyseerr.languageLabel(Jellyseerr.Video("e", "Trailer", "Trailer", null), null) shouldBe "EN"
	}
})

class JellyseerrCreditsTests : io.kotest.core.spec.style.FunSpec({
	test("Filmografie: neueste zuerst, ohne Datum ans Ende, doppelte weg") {
		fun m(id: Int, date: String?) = Jellyseerr.Media(id, "movie", "T$id", date?.take(4), null, "p", Jellyseerr.Status.NONE, null, date)
		val result = Jellyseerr.sortCredits(listOf(m(1, "2001-01-01"), m(2, null), m(3, "2020-05-05"), m(1, "2001-01-01")))
		result.map { it.id } shouldContainExactly listOf(3, 1, 2)
	}
})
