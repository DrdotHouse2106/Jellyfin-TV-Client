package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class LiveTvFeedbackTests : FunSpec({
	val id = UUID.fromString("c2b157ea-3dc8-4584-a30b-cb96492eda16")
	val tatort = BaseItemDto(id = id, type = BaseItemKind.TV_PROGRAM, name = "Tatort", genres = listOf("Series", "Krimi", "Drama"))

	test("Genre: erstes aussagekräftiges, allgemeine wie Series werden übersprungen") {
		LiveTvFeedback.genreOf(tatort) shouldBe "Krimi"
		LiveTvFeedback.genreOf(tatort.copy(genres = listOf("News"))) shouldBe null
	}

	test("Body für den Server: Sendung, Titel und Art; Genre nur bei „Weniger <Genre>“") {
		val less = LiveTvFeedback.body(tatort, LiveTvFeedback.Kind.LESS)
		less shouldContain "\"ProgramId\":\"$id\""
		less shouldContain "\"Title\":\"Tatort\""
		less shouldContain "\"Art\":\"weniger\""
		less shouldNotContain "Genre"
		LiveTvFeedback.body(tatort, LiveTvFeedback.Kind.LESS_GENRE) shouldContain "\"Genre\":\"Krimi\""
		LiveTvFeedback.body(tatort, LiveTvFeedback.Kind.NEVER) shouldContain "\"Art\":\"nie\""
	}

	test("Rückgängig über Key bzw. Genre") {
		val mark = LiveTvFeedback.parseMark(LiveTvFeedback.Kind.NEVER, """{"Kind":"nie","Key":"tatort","Title":"Tatort","At":"2026-11-01T13:00:00Z"}""")
		LiveTvFeedback.undoPath(mark, tatort) shouldBe "/LocalRecs/LiveTv/Feedback?key=tatort"
		val genre = LiveTvFeedback.parseMark(LiveTvFeedback.Kind.LESS_GENRE, """{"Kind":"genre_weniger","Genre":"Krimi"}""")
		LiveTvFeedback.undoPath(genre, tatort) shouldBe "/LocalRecs/LiveTv/Feedback?genre=Krimi"
	}
})
