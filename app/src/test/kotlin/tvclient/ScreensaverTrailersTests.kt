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

	test("Trailer vor Teaser, sonst der längste") {
		val trailer = video("Dune - Trailer", 140)
		ScreensaverTrailers.pickTrailer(listOf(video("Dune - Teaser", 150), trailer)) shouldBe trailer
		val long = video("Spot 2", 120)
		ScreensaverTrailers.pickTrailer(listOf(video("Spot 1", 80), long)) shouldBe long
	}

	test("Clips unter 15 Sekunden fallen weg") {
		ScreensaverTrailers.pickTrailer(listOf(video("Das Mercury Puzzle-other", 6))) shouldBe null
		val ok = video("Trailer", 59)
		ScreensaverTrailers.pickTrailer(listOf(video("Clip", 6), ok)) shouldBe ok
	}

	test("Ohne Längenangabe gilt ein Trailer als brauchbar") {
		val unknown = video("Trailer", null)
		ScreensaverTrailers.pickTrailer(listOf(unknown)) shouldBe unknown
	}

	test("Anzeigedauer: unbrauchbar kurze oder fehlende Länge gilt als Standard") {
		ScreensaverTrailers.showDuration(null) shouldBe kotlin.time.Duration.parse("123s")
		ScreensaverTrailers.showDuration(kotlin.time.Duration.parse("0s")) shouldBe kotlin.time.Duration.parse("123s")
		ScreensaverTrailers.showDuration(kotlin.time.Duration.parse("2s")) shouldBe kotlin.time.Duration.parse("123s")
		ScreensaverTrailers.showDuration(kotlin.time.Duration.parse("60s")) shouldBe kotlin.time.Duration.parse("63s")
		ScreensaverTrailers.showDuration(kotlin.time.Duration.parse("200s")) shouldBe kotlin.time.Duration.parse("180s")
	}
})
