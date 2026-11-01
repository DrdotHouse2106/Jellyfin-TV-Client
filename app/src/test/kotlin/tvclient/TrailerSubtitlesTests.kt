package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TrailerSubtitlesTests : FunSpec({
	test("Untertitel nur bei fremdsprachigen Trailern") {
		TrailerWindow.subtitlesFor("de") shouldBe false
		TrailerWindow.subtitlesFor("de-DE") shouldBe false
		TrailerWindow.subtitlesFor("OmU") shouldBe false
		TrailerWindow.subtitlesFor(null) shouldBe false
		TrailerWindow.subtitlesFor("") shouldBe false
		TrailerWindow.subtitlesFor("en") shouldBe true
		TrailerWindow.subtitlesFor("EN") shouldBe true
		TrailerWindow.subtitlesFor("ja") shouldBe true
	}

	test("Knopf mit Sprache wie im Web") {
		TrailerWindow.buttonLabel("Trailer ansehen", "en") shouldBe "Trailer ansehen (EN)"
		TrailerWindow.buttonLabel("Trailer ansehen", "omu") shouldBe "Trailer ansehen (OmU)"
		TrailerWindow.buttonLabel("Trailer ansehen", "de") shouldBe "Trailer ansehen"
		TrailerWindow.buttonLabel("Trailer ansehen", null) shouldBe "Trailer ansehen"
	}

	test("YouTube-Player: deutsche Untertitel nur auf Wunsch") {
		val key = "abcdefghijk"
		TrailerWindow.youtubeHtml(key, withSound = true, volume = 1f, subtitles = true).contains("cc_load_policy: 1, cc_lang_pref: 'de'") shouldBe true
		val off = TrailerWindow.youtubeHtml(key, withSound = true, volume = 1f, subtitles = false)
		off.contains("cc_load_policy: 0") shouldBe true
		off.contains("cc_lang_pref") shouldBe false
	}
})
