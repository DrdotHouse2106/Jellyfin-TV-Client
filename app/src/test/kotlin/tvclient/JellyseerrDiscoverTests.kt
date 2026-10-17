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
