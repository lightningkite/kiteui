package com.lightningkite.kiteui.views.direct

import android.graphics.Paint
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*


actual class TextArea actual constructor(context: RContext) : RView(context) {
    override val native = EditText(context.activity).focusIsKeyboard().apply {
        maxLines = Int.MAX_VALUE
        inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.setTextColor(theme.foreground.colorInt())
        native.setHintTextColor(theme.foreground.closestColor().withAlpha(0.5f).colorInt())
        native.setTypeface(
            TypefaceCompat.create(
                native.context,
                theme.font.font,
                theme.font.weight,
                theme.font.italic
            )
        )
        native.paintFlags = native.paintFlags and (android.graphics.Paint.UNDERLINE_TEXT_FLAG or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG).inv() or
                (if(theme.font.underline) android.graphics.Paint.UNDERLINE_TEXT_FLAG else 0) or
                (if(theme.font.strikethrough) Paint.STRIKE_THRU_TEXT_FLAG else 0)
        native.isAllCaps = theme.font.allCaps
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value.toFloat())
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }
    actual val content: ImmediateWritable<String> = native.contentProperty()
    actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    actual var hint: String
        get() {
            return native.hint.toString()
        }
        set(value) {
            native.hint = value
        }
    var align: Align
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
                Align.Start -> native.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                Align.End -> native.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
                Align.Center -> native.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                Align.Stretch -> {
                    native.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                    native.updateLayoutParams<ViewGroup.LayoutParams> {
                        this.width = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
            }
        }
    var textSize: Dimension
        get() {
            return Dimension(native.textSize)
        }
        set(value) {
            native.setTextSize(TypedValue.COMPLEX_UNIT_PX, value.value.toFloat())
        }
}
