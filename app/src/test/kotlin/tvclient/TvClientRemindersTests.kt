package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class TvClientRemindersTests : FunSpec({
	test("Gemerkte Sendungen lesen (mit und ohne Bindestriche)") {
		val text = """[
			{"ProgramId":"ABCDEF0123456789ABCDEF0123456789","Name":"Tatort","IsSeries":true},
			{"ProgramId":"11111111-2222-3333-4444-555555555555","Name":"Film","IsSeries":false}
		]"""
		TvClientReminders.parseReminders(text) shouldBe mapOf(
			"abcdef0123456789abcdef0123456789" to true,
			"11111111222233334444555555555555" to false,
		)
	}

	test("Id ohne Bindestriche") {
		TvClientReminders.key(UUID.fromString("11111111-2222-3333-4444-555555555555")) shouldBe "11111111222233334444555555555555"
	}
})
