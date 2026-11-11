package org.jellyfin.androidtv.tvclient

import androidx.media3.common.C
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.tvclient.TvClientSound.Mode

class TvClientSoundTests : FunSpec({
	val stereoTv = TvClientSound.Output("HDMI", "HDMI:TV", emptySet(), 2)
	val soundbar = TvClientSound.Output("HDMI", "HDMI:Bar", setOf(C.ENCODING_AC3, C.ENCODING_E_AC3), 6)
	val dolby = setOf(Codec.Audio.AC3, Codec.Audio.EAC3)

	test("Fernseher ohne Dolby, Stick dekodiert selbst: E-AC3 direkt nur bis 2 Kanäle, Umwandlung in Stereo") {
		TvClientSound.decide(stereoTv, dolby, Mode.AUTO, false) shouldBe
			TvClientSound.Decision(stereo = false, passthrough = emptySet(), blocked = emptySet(), maxChannels = 2)
	}

	test("Ohne Decoder und ohne Passthrough: Dolby nie direkt") {
		TvClientSound.decide(stereoTv, emptySet(), Mode.AUTO, false)?.blocked shouldBe dolby
	}

	test("Soundbar mit E-AC3: wird durchgereicht") {
		TvClientSound.decide(soundbar, emptySet(), Mode.AUTO, false) shouldBe
			TvClientSound.Decision(stereo = false, passthrough = dolby, blocked = emptySet(), maxChannels = 6)
	}

	test("Kein Ton? gemerkt oder Immer Stereo: Stereo; Durchreichen erzwingen: alles melden") {
		TvClientSound.decide(soundbar, dolby, Mode.AUTO, true)?.stereo shouldBe true
		TvClientSound.decide(soundbar, dolby, Mode.STEREO, false)?.stereo shouldBe true
		TvClientSound.decide(stereoTv, emptySet(), Mode.PASSTHROUGH, false) shouldBe null
	}

	test("Beschreibung für Log und Einstellungen") {
		soundbar.describe() shouldBe "HDMI, Passthrough: AC3, E-AC3; max. 6 Kanäle"
		stereoTv.describe() shouldBe "HDMI, Passthrough: keins; max. 2 Kanäle"
	}

	test("Atmos (E-AC3-JOC) zählt als E-AC3") {
		TvClientSound.fromEncodings(setOf(C.ENCODING_E_AC3_JOC)) shouldBe setOf(Codec.Audio.EAC3)
	}
})
