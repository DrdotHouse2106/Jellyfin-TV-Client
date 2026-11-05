package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClientDiagnosticsTests : FunSpec({
	test("Client-Log: Titel und Zugangsdaten geschwärzt") {
		ClientDiagnostics.redact("Trailer-Vorschau: kein Trailer – Reihe „Für dich“, „Tatort“ (123): kein Treffer") shouldBe
			"Trailer-Vorschau: kein Trailer – Reihe „…“, „…“ (123): kein Treffer"
		ClientDiagnostics.redact("GET /Videos/1/stream?api_key=geheim") shouldBe "GET /Videos/1/stream?api_key=***"
	}

	test("WebView: nur die eigene Player-Seite") {
		TrailerWindow.isPlayerPage("${TrailerWindow.ORIGIN}/") shouldBe true
		TrailerWindow.isPlayerPage("about:blank") shouldBe true
		TrailerWindow.isPlayerPage("https://www.youtube.com/watch?v=abcdefghijk") shouldBe false
		TrailerWindow.isPlayerPage("${TrailerWindow.ORIGIN}.evil.example/") shouldBe false
	}

	test("Entdecken: Bilder nur von TMDb bzw. dem eigenen Server") {
		fun item(url: String) = TvClientSections.parseDiscoverItem(
			"http://srv:8096",
			kotlinx.serialization.json.Json.parseToJsonElement("""{"TmdbId":1,"Title":"X","PosterUrl":"$url"}""") as kotlinx.serialization.json.JsonObject,
		)?.posterUrl
		item("https://image.tmdb.org/t/p/a.jpg") shouldBe "https://image.tmdb.org/t/p/a.jpg"
		item("/LocalRecs/x.jpg") shouldBe "http://srv:8096/LocalRecs/x.jpg"
		item("https://evil.example/a.jpg") shouldBe null
	}
})
