package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

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
