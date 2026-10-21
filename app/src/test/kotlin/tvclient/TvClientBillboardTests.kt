package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class TvClientBillboardTests : FunSpec({
	test("Banner vom Server: Reihenfolge und Badges") {
		val selection = TvClientBillboard.parse(
			"""{"Items":[{"Id":"c2b157ea3dc84584a30bcb96492eda16","Type":"Movie"},{"Id":"0f8fad5b-d9cb-469f-a165-70867728950e","Type":"Series"},{"Id":"kaputt"}],
			"TotalRecordCount":3,"Reasons":{"0f8fad5bd9cb469fa16570867728950e":"Neue Folgen","x":" "}}"""
		)
		selection.ids shouldBe listOf(UUID.fromString("c2b157ea-3dc8-4584-a30b-cb96492eda16"), UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"))
		selection.reasons shouldBe mapOf("0f8fad5bd9cb469fa16570867728950e" to "Neue Folgen")
	}
})
