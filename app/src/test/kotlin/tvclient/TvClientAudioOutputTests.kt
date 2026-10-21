package org.jellyfin.androidtv.tvclient

import androidx.media3.common.C
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.constant.Codec

class TvClientAudioOutputTests : FunSpec({
	test("Ausgang nur mit Dolby Digital: kein DTS und kein TrueHD direkt") {
		TvClientAudioOutput.fromEncodings(setOf(C.ENCODING_AC3, C.ENCODING_E_AC3)) shouldBe emptySet()
	}

	test("Ausgang mit DTS: DTS direkt, TrueHD weiter über den Server") {
		TvClientAudioOutput.fromEncodings(setOf(C.ENCODING_AC3, C.ENCODING_DTS)) shouldBe setOf(Codec.Audio.DCA, Codec.Audio.DTS)
	}

	test("AV-Receiver mit DTS-HD und TrueHD: alles direkt") {
		TvClientAudioOutput.fromEncodings(setOf(C.ENCODING_DTS_HD, C.ENCODING_DOLBY_TRUEHD)) shouldBe TvClientAudioOutput.HD_AUDIO_CODECS
	}
})
