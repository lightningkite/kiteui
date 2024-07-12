package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.Typeface
import android.graphics.text.LineBreakConfig
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Layout
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual abstract class TextView actual constructor(context: RContext) :
    RView(context) {
    override abstract val native: android.widget.TextView
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
    actual var textSize: Dimension
        get() {
            return Dimension(native.textSize)
        }
        set(value) {
            native.setTextSize(TypedValue.COMPLEX_UNIT_PX, value.value.toFloat())
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
}

object TextSizes {
    val h1: Float get() = 2.rem.value
    val h2: Float get() = 1.6.rem.value
    val h3: Float get() = 1.4.rem.value
    val h4: Float get() = 1.3.rem.value
    val h5: Float get() = 1.2.rem.value
    val h6: Float get() = 1.1.rem.value
    val h = floatArrayOf(
        h1,
        h2,
        h3,
        h4,
        h5,
        h6,
    )
    val body: Float get() = 1.rem.value
    val subtext: Float get() = 0.8.rem.value
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

actual class HeaderView actual constructor(context: RContext, level: Int) : TextView(context) {
    override val native = android.widget.TextView(context.activity).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, TextSizes.h[level - 1])
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.setTextColor(theme.foreground.colorInt())
        native.setTypeface(theme.title.typeface(context.activity))
        native.isAllCaps = theme.title.allCaps
    }
}

actual class BodyTextView actual constructor(context: RContext) : TextView(context) {
    override val native = android.widget.TextView(context.activity).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, TextSizes.body)
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.setTextColor(theme.foreground.colorInt())
        native.setTypeface(theme.body.typeface(context.activity))
        native.isAllCaps = theme.body.allCaps
    }
}

actual class SubTextView actual constructor(context: RContext) : TextView(context) {
    override val native = android.widget.TextView(context.activity).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, TextSizes.subtext)
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.setTextColor(theme.foreground.colorInt())
        native.setTypeface(
            TypefaceCompat.create(
                native.context,
                theme.body.font,
                theme.body.weight,
                theme.body.italic
            )
        )
        native.isAllCaps = theme.body.allCaps
    }
}