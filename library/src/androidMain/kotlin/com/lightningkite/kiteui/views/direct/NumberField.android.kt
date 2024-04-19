package com.lightningkite.kiteui.views.direct

import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.asDouble
import com.lightningkite.kiteui.views.ViewAction
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.launch

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NNumberField = EditText

actual val NumberField.content: Writable<Double?>
    get() {
        return this@content.native.content.asDouble()
    }
actual var NumberField.keyboardHints: KeyboardHints
    get() {
        return native.keyboardHints
    }
    set(value) {
        native.keyboardHints = value
    }
actual var NumberField.action: Action?
    get() {
        return ViewAction[native]
    }
    set(value) {
        ViewAction[native] = value
        native.setImeActionLabel(value?.title, KeyEvent.KEYCODE_ENTER)
        native.setOnEditorActionListener { v, actionId, event ->
            launch {
                value?.onSelect?.invoke()
            }
            value != null
        }
    }
actual var NumberField.hint: String
    get() {
        return this@hint.native.hint.toString()
    }
    set(value) {
        this@hint.native.hint = value
    }
actual var NumberField.range: ClosedRange<Double>?
    get() {
        return native.tag as? ClosedRange<Double>
    }
    set(value) {
        if (value == null) return

        native.tag = value
        native.doAfterTextChanged {
            try {
                if (it == null) return@doAfterTextChanged

                val string = it.toString()
                val doubleValue = string.toDouble()
                if (doubleValue < value.start || doubleValue > value.endInclusive) {
                    val newValue = doubleValue.coerceIn(value)
                    it.clear()
                    it.append(newValue.toString())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
actual var NumberField.align: Align
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
actual var NumberField.textSize: Dimension
    get() {
        return Dimension(native.textSize)
    }
    set(value) {
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, value.value.toFloat())
    }

@ViewDsl
actual inline fun ViewWriter.numberFieldActual(crossinline setup: NumberField.() -> Unit) {
    return viewElement(factory = ::EditText, wrapper = ::NumberField) {
        handleTheme<TextView>(native, foreground = applyTextColorFromTheme, viewLoads = true) {
            keyboardHints = KeyboardHints.decimal
            align = Align.End
            setup(this)
        }
    }
}