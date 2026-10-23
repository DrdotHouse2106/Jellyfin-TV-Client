package org.jellyfin.androidtv.tvclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class PersonalScreensaverTests : FunSpec({
	fun movie(played: Boolean, positionTicks: Long) = UUID.randomUUID().let { id ->
		BaseItemDto(
			id = id,
			type = BaseItemKind.MOVIE,
			name = "Film",
			userData = UserItemDataDto(
				playbackPositionTicks = positionTicks,
				playCount = if (played) 1 else 0,
				isFavorite = false,
				played = played,
				key = id.toString(),
				itemId = id,
			),
		)
	}

	test("Bildschirmschoner: nur ungesehene, nicht angefangene Titel") {
		PersonalScreensaver.isStartedOrPlayed(movie(played = false, positionTicks = 0)) shouldBe false
		// z. B. eben gestoppt bei 59 %
		PersonalScreensaver.isStartedOrPlayed(movie(played = false, positionTicks = 41_111_770_000)) shouldBe true
		PersonalScreensaver.isStartedOrPlayed(movie(played = true, positionTicks = 0)) shouldBe true
		PersonalScreensaver.isStartedOrPlayed(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE)) shouldBe false
	}
})
