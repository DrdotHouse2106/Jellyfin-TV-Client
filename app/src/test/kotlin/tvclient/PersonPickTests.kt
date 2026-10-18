package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class PersonPickTests : FunSpec({
	fun results(text: String) = Json.parseToJsonElement(text).jsonArray.map { it.jsonObject }

	test("Zwei Personen mit gleichem Namen: die bekanntere") {
		val list = results(
			"""[
				{"mediaType":"person","id":1354848,"name":"Mark Wahlberg","popularity":0.6,"knownFor":[]},
				{"mediaType":"person","id":13240,"name":"Mark Wahlberg","popularity":25.3,"knownFor":[{},{},{}]},
				{"mediaType":"movie","id":7,"name":"Mark Wahlberg"}
			]""",
		)
		Jellyseerr.pickPerson("Mark Wahlberg", list) shouldBe 13240
	}

	test("Kein exakter Name: kein Treffer") {
		val list = results("""[{"mediaType":"person","id":5,"name":"Kate Mara Jr."}]""")
		Jellyseerr.pickPerson("Kate Mara", list) shouldBe null
	}

	test("TMDB-Id der Jellyfin-Person") {
		val text = """{"Items":[
			{"Name":"Kate Mara Fan","ProviderIds":{"Tmdb":"1"}},
			{"Name":"Kate Mara","ProviderIds":{"Imdb":"nm1","Tmdb":"51072"}}
		]}"""
		LiveTvCredits.pickJellyfinTmdbId("Kate Mara", text) shouldBe 51072
		LiveTvCredits.pickJellyfinTmdbId("Michael Peña", """{"Items":[]}""") shouldBe null
	}
})
