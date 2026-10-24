package org.jellyfin.androidtv.tvclient

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Jellyfin-TV-Client: dunkles Info-Fenster im Stil der App (statt des hellen Android-Dialogs mit Knöpfen in
 * Großbuchstaben). Titel, Zeilen mit eigener Hervorhebung und Knöpfe in normaler Schreibung.
 */
object TvInfoDialog {
	enum class Style { REASON, STRONG, NORMAL, MUTED }

	data class Line(val text: String, val style: Style = Style.NORMAL)

	data class Button(val label: String, val onClick: () -> Unit = {})

	fun show(context: Context, title: String, lines: List<Line>, buttons: List<Button>): Dialog {
		fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

		val dialog = Dialog(context)
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

		val content = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(32), dp(28), dp(32), dp(24))
			background = GradientDrawable().apply {
				setColor(Color.parseColor("#F2141414"))
				cornerRadius = dp(10).toFloat()
			}
		}
		content.addView(TextView(context).apply {
			text = title
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
			typeface = Typeface.DEFAULT_BOLD
		})
		lines.filter { it.text.isNotBlank() }.forEach { line ->
			content.addView(TextView(context).apply {
				text = line.text
				setLineSpacing(0f, 1.15f)
				when (line.style) {
					Style.REASON -> {
						setTextColor(RecommendationReasons.COLOR)
						typeface = Typeface.DEFAULT_BOLD
						setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
					}
					Style.STRONG -> {
						setTextColor(Color.WHITE)
						typeface = Typeface.DEFAULT_BOLD
						setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
					}
					Style.NORMAL -> {
						setTextColor(Color.parseColor("#E0E0E0"))
						setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
					}
					Style.MUTED -> {
						setTextColor(Color.parseColor("#B3B3B3"))
						setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
					}
				}
			}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
		}

		val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
		val views = buttons.mapIndexed { index, button ->
			TextView(context).apply {
				text = button.label
				isAllCaps = false
				isFocusable = true
				isClickable = true
				typeface = Typeface.DEFAULT_BOLD
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
				setPadding(dp(24), dp(10), dp(24), dp(10))
				background = StateListDrawable().apply {
					addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply {
						setColor(Color.WHITE)
						cornerRadius = dp(8).toFloat()
					})
					addState(intArrayOf(), GradientDrawable().apply {
						setColor(Color.parseColor("#2EFFFFFF"))
						cornerRadius = dp(8).toFloat()
					})
				}
				setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()), intArrayOf(Color.BLACK, Color.WHITE)))
				setOnClickListener {
					dialog.dismiss()
					button.onClick()
				}
			}.also {
				row.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { if (index > 0) marginStart = dp(12) })
			}
		}
		content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(24) })

		dialog.setContentView(content, ViewGroup.LayoutParams(dp(720), ViewGroup.LayoutParams.WRAP_CONTENT))
		dialog.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			setDimAmount(0.6f)
		}
		dialog.show()
		views.firstOrNull()?.requestFocus()
		return dialog
	}
}
