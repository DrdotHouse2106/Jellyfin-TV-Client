package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.util.UUID

class Top10Tests : FunSpec({
	val a = UUID.randomUUID()
	val b = UUID.randomUUID()
	val c = UUID.randomUUID()
	val user1 = UUID.randomUUID()
	val user2 = UUID.randomUUID()

	test("Mehr verschiedene Zuschauer zählen vor mehr Wiedergaben") {
		val usages = listOf(
			Top10.Usage(a, user1, 9),
			Top10.Usage(b, user1, 1),
			Top10.Usage(b, user2, 1),
			Top10.Usage(c, user2, 2),
		)
		Top10.rank(usages) shouldContainExactly listOf(b, a, c)
	}

	test("Folgen derselben Serie werden zusammengezählt") {
		val usages = listOf(
			Top10.Usage(a, user1, 1),
			Top10.Usage(a, user1, 1),
			Top10.Usage(b, user1, 1),
		)
		Top10.rank(usages, size = 1) shouldContainExactly listOf(a)
	}
})
