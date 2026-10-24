package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
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

	test("YouTube-artige Nummern (Jahr als Staffel) werden nicht angezeigt") {
		val video = BaseItemDto(
			id = UUID.randomUUID(),
			type = BaseItemKind.EPISODE,
			name = "Video",
			seriesName = "Kanal",
			parentIndexNumber = 2026,
			indexNumber = 101698,
		)

		heroSubtitle(video) shouldBe "Video"
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

		heroFacts(movie) shouldBe listOf("2021", "FSK 18", "1 Std. 50 Min.", "★ 6,8", "Action, Fantasy")
	}

	test("Beschreibung ohne Markdown-Links und HTML") {
		cleanOverview("For young [Tanjirou](http://anidb.net/ch99458), these <b>rumors</b>\nwill come true.") shouldBe
			"For young Tanjirou, these rumors will come true."
	}

	test("Billboard: Filme und Serien abwechselnd") {
		fun item(kind: BaseItemKind, name: String) = BaseItemDto(id = UUID.randomUUID(), type = kind, name = name)
		val items = listOf(
			item(BaseItemKind.MOVIE, "F1"), item(BaseItemKind.MOVIE, "F2"), item(BaseItemKind.MOVIE, "F3"),
			item(BaseItemKind.SERIES, "S1"),
		)

		HomeBillboard.interleave(items).map { it.name } shouldBe listOf("F1", "S1", "F2", "F3")
	}

	test("Banner: Logo/Titel nur über Hintergrundbild, nicht über Thumb (Titel steht dort schon im Bild)") {
		fun movie(backdrop: Boolean) = BaseItemDto(
			id = UUID.randomUUID(),
			type = BaseItemKind.MOVIE,
			name = "Film",
			backdropImageTags = if (backdrop) listOf("b") else null,
			imageTags = mapOf(ImageType.THUMB to "t", ImageType.LOGO to "l"),
		)

		heroShowsTitle(movie(backdrop = true)) shouldBe true
		heroShowsTitle(movie(backdrop = false)) shouldBe false
		heroShowsTitle(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, name = "Ohne Bild")) shouldBe true
	}

	test("Altersfreigabe einheitlich als FSK") {
		ParentalRating.display("12") shouldBe "FSK 12"
		ParentalRating.display("FSK-16") shouldBe "FSK 16"
		ParentalRating.display("DE-6") shouldBe "FSK 6"
		ParentalRating.display("PG-13") shouldBe "PG-13"
		ParentalRating.display(" ") shouldBe null
	}
})
