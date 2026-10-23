package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class SelfUpdateTests : FunSpec({
	test("Versionen vergleichen") {
		SelfUpdate.isNewer("v1.24.10", "1.24.9") shouldBe true
		SelfUpdate.isNewer("1.25.0", "1.24.11") shouldBe true
		SelfUpdate.isNewer("v1.24.1", "1.24.1") shouldBe false
		SelfUpdate.isNewer("1.24.0", "1.24.1") shouldBe false
		SelfUpdate.isNewer("1.24", "1.24.0") shouldBe false
		SelfUpdate.isNewer("1.25.0-beta", "1.24.9") shouldBe true
	}

	test("Änderungen seit der installierten Version") {
		val changelog = """
			# Änderungen

			## 1.24.2 – 2026-10-19

			- **Neu:** Update-Knopf

			## 1.24.1 – 2026-10-18

			- Querformat

			## 1.24.0 – 2026-10-18

			- Heimnetz
		""".trimIndent()
		val notes = SelfUpdate.changesSince(changelog, "1.24.0")
		notes shouldContain "1.24.2"
		notes shouldContain "• Neu: Update-Knopf"
		notes shouldContain "Querformat"
		notes shouldNotContain "Heimnetz"
	}

	test("Dieselbe Version höchstens alle 12 Stunden anbieten") {
		val now = 100L * 60 * 60 * 1000
		SelfUpdate.shouldOffer(null, "1.30.7", now) shouldBe true
		SelfUpdate.shouldOffer("1.30.7|${now - 60_000}", "1.30.7", now) shouldBe false
		SelfUpdate.shouldOffer("1.30.7|${now - 13L * 60 * 60 * 1000}", "1.30.7", now) shouldBe true
		SelfUpdate.shouldOffer("1.30.6|${now - 60_000}", "1.30.7", now) shouldBe true
	}
})
