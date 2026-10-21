package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TvClientLocalAddressTests : FunSpec({
	test("IP ohne Schema und Port wird ergänzt") {
		TvClientLocalAddress.normalizeAddress(" 192.168.72.91 ") shouldBe "http://192.168.72.91:8096"
		TvClientLocalAddress.normalizeAddress("192.168.72.91:8920/") shouldBe "http://192.168.72.91:8920"
		TvClientLocalAddress.normalizeAddress("https://server.lan") shouldBe "https://server.lan"
	}

	test("Leere und unbrauchbare Adressen") {
		TvClientLocalAddress.normalizeAddress("") shouldBe null
		TvClientLocalAddress.normalizeAddress(null) shouldBe null
		TvClientLocalAddress.normalizeAddress("http://[::1]:8096") shouldBe null
		TvClientLocalAddress.normalizeAddress("http://127.0.0.1:8096") shouldBe null
		TvClientLocalAddress.normalizeAddress("ftp://192.168.1.2") shouldBe null
	}

	test("Private Adressen erkennen") {
		TvClientLocalAddress.isPrivateAddress("http://192.168.72.91:8096") shouldBe true
		TvClientLocalAddress.isPrivateAddress("http://10.0.0.5:8096") shouldBe true
		TvClientLocalAddress.isPrivateAddress("http://172.20.1.1:8096") shouldBe true
		TvClientLocalAddress.isPrivateAddress("http://172.32.1.1:8096") shouldBe false
		TvClientLocalAddress.isPrivateAddress("https://212.227.68.184") shouldBe false
	}

	test("Server-Id mit und ohne Bindestriche") {
		TvClientLocalAddress.sameId("ec3992c9d9df4e28b80d3fdb68593284", "ec3992c9-d9df-4e28-b80d-3fdb68593284") shouldBe true
		TvClientLocalAddress.sameId("ec3992c9d9df4e28b80d3fdb68593284", "00000000-d9df-4e28-b80d-3fdb68593284") shouldBe false
		TvClientLocalAddress.sameId(null, "ec3992c9") shouldBe false
	}

	test("Netzkennung aus Gateway, DNS und Domain") {
		val home = TvClientLocalAddress.networkKey(listOf("192.168.72.1"), listOf("192.168.72.1"), "fritz.box")
		val other = TvClientLocalAddress.networkKey(listOf("192.168.72.1"), listOf("8.8.8.8"), null)
		(home == other) shouldBe false
		TvClientLocalAddress.networkKey(listOf("192.168.72.1"), listOf("192.168.72.1"), "fritz.box") shouldBe home
		TvClientLocalAddress.networkKey(emptyList(), listOf("1.1.1.1"), null) shouldBe null
	}

	test("Identitätsnachweis: Testvektor des Plugins") {
		val nonce = "ab".repeat(32)
		val proof = TvClientLocalAddress.proof("token-123", nonce, "c2b157ea3dc84584a30bcb96492eda16", "192.168.1.10:8096")
		proof shouldBe "baadad25df095f9c875dbbf4fab4b8c6741d39c30d189a8c6ee490b7c57024dc"
		TvClientLocalAddress.verifyProof("token-123", nonce, "c2b157ea3dc84584a30bcb96492eda16", "192.168.1.10:8096", listOf("00", proof.uppercase())) shouldBe true
		TvClientLocalAddress.verifyProof("falsch", nonce, "c2b157ea3dc84584a30bcb96492eda16", "192.168.1.10:8096", listOf(proof)) shouldBe false
		// Anderer Host (Weiterreichen über die öffentliche Adresse) passt nicht
		TvClientLocalAddress.verifyProof("token-123", nonce, "c2b157ea3dc84584a30bcb96492eda16", "jellyfin.example.org", listOf(proof)) shouldBe false
	}

	test("Host wie im Host-Header") {
		TvClientLocalAddress.hostHeader("http://192.168.1.10:8096") shouldBe "192.168.1.10:8096"
		TvClientLocalAddress.hostHeader("http://Server.LAN:80/jf") shouldBe "server.lan"
		TvClientLocalAddress.hostHeader("https://srv.lan") shouldBe "srv.lan"
	}
})
