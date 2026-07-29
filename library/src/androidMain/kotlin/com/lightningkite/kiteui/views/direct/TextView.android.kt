package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreakConfig
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public actual class TextView actual constructor(context: ElementContext) : NativeElement(context) {
    override val driverValue: String? get() = content
    override val native: android.widget.TextView = android.widget.TextView(context.activity)

    public actual var content: String
        get() {
            return native.text.toString()
        }
        set(value) {
            native.text = value
        }

    private var _align: Align? = null
    private var _fontAndStyle: FontAndStyle? = null

    public actual var align: Align?
        get() = _align
        set(value) {
            _align = value
            applyAlign(value ?: _fontAndStyle?.align ?: Align.Start)
        }

    private fun applyAlign(value: Align) {
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

    public actual var ellipsis: Boolean = true
        set(value) {
            field = value
            native.ellipsize = if (value) TextUtils.TruncateAt.END else TextUtils.TruncateAt.MARQUEE
        }
    public actual var wraps: Boolean = true
        set(value) {
            field = value
            native.maxLines = if (value) Integer.MAX_VALUE else 1
        }
    public actual var wordBreak: WordBreak = WordBreak.Normal
        set(value) {
            field = value
            if(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                when (value) {
                    WordBreak.Normal -> native.lineBreakStyle = LineBreakConfig.LINE_BREAK_STYLE_NORMAL
                    WordBreak.BreakAll -> native.lineBreakStyle = LineBreakConfig.LINE_BREAK_STYLE_NONE
                }
            }
        }
    public actual var lineClamp: Int? = null
        set(value) {
            field = value
            value?.let {
                native.maxLines = value
                native.ellipsize = TextUtils.TruncateAt.END
            }
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) { super.nativeApplyTheme(theme); val theme = theme.theme
        debugPrint {
            "native.setTextColor: ${theme.id} ${theme.foreground}"
        }
        _fontAndStyle = theme.font
        native.setTextColor(theme.foreground.colorInt())
        native.setTypeface(theme.font.typeface(context.activity))
        native.isAllCaps = theme.font.allCaps
        native.paintFlags = native.paintFlags and (android.graphics.Paint.UNDERLINE_TEXT_FLAG or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG).inv() or
                (if(theme.font.underline) android.graphics.Paint.UNDERLINE_TEXT_FLAG else 0) or
                (if(theme.font.strikethrough) Paint.STRIKE_THRU_TEXT_FLAG else 0)
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value)
        applyAlign(_align ?: theme.font.align)
    }

    @RequiresApi(VERSION_CODES.N)
    public actual fun setBasicHtmlContent(html: String) {
        if(html.contains("<a")) {
            native.movementMethod = LinkMovementMethod.getInstance()
        } else {
            native.movementMethod = null
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                native.setFocusable(View.FOCUSABLE_AUTO)
            }
            native.isClickable = false
            native.isLongClickable = false
        }
        native.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }
}


private val typefaceCache = HashMap<FontAndStyle, Typeface>()
public fun FontAndStyle.typeface(context: Context) = typefaceCache.getOrPut(this) {
    TypefaceCompat.create(
        context,
        this.font.toTypeface(),
        this.weight,
        this.italic
    )
}
