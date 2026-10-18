package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LogoTintTests : FunSpec({
	fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

	test("Schwarzes Logo wird eingefärbt") {
		LogoTint.isDark(IntArray(10) { argb(255, 10, 10, 10) }) shouldBe true
	}

	test("Roter Marvel-Kasten bleibt farbig") {
		LogoTint.isDark(IntArray(10) { argb(255, 236, 29, 36) }) shouldBe false
	}

	test("Weißes Logo bleibt") {
		LogoTint.isDark(IntArray(10) { argb(255, 250, 250, 250) }) shouldBe false
	}

	test("Durchsichtige Pixel zählen nicht") {
		val pixels = IntArray(100) { if (it < 90) argb(0, 0, 0, 0) else argb(255, 255, 255, 255) }
		LogoTint.isDark(pixels) shouldBe false
		LogoTint.isDark(IntArray(5) { argb(0, 0, 0, 0) }) shouldBe false
	}
})
