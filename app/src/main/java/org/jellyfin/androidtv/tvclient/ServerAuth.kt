package org.jellyfin.androidtv.tvclient

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.Interceptor
import okhttp3.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.java.KoinJavaComponent

/**
 * Jellyfin-TV-Client: Anmeldung an den eigenen Server per Header statt als „ApiKey=“ in der Adresse.
 * So steht der Token weder in Protokollen noch im Bildspeicher (Coil) oder in Adressen, die weitergegeben
 * werden könnten. Gilt für Bilder des Plugins (Welten, QR-Code) und die Trailer der Vorschau.
 */
object ServerAuth {
	private fun api() = runCatching { KoinJavaComponent.get<ApiClient>(ApiClient::class.java) }.getOrNull()

	/** Header für eine Adresse auf dem angemeldeten Server, sonst null (fremde Server bekommen nichts). */
	fun headerFor(url: String): String? {
		val api = api() ?: return null
		val base = api.baseUrl?.trimEnd('/') ?: return null
		val token = api.accessToken ?: return null
		if (!url.startsWith("$base/")) return null
		return "MediaBrowser Token=\"$token\""
	}

	/** OkHttp-Interceptor für den Bildlader. */
	val interceptor = Interceptor { chain ->
		val request = chain.request()
		val header = if (request.header("Authorization") == null) headerFor(request.url.toString()) else null
		val response: Response = chain.proceed(if (header != null) request.newBuilder().header("Authorization", header).build() else request)
		// Der QR-Code enthält den geheimen Kalender-Link: nicht im Bildspeicher ablegen
		if (request.url.encodedPath.contains("/LocalRecs/Calendar/")) {
			response.newBuilder().header("Cache-Control", "no-store").build()
		} else {
			response
		}
	}

	/** Player für einen Trailer; bei Adressen des eigenen Servers mit Anmelde-Header. */
	@OptIn(UnstableApi::class)
	fun player(context: Context, url: String): ExoPlayer {
		val header = headerFor(url) ?: return ExoPlayer.Builder(context).build()
		val http = DefaultHttpDataSource.Factory().setDefaultRequestProperties(mapOf("Authorization" to header))
		return ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(http)).build()
	}
}
