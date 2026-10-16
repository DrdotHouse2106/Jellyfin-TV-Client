package org.jellyfin.androidtv.ui.card

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.ImageView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.util.MenuBuilder
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.showIfNotEmpty

@Composable
fun UserCard(
	image: @Composable () -> Unit,
	name: @Composable () -> Unit,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	// Jellyfin-TV-Client: Netflix-Profile – abgerundete Quadrate statt Kreise
	shape: Shape = RoundedCornerShape(8.dp),
) {
	val focused by interactionSource.collectIsFocusedAsState()

	// Mix button background with input foreground because we display text beneath the profile picture on a transparent background similar
	// to the input text
	val color = when {
		focused -> Color.White to Color.White
		else -> Color.Transparent to Color(0xFF9A9A9A)
	}
	val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "UserCardFocusScale")

	Column(
		modifier = modifier
			.scale(scale)
			.focusable(interactionSource = interactionSource)
			.clickable(interactionSource = interactionSource, onClick = onClick, indication = null)
	) {
		Box(
			modifier = Modifier
				.aspectRatio(1f)
				.clip(shape)
				.border(3.dp, color.first, shape)
		) {
			image()
		}

		Spacer(Modifier.height(12.dp))

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.basicMarquee(
					iterations = if (focused) Int.MAX_VALUE else 0,
					initialDelayMillis = 0,
				),
			contentAlignment = Alignment.TopCenter,
		) {
			ProvideTextStyle(
				LocalTextStyle.current.copy(
					color = color.second,
					fontSize = 18.sp,
				)
			) {
				name()
			}
		}
	}
}

/** Jellyfin-TV-Client: feste Profilfarbe pro Name (wie die bunten Netflix-Profile). */
private val profileColors = listOf(
	Color(0xFF1F5FBF), Color(0xFFB81D24), Color(0xFFC9961A), Color(0xFF1E6F6A),
	Color(0xFF5E3BA6), Color(0xFF2F7D32), Color(0xFFB4501A),
)

private fun profileColor(name: String?): Color =
	profileColors[Math.floorMod(name.orEmpty().lowercase().hashCode(), profileColors.size)]

class UserCardView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {
	var name by mutableStateOf<String?>(null)
	var image by mutableStateOf<String?>(null)
	/** Jellyfin-TV-Client: Kachel „Profil hinzufügen“ mit Plus. */
	var addTile by mutableStateOf(false)
	private var focused by mutableStateOf(false)

	init {
		isFocusable = true
		descendantFocusability = FOCUS_BLOCK_DESCENDANTS
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) defaultFocusHighlightEnabled = false
	}

	fun setPopupMenu(init: MenuBuilder.() -> Unit) {
		setOnLongClickListener {
			popupMenu(context, this, init = init).showIfNotEmpty()
		}
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		if (super.onKeyUp(keyCode, event)) return true

		// Menu key should show the popup menu
		if (event.keyCode == KeyEvent.KEYCODE_MENU) return performLongClick()

		return false
	}

	override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

		focused = gainFocus
	}

	@Composable
	override fun Content() {
		val interactionSource = remember { MutableInteractionSource() }

		// Forward focus events to the interaction source
		val focusInteraction = remember { FocusInteraction.Focus() }
		LaunchedEffect(focused) {
			if (focused) interactionSource.emit(focusInteraction)
			else interactionSource.emit(FocusInteraction.Unfocus(focusInteraction))
		}

		UserCard(
			image = {
				if (addTile) {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.background(Color(0xFF262626)),
						contentAlignment = Alignment.Center,
					) {
						Text("+", fontSize = 72.sp, color = Color(0xFF9A9A9A))
					}
				} else if (image != null) {
					AsyncImage(
						modifier = Modifier.fillMaxSize(),
						scaleType = ImageView.ScaleType.CENTER_CROP,
						url = image,
					)
				} else {
					// Jellyfin-TV-Client: farbige Kachel mit Anfangsbuchstaben wie bei Netflix
					val letter = name?.trim()?.firstOrNull()?.uppercaseChar()
					Box(
						modifier = Modifier
							.fillMaxSize()
							.background(profileColor(name)),
						contentAlignment = Alignment.Center,
					) {
						if (letter != null) {
							Text(letter.toString(), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.White)
						} else {
							Icon(
								imageVector = ImageVector.vectorResource(R.drawable.ic_user),
								contentDescription = name,
								modifier = Modifier.size(64.dp),
							)
						}
					}
				}
			},
			name = {
				Text(name.orEmpty(), maxLines = 1)
			},
			modifier = Modifier
				.padding(horizontal = 14.dp, vertical = 10.dp)
				.width(150.dp),
			interactionSource = interactionSource,
			// We use our own click handler for views used in presenters
			onClick = {}
		)
	}
}
