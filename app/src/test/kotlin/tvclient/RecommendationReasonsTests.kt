package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class RecommendationReasonsTests : FunSpec({
	test("Ids ohne Bindestriche, klein; leere Texte fallen weg") {
		val reasons = RecommendationReasons.parse(
			"""{"2A34445FBE389AAA2A2761E2E0AA0160":"Nächster Teil nach „Leberkäsjunkie“","c08a684d897ad56be093d9a61903d790":"  ","x":1}"""
		)
		reasons shouldBe mapOf("2a34445fbe389aaa2a2761e2e0aa0160" to "Nächster Teil nach „Leberkäsjunkie“")
	}

	test("Schlüssel aus einer UUID") {
		RecommendationReasons.key(UUID.fromString("2a34445f-be38-9aaa-2a27-61e2e0aa0160")) shouldBe "2a34445fbe389aaa2a2761e2e0aa0160"
	}
})
