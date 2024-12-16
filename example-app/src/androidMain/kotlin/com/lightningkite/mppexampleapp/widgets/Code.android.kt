package com.lightningkite.mppexampleapp.widgets

import android.graphics.Paint
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.colorInt
import com.lightningkite.kiteui.views.direct.typeface

actual class Code actual constructor(context: RContext) :
    RView(context) {
    val actualNative = android.widget.TextView(context.activity)
    override val native: View = actualNative
    actual var content: String
        get() {
            return actualNative.text.toString()
        }
        set(value) {
            actualNative.text = value
        }
    var align: Align
        get() {
            return when (actualNative.gravity) {
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

    var ellipsis: Boolean = true
        set(value) {
            field = value
            actualNative.ellipsize = if (value) TextUtils.TruncateAt.END else TextUtils.TruncateAt.MARQUEE
        }
    var wraps: Boolean = true
        set(value) {
            field = value
            actualNative.maxLines = if (value) Integer.MAX_VALUE else 1
        }
    var wordBreak: WordBreak = WordBreak.Normal
        set(value) {
            field = value
        }
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        if (this == viewDebugTarget) {
            println("native.setTextColor: ${theme.id} ${theme.foreground}")
        }
        actualNative.setTextColor(theme.foreground.colorInt())
        actualNative.setTypeface(theme.font.typeface(context.activity))
        actualNative.isAllCaps = theme.font.allCaps
        actualNative.paintFlags = actualNative.paintFlags and (android.graphics.Paint.UNDERLINE_TEXT_FLAG or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG).inv() or
                (if(theme.font.underline) android.graphics.Paint.UNDERLINE_TEXT_FLAG else 0) or
                (if(theme.font.strikethrough) Paint.STRIKE_THRU_TEXT_FLAG else 0)
        actualNative.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value)
    }
    fun setBasicHtmlContent(html: String) {
        actualNative.movementMethod = LinkMovementMethod.getInstance()
        actualNative.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }
}
