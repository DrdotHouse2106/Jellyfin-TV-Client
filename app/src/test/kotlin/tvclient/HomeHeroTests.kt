package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class HomeHeroTests : FunSpec({
	test("Folge: Serienname als Titel, Staffel und Folge darunter") {
		val episode = BaseItemDto(
			id = UUID.randomUUID(),
			type = BaseItemKind.EPISODE,
			name = "Pilot",
			seriesName = "Navy CIS",
			parentIndexNumber = 1,
			indexNumber = 3,
		)

		heroTitle(episode) shouldBe "Navy CIS"
		heroSubtitle(episode) shouldBe "S1:F3 · Pilot"
	}

	test("Film: Jahr, FSK, Laufzeit, Bewertung und Genres") {
		val movie = BaseItemDto(
			id = UUID.randomUUID(),
			type = BaseItemKind.MOVIE,
			name = "Mortal Kombat",
			productionYear = 2021,
			officialRating = "FSK-18",
			runTimeTicks = 110L * 60 * 10_000_000,
			communityRating = 6.8f,
			genres = listOf("Action", "Fantasy", "Abenteuer"),
		)

		heroFacts(movie) shouldBe listOf("2021", "FSK-18", "1 Std. 50 Min.", "★ 6,8", "Action, Fantasy")
	}
})
