package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

class TvClientSubtitlesTests : FunSpec({
	fun stream(type: MediaStreamType, index: Int, language: String, forced: Boolean = false, default: Boolean = false, title: String? = null) =
		mockk<MediaStream> {
			every { this@mockk.type } returns type
			every { this@mockk.index } returns index
			every { this@mockk.language } returns language
			every { isForced } returns forced
			every { isDefault } returns default
			every { this@mockk.title } returns title
			every { displayTitle } returns title
		}

	// Wie „Mayday“: Ton 1 deu (Standard), 2 eng; Untertitel 3 deu erzwungen (Standard), 4 deu voll
	val mayday = listOf(
		stream(MediaStreamType.AUDIO, 1, "ger", default = true),
		stream(MediaStreamType.AUDIO, 2, "eng"),
		stream(MediaStreamType.SUBTITLE, 3, "ger", forced = true, default = true),
		stream(MediaStreamType.SUBTITLE, 4, "ger"),
	)
	val own = setOf("de")

	test("Deutscher Ton: nur erzwungene Untertitel") {
		TvClientSubtitles.decide(mayday, 3, 1, own) shouldBe 3
	}

	test("Originalton: volle deutsche Untertitel statt erzwungener") {
		TvClientSubtitles.decide(mayday, 3, 2, own) shouldBe 4
	}

	test("Originalton ohne Server-Vorschlag: volle deutsche Untertitel") {
		TvClientSubtitles.decide(mayday, null, 2, own) shouldBe 4
	}

	test("Deutscher Ton ohne erzwungene Untertitel: aus") {
		val streams = listOf(
			stream(MediaStreamType.AUDIO, 1, "deu"),
			stream(MediaStreamType.SUBTITLE, 2, "deu"),
		)
		TvClientSubtitles.decide(streams, 2, 1, own) shouldBe -1
	}

	test("Fremder Ton ohne deutsche Untertitel: gleiche Sprache wie Ton weglassen") {
		val streams = listOf(
			stream(MediaStreamType.AUDIO, 1, "eng"),
			stream(MediaStreamType.SUBTITLE, 2, "eng"),
		)
		TvClientSubtitles.decide(streams, 2, 1, own) shouldBe -1
	}

	// Wie „96 Hours – Taken 2“: Spur 3 heißt „German Forced“, ist in der Datei aber nicht als erzwungen markiert
	val taken = listOf(
		stream(MediaStreamType.AUDIO, 1, "ger", default = true),
		stream(MediaStreamType.AUDIO, 2, "eng"),
		stream(MediaStreamType.SUBTITLE, 3, "ger", title = "German Forced"),
		stream(MediaStreamType.SUBTITLE, 4, "ger"),
		stream(MediaStreamType.SUBTITLE, 5, "eng", title = "Englisch Full"),
	)

	test("Erzwungen am Namen erkannt: deutscher Ton wählt „German Forced“") {
		TvClientSubtitles.decide(taken, null, 1, own) shouldBe 3
	}

	test("Erzwungen am Namen erkannt: Originalton wählt die vollen deutschen Untertitel") {
		TvClientSubtitles.decide(taken, null, 2, own) shouldBe 4
	}

	test("Namen wie „erzwungen“ oder „nur fremdsprachige“ zählen auch") {
		TvClientSubtitles.isForced(stream(MediaStreamType.SUBTITLE, 1, "ger", title = "Deutsch (erzwungen)")) shouldBe true
		TvClientSubtitles.isForced(stream(MediaStreamType.SUBTITLE, 1, "ger", title = "Nur fremdsprachige Stellen")) shouldBe true
		TvClientSubtitles.isForced(stream(MediaStreamType.SUBTITLE, 1, "ger", title = "Deutsch")) shouldBe false
	}
})
