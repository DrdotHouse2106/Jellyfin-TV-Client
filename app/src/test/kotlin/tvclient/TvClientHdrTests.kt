package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TvClientHdrTests : FunSpec({
	test("Bildschirm ohne HDR: nichts direkt") {
		TvClientHdr.fromTypes(IntArray(0)) shouldBe TvClientHdr.NONE
		TvClientHdr.NONE.toString() shouldBe "keine"
	}

	test("Nur HDR10: HDR10+ läuft über die HDR10-Grundlage, Dolby Vision und HLG nicht") {
		val hdr = TvClientHdr.fromTypes(intArrayOf(2))
		hdr.hdr10 shouldBe true
		hdr.hdr10PlusOrBase shouldBe true
		hdr.dolbyVision shouldBe false
		hdr.hlg shouldBe false
		hdr.toString() shouldBe "HDR10"
	}

	test("Alle Typen") {
		TvClientHdr.fromTypes(intArrayOf(1, 2, 3, 4)) shouldBe TvClientHdr.ALL
	}
})
