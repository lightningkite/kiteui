package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreakConfig
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import android.text.Layout
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class TextView actual constructor(context: RContext) :
    RView(context) {
    override val native: android.widget.TextView = android.widget.TextView(context.activity)
    actual var content: String
        get() {
            return native.text.toString()
        }
        set(value) {
            native.text = value
        }
    actual var align: Align
        get() {
            return when (native.gravity) {
                Gravity.START -> Align.Start
                Gravity.END -> Align.End
                Gravity.CENTER -> Align.Center
                Gravity.CENTER_VERTICAL -> Align.Start
                Gravity.CENTER_HORIZONTAL -> Align.Center
                else -> Align.Start
            }
        }
        set(value) {
            when (value) {
                Align.Start -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_START
                Align.End -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_END
                Align.Center -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_CENTER
                Align.Stretch -> {
                    native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_START
                    native.updateLayoutParams<ViewGroup.LayoutParams> {
                        this.width = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
            }
        }

    actual var ellipsis: Boolean = true
        set(value) {
            field = value
            native.ellipsize = if (value) TextUtils.TruncateAt.END else TextUtils.TruncateAt.MARQUEE
        }
    actual var wraps: Boolean = true
        set(value) {
            field = value
            native.maxLines = if (value) Integer.MAX_VALUE else 1
        }
    actual var wordBreak: WordBreak = WordBreak.Normal
        set(value) {
            field = value
            if(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                when (value) {
                    WordBreak.Normal -> native.lineBreakStyle = LineBreakConfig.LINE_BREAK_STYLE_NORMAL
                    WordBreak.BreakAll -> native.lineBreakStyle = LineBreakConfig.LINE_BREAK_STYLE_NONE
                }
            }
        }
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        if (this == viewDebugTarget) {
            println("native.setTextColor: ${theme.id} ${theme.foreground}")
        }
        native.setTextColor(theme.foreground.colorInt())
        native.setTypeface(theme.font.typeface(context.activity))
        native.isAllCaps = theme.font.allCaps
        native.paintFlags = native.paintFlags and (android.graphics.Paint.UNDERLINE_TEXT_FLAG or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG).inv() or
                (if(theme.font.underline) android.graphics.Paint.UNDERLINE_TEXT_FLAG else 0) or
                (if(theme.font.strikethrough) Paint.STRIKE_THRU_TEXT_FLAG else 0)
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value)
    }
    actual fun setBasicHtmlContent(html: String) {
        native.movementMethod = LinkMovementMethod.getInstance()
        native.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }
}


private val typefaceCache = HashMap<FontAndStyle, Typeface>()
fun FontAndStyle.typeface(context: Context) = typefaceCache.getOrPut(this) {
    TypefaceCompat.create(
        context,
        this.font,
        this.weight,
        this.italic
    )
}
