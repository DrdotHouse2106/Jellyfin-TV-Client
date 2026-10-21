package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PersonCreditsTests : FunSpec({
	fun media(id: Int, type: String, status: Jellyseerr.Status) =
		Jellyseerr.Media(id, type, "T$id", null, null, null, status, null)

	test("Personenseite: Bibliothek zuerst, dann weitere Filme und Serien") {
		val movies = listOf(media(1, "movie", Jellyseerr.Status.AVAILABLE), media(2, "movie", Jellyseerr.Status.NONE), media(3, "movie", Jellyseerr.Status.REQUESTED))
		val series = listOf(media(4, "tv", Jellyseerr.Status.PARTIAL), media(5, "tv", Jellyseerr.Status.NONE))
		val split = splitByLibrary(movies, series)
		split.library.map { it.id } shouldBe listOf(1, 4)
		split.movies.map { it.id } shouldBe listOf(2, 3)
		split.series.map { it.id } shouldBe listOf(5)
	}
})
