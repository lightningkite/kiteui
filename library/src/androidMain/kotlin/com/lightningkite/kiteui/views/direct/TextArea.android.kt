package com.lightningkite.kiteui.views.direct

import android.text.Editable
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
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*


actual class TextArea actual constructor(context: RContext): RView(context) {
    override val native = EditText(context.activity)
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.setTextColor(theme.foreground.colorInt())
        native.setHintTextColor(theme.foreground.closestColor().withAlpha(0.5f).colorInt())
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
    var action: Action? = null
        set(value) {
            field = value
            native.setImeActionLabel(value?.title, KeyEvent.KEYCODE_ENTER)
            native.setOnEditorActionListener { v, actionId, event ->
                launch {
                    value?.onSelect?.invoke()
                }
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
