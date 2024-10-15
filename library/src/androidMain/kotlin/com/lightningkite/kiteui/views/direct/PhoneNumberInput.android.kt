package com.lightningkite.kiteui.views.direct

import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.lens
import com.lightningkite.kiteui.utils.formatUSPhoneNumber
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction

actual class PhoneNumberInput actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = EditText(context.activity)
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
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value.toFloat())
    }

    actual val content: ImmediateWritable<String> = native.contentProperty().lens(
        get = { it.filter { it.isDigit() } },
        set = { it.formatUSPhoneNumber() }
    )
    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    init {
        native.keyboardHints = KeyboardHints.phone
    }

    override fun actionSet(value: Action?) {
        super.actionSet(value)
        native.setImeActionLabel(value?.title, KeyEvent.KEYCODE_ENTER)
        native.setOnEditorActionListener { v, actionId, event ->
            value?.startAction(this)
            value != null
        }
    }

    actual var hint: String
        get() {
            return native.hint.toString()
        }
        set(value) {
            native.hint = value
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

    actual var textSize: Dimension
        get() {
            return Dimension(native.textSize)
        }
        set(value) {
            native.setTextSize(TypedValue.COMPLEX_UNIT_PX, value.value.toFloat())
        }
}