package org.jellyfin.androidtv.tvclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.compose.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * Jellyfin-TV-Client: Profilbild selbst wählen – Figur und Hintergrundfarbe aus dem Katalog des Plugins
 * Local Recommendations (ab 12.22). Das runde Bild erzeugt der Server und setzt es als Jellyfin-Profilbild.
 */
object TvClientAvatars {
	const val RESULT_KEY = "tvclient_avatar_changed"
	private const val TIMEOUT_MS = 10_000

	data class Icon(val id: String, val name: String, val imageUrl: String, val cover: Boolean)
	data class Category(val name: String, val icons: List<Icon>)
	data class Catalog(val colors: List<String>, val categories: List<Category>, val currentIcon: String?, val currentColor: String?, val license: String?)

	private val json = Json { ignoreUnknownKeys = true }

	private fun request(baseUrl: String, token: String, method: String, path: String, body: String? = null): Pair<Int, String> {
		val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
		return try {
			connection.requestMethod = method
			connection.connectTimeout = TIMEOUT_MS
			connection.readTimeout = TIMEOUT_MS
			connection.setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
			connection.setRequestProperty("Accept", "application/json")
			if (body != null) {
				connection.doOutput = true
				connection.setRequestProperty("Content-Type", "application/json")
				connection.outputStream.use { it.write(body.toByteArray()) }
			}
			val code = connection.responseCode
			code to (if (code in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else "")
		} finally {
			connection.disconnect()
		}
	}

	/** Bietet der Server Profilbilder an? (GET /LocalRecs/Web/Config → „Avatars“: true) */
	suspend fun isAvailable(baseUrl: String, token: String): Boolean = withContext(Dispatchers.IO) {
		runCatching {
			val config = json.parseToJsonElement(request(baseUrl, token, "GET", "/LocalRecs/Web/Config").second) as? JsonObject
			(config?.get("Avatars") as? JsonPrimitive)?.booleanOrNull == true
		}.getOrDefault(false)
	}

	private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

	/** Antwort von GET /LocalRecs/Avatars lesen; Bildadressen werden vollständig gemacht. */
	fun parseCatalog(text: String, baseUrl: String): Catalog {
		val root = json.parseToJsonElement(text) as JsonObject
		val base = baseUrl.trimEnd('/')
		val categories = (root["Categories"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map { category ->
			Category(
				name = category.string("Name").orEmpty(),
				icons = (category["Icons"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.mapNotNull { icon ->
					val id = icon.string("Id") ?: return@mapNotNull null
					val url = icon.string("ImageUrl") ?: return@mapNotNull null
					Icon(
						id = id,
						name = icon.string("Name").orEmpty(),
						imageUrl = if (url.startsWith("http")) url else "$base/${url.trimStart('/')}",
						cover = icon.string("Fit") == "cover",
					)
				},
			)
		}.filter { it.icons.isNotEmpty() }
		val current = root["Current"] as? JsonObject
		return Catalog(
			colors = (root["Colors"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content },
			categories = categories,
			currentIcon = current?.string("Icon"),
			currentColor = current?.string("Color"),
			license = root.string("License"),
		)
	}

	suspend fun catalog(baseUrl: String, token: String): Catalog? = withContext(Dispatchers.IO) {
		runCatching {
			val (code, body) = request(baseUrl, token, "GET", "/LocalRecs/Avatars")
			if (code != 200) null else parseCatalog(body, baseUrl)
		}.onFailure { Timber.w(it, "Profilbild-Katalog nicht verfügbar") }.getOrNull()
	}

	/** Bild auf dem Server erzeugen und als Profilbild setzen; true bei Erfolg. */
	suspend fun apply(baseUrl: String, token: String, icon: String, color: String): Boolean = withContext(Dispatchers.IO) {
		runCatching {
			val body = buildJsonObject {
				put("Icon", icon)
				put("Color", color)
			}.toString()
			request(baseUrl, token, "POST", "/LocalRecs/Avatars/Me", body).first == 200
		}.getOrDefault(false)
	}

	/** Auswahl öffnen (für das Profil mit diesem Token). Nach dem Speichern kommt [RESULT_KEY] als Fragment-Ergebnis. */
	fun show(fragmentManager: FragmentManager, baseUrl: String, token: String, name: String) {
		AvatarPickerDialog().apply {
			arguments = bundleOf("baseUrl" to baseUrl, "token" to token, "name" to name)
		}.show(fragmentManager, "avatar_picker")
	}

	fun parseColor(value: String?): Color = runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(Color(0xFF2E96E0))
}

class AvatarPickerDialog : DialogFragment() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		val args = requireArguments()
		JellyfinTheme {
			AvatarPicker(
				baseUrl = args.getString("baseUrl").orEmpty(),
				token = args.getString("token").orEmpty(),
				name = args.getString("name").orEmpty(),
				onDone = { changed ->
					if (changed) parentFragmentManager.setFragmentResult(TvClientAvatars.RESULT_KEY, Bundle())
					dismissAllowingStateLoss()
				},
			)
		}
	}
}

/** Rundes Profilbild: Kreis in der Farbe, darauf die Figur (80 % bei „contain“, randfüllend bei „cover“). */
@Composable
fun AvatarPreview(icon: TvClientAvatars.Icon?, color: Color, size: Dp) {
	Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
		if (icon != null) {
			AsyncImage(
				modifier = if (icon.cover) Modifier.fillMaxSize() else Modifier.size(size * 0.8f),
				url = icon.imageUrl,
				aspectRatio = 1f,
			)
		}
	}
}

@Composable
private fun AvatarPicker(baseUrl: String, token: String, name: String, onDone: (Boolean) -> Unit) {
	val context = androidx.compose.ui.platform.LocalContext.current
	val scope = rememberCoroutineScope()
	var catalog by remember { mutableStateOf<TvClientAvatars.Catalog?>(null) }
	var failed by remember { mutableStateOf(false) }
	var icon by remember { mutableStateOf<String?>(null) }
	var color by remember { mutableStateOf<String?>(null) }
	var saving by remember { mutableStateOf(false) }
	val first = remember { FocusRequester() }

	LaunchedEffect(Unit) {
		val loaded = TvClientAvatars.catalog(baseUrl, token)
		if (loaded == null) failed = true
		catalog = loaded
		icon = loaded?.currentIcon ?: loaded?.categories?.firstOrNull()?.icons?.firstOrNull()?.id
		color = loaded?.currentColor ?: loaded?.colors?.firstOrNull()
		delay(150)
		runCatching { first.requestFocus() }
	}

	val all = catalog?.categories?.flatMap { it.icons }.orEmpty()
	val selectedIcon = all.firstOrNull { it.id == icon }
	val failedText = stringResource(R.string.tvclient_avatar_failed)
	val savedText = stringResource(R.string.tvclient_avatar_saved)

	Column(
		Modifier
			.fillMaxSize()
			.background(Color(0xFF0B0E14))
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 64.dp, vertical = 36.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
			AvatarPreview(selectedIcon, TvClientAvatars.parseColor(color), 140.dp)
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				Text(stringResource(R.string.tvclient_avatar_title, name), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					PickerButton(stringResource(R.string.lbl_save), Modifier.focusRequester(first)) {
						val chosenIcon = icon
						val chosenColor = color
						if (saving || chosenIcon == null || chosenColor == null) return@PickerButton
						saving = true
						scope.launch {
							val ok = TvClientAvatars.apply(baseUrl, token, chosenIcon, chosenColor)
							saving = false
							Toast.makeText(context, if (ok) savedText else failedText, Toast.LENGTH_SHORT).show()
							if (ok) onDone(true)
						}
					}
					PickerButton(stringResource(R.string.lbl_cancel)) { onDone(false) }
				}
			}
		}

		if (failed) Text(failedText, fontSize = 16.sp, color = Color(0xFFB3B3B3))

		catalog?.let { loaded ->
			Text(stringResource(R.string.tvclient_avatar_color), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9CED8))
			LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				items(loaded.colors) { value ->
					ColorDot(TvClientAvatars.parseColor(value), selected = value == color) { color = value }
				}
			}
			loaded.categories.forEach { category ->
				Text(category.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9CED8), modifier = Modifier.padding(top = 8.dp))
				LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
					items(category.icons, key = { it.id }) { item ->
						IconChoice(item, TvClientAvatars.parseColor(color), selected = item.id == icon) { icon = item.id }
					}
				}
			}
			loaded.license?.takeIf { it.isNotBlank() }?.let {
				Text(it, fontSize = 12.sp, color = Color(0xFF8A8F99), modifier = Modifier.padding(top = 12.dp))
			}
		}
	}
}

@Composable
private fun PickerButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	Box(
		modifier
			.background(if (focused) Color.White else Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(horizontal = 20.dp, vertical = 10.dp),
	) {
		Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (focused) Color(0xFF0B0E14) else Color.White)
	}
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	Box(
		Modifier
			.size(52.dp)
			.border(3.dp, if (focused) Color.White else if (selected) Color(0xFF46D369) else Color.Transparent, CircleShape)
			.padding(6.dp)
			.clip(CircleShape)
			.background(color)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick),
	)
}

@Composable
private fun IconChoice(icon: TvClientAvatars.Icon, color: Color, selected: Boolean, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	Column(
		Modifier
			.border(3.dp, if (focused) Color.White else if (selected) Color(0xFF46D369) else Color.Transparent, RoundedCornerShape(16.dp))
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		AvatarPreview(icon, color, 96.dp)
		Text(icon.name, fontSize = 13.sp, color = if (focused) Color.White else Color(0xFFB3B3B3), maxLines = 1, modifier = Modifier.padding(top = 6.dp).widthIn(max = 104.dp))
	}
}
