package org.jellyfin.androidtv.tvclient

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.jellyfin.androidtv.R

/**
 * Jellyfin-TV-Client: ein gemeinsames Fenster für Tonspur und Untertitel (zwei Spalten) statt zweier
 * getrennter Menüs. Links Ton, rechts Untertitel; die aktive Auswahl hat einen Haken.
 */
object AudioSubtitleDialog {
	data class Option(val index: Int, val label: String, val selected: Boolean)

	fun show(
		context: Context,
		audio: List<Option>,
		subtitles: List<Option>,
		onAudio: (Int) -> Unit,
		onSubtitle: (Int) -> Unit,
		onDismiss: () -> Unit,
	): Dialog {
		fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

		val dialog = Dialog(context)
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

		fun column(title: Int, options: List<Option>, onPick: (Int) -> Unit): LinearLayout {
			val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			list.addView(TextView(context).apply {
				setText(title)
				setTextColor(Color.parseColor("#B3B3B3"))
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
				typeface = Typeface.DEFAULT_BOLD
				setPadding(dp(16), 0, dp(16), dp(10))
			})
			options.forEach { option ->
				list.addView(TextView(context).apply {
					text = (if (option.selected) "✓  " else "     ") + option.label
					setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
					isFocusable = true
					isClickable = true
					maxLines = 2
					setPadding(dp(16), dp(10), dp(16), dp(10))
					background = StateListDrawable().apply {
						addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply {
							setColor(Color.WHITE)
							cornerRadius = dp(4).toFloat()
						})
						addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
					}
					setTextColor(
						android.content.res.ColorStateList(
							arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
							intArrayOf(Color.BLACK, if (option.selected) Color.WHITE else Color.parseColor("#D0D0D0")),
						)
					)
					setOnClickListener {
						dialog.dismiss()
						if (!option.selected) onPick(option.index)
					}
				}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
			}
			return list
		}

		val columns = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			setPadding(dp(24), dp(24), dp(24), dp(24))
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#F0141414"))
				cornerRadius = dp(8).toFloat()
			}
			if (audio.isNotEmpty()) addView(
				ScrollView(context).apply { addView(column(R.string.lbl_audio_track, audio, onAudio)) },
				LinearLayout.LayoutParams(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT),
			)
			if (subtitles.isNotEmpty()) addView(
				ScrollView(context).apply { addView(column(R.string.lbl_subtitle_track, subtitles, onSubtitle)) },
				LinearLayout.LayoutParams(dp(360), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(16) },
			)
		}

		dialog.setContentView(columns)
		dialog.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			setGravity(Gravity.BOTTOM or Gravity.END)
			attributes = attributes.apply {
				x = dp(48)
				y = dp(120)
			}
			setDimAmount(0.3f)
		}
		dialog.setOnDismissListener { onDismiss() }
		dialog.show()

		// Fokus auf die aktive Untertitel-Auswahl (sonst die aktive Tonspur)
		columns.post {
			val target = (0 until columns.childCount).reversed().asSequence()
				.mapNotNull { ((columns.getChildAt(it) as? ScrollView)?.getChildAt(0) as? LinearLayout) }
				.flatMap { col -> (1 until col.childCount).asSequence().map { col.getChildAt(it) as TextView } }
				.firstOrNull { it.text.startsWith("✓") }
			target?.requestFocus()
		}
		return dialog
	}
}
