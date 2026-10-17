package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class ScreensaverTrailersTests : FunSpec({
	fun video(name: String, seconds: Long?) = BaseItemDto(
		id = UUID.randomUUID(),
		type = BaseItemKind.TRAILER,
		name = name,
		runTimeTicks = seconds?.let { it * 10_000_000 },
	)

	test("Teaser am Namen erkennen") {
		val teaser = video("Dune - Teaser", 150)
		ScreensaverTrailers.pickTeaser(listOf(video("Dune - Trailer", 140), teaser)) shouldBe teaser
	}

	test("Sonst das kürzeste Video unter 100 Sekunden") {
		val short = video("Clip", 45)
		ScreensaverTrailers.pickTeaser(listOf(video("Trailer", 150), video("Spot", 80), short)) shouldBe short
	}

	test("Kein Teaser vorhanden") {
		ScreensaverTrailers.pickTeaser(listOf(video("Trailer", 150))) shouldBe null
	}
})
