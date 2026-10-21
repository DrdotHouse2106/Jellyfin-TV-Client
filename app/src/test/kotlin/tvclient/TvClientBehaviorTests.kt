package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TvClientBehaviorTests : FunSpec({
	val kind = { row: String -> row.removePrefix("rec-").takeIf { row.startsWith("rec-") } }

	test("Nur die Plätze der Empfehlungszeilen tauschen") {
		val rows = listOf("resume", "rec-movies", "rec-series", "top10", "rec-livetv")
		TvClientBehavior.reorder(rows, kind, listOf("livetv", "series", "movies")) shouldBe
			listOf("resume", "rec-livetv", "rec-series", "top10", "rec-movies")
	}

	test("Ohne Reihenfolge vom Server bleibt alles") {
		val rows = listOf("resume", "rec-movies", "rec-series")
		TvClientBehavior.reorder(rows, kind, null) shouldBe rows
		TvClientBehavior.reorder(rows, kind, emptyList()) shouldBe rows
	}
})
