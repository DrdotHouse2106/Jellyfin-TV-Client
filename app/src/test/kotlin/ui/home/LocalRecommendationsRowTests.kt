package org.jellyfin.androidtv.ui.home

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.constant.HomeSectionType

class LocalRecommendationsRowTests : FunSpec({
	val recs = listOf(
		HomeSectionType.LOCAL_RECS_MOVIES,
		HomeSectionType.LOCAL_RECS_SERIES,
		HomeSectionType.LOCAL_RECS_LIVE_TV,
	)

	test("Empfehlungen kommen nach Weiterschauen / Als Nächstes") {
		val sections = listOf(
			HomeSectionType.LIBRARY_TILES_SMALL,
			HomeSectionType.RESUME,
			HomeSectionType.NEXT_UP,
			HomeSectionType.LATEST_MEDIA,
		)

		withLocalRecommendations(sections, true) shouldBe listOf(
			HomeSectionType.LIBRARY_TILES_SMALL,
			HomeSectionType.RESUME,
			HomeSectionType.NEXT_UP,
		) + recs + HomeSectionType.LATEST_MEDIA
	}

	test("Ohne Weiterschauen stehen die Empfehlungen am Anfang") {
		withLocalRecommendations(listOf(HomeSectionType.LATEST_MEDIA), true) shouldBe recs + HomeSectionType.LATEST_MEDIA
	}

	test("Selbst gewählte Empfehlungs-Zeilen werden nicht verändert") {
		val sections = listOf(HomeSectionType.LOCAL_RECS_SERIES, HomeSectionType.RESUME)
		withLocalRecommendations(sections, true) shouldBe sections
	}

	test("Ausgeschaltet bleibt alles wie es ist") {
		val sections = listOf(HomeSectionType.RESUME, HomeSectionType.LATEST_MEDIA)
		withLocalRecommendations(sections, false) shouldBe sections
	}
})
