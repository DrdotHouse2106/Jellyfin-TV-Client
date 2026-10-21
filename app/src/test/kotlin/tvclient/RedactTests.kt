package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.jellyfin.androidtv.redactSecrets

class RedactTests : FunSpec({
	test("Tokens in Protokollzeilen schwärzen") {
		redactSecrets("GET http://srv/Videos/1/stream?static=true&ApiKey=abc123&x=1") shouldBe "GET http://srv/Videos/1/stream?static=true&ApiKey=***&x=1"
		redactSecrets("url?api_key=secret") shouldBe "url?api_key=***"
		redactSecrets("Authorization: MediaBrowser Token=\"geheim\", Client=\"x\"") shouldNotContain "geheim"
		redactSecrets("Untertitel: Ton=de") shouldBe "Untertitel: Ton=de"
	}
})
