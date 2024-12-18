package com.lightningkite.kiteui.views.direct

import android.graphics.Paint
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
import androidx.core.text.set
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*

actual class NumberInput actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = EditText(context.activity).focusIsKeyboard().apply {
        var block = false
        doAfterTextChanged { _ ->
            if(block) return@doAfterTextChanged
            block = true
            post {
                val str = this.text.toString()
                try {
                    if (str == null) return@post
                    numberAutocommaRepair(
                        dirty = str,
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
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
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
        native.paintFlags = native.paintFlags and (Paint.UNDERLINE_TEXT_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).inv() or
                (if(theme.font.underline) Paint.UNDERLINE_TEXT_FLAG else 0) or
                (if(theme.font.strikethrough) Paint.STRIKE_THRU_TEXT_FLAG else 0)
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

    override fun actionSet(value: Action?) {
        super.actionSet(value)
        native.setImeActionLabel(value?.title, KeyEvent.KEYCODE_ENTER)
        native.setOnEditorActionListener { v, actionId, event ->
            // Design decision: whenever an action is set on a TextField, we will release focus first
            native.clearFocus()
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

    init {
        keyboardHints = KeyboardHints.decimal
        align = Align.End
    }
}

