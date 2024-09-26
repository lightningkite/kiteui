package com.lightningkite.kiteui.views.direct

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.asDouble
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*

actual class NumberInput actual constructor(context: RContext): RView(context) {
    override val native = EditText(context.activity).apply {
        var block = false
        doAfterTextChanged {
            if(block) return@doAfterTextChanged
            block = true
            post {
                try {
                    if (it == null) return@post
                    numberAutocommaRepair(
                        dirty = it.toString(),
                        selectionStart = selectionStart,
                        selectionEnd = selectionEnd,
                        setResult = {
                            setText(it)
                        },
                        setSelectionRange = { start, end ->
                            setSelection(start, end)
                        }
                    )
                } finally {
                    block = false
                }
            }
        }
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return t
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value.toFloat())
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
        native.isAllCaps = theme.font.allCaps
    }
    actual val content: ImmediateWritable<Double?> = native.contentProperty().asDouble()
    actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
        }
    actual var action: Action? = null
        set(value) {
            field = value
            native.setImeActionLabel(value?.title, KeyEvent.KEYCODE_ENTER)
            native.setOnEditorActionListener { v, actionId, event ->
                launchManualCancel {
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
    @Suppress("UNCHECKED_CAST")
    actual var range: ClosedRange<Double>?
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
    init {
        keyboardHints = KeyboardHints.decimal
        align = Align.End
    }
}

