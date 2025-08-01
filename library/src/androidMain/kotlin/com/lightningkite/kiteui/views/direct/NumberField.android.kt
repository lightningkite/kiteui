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
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

public actual class NumberInput public actual constructor(context: RContext) : RViewWithAction(context) {
    override val native: EditText = EditText(context.activity).focusIsKeyboard().apply {
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
                        allowDecimal = keyboardHints.type.allowDecimal,
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

    public actual var enabled: Boolean
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

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
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
    actual val content: MutableReactiveValue<Double?> = native.contentProperty().asDouble()
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
            value?.startAction(this)
            value != null
        }
    }

    public actual var hint: String
        get() {
            return native.hint.toString()
        }
        set(value) {
            native.hint = value
        }

    @Suppress("UNCHECKED_CAST")
    public actual var range: ClosedRange<Double>?
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
    public actual var align: Align
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

