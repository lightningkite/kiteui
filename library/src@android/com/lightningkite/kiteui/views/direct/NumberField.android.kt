package com.lightningkite.kiteui.views.direct

import android.graphics.Paint
import android.util.TypedValue
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.kiteui.views.AiDriver

public actual class NumberInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = numberInputDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + numberInputDriverActions()

    override val native: EditText = EditText(context.activity).focusIsKeyboard().apply {
        var block = false
        doAfterTextChanged { _ ->
            if(block) return@doAfterTextChanged
            block = true
            post {
                val str = this.text.toString()
                try {
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

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        val theme = theme.theme
        _fontAndStyle = theme.font
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value)
        native.setTextColor(theme.foreground.colorInt())
        native.setHintTextColor(theme.foreground.closestColor().withAlpha(0.5f).colorInt())
        native.setTypeface(
            TypefaceCompat.create(
                native.context,
                theme.font.font.toTypeface(),
                theme.font.weight,
                theme.font.italic
            )
        )
        native.paintFlags = native.paintFlags and (Paint.UNDERLINE_TEXT_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).inv() or
                (if(theme.font.underline) Paint.UNDERLINE_TEXT_FLAG else 0) or
                (if(theme.font.strikethrough) Paint.STRIKE_THRU_TEXT_FLAG else 0)
        native.isAllCaps = theme.font.allCaps
        applyAlign(_align ?: theme.font.align)
    }
    public actual val content: MutableReactiveValue<Double?> = native.contentProperty().asDouble()
    public actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
        }

    override fun nativeSetAction(action: Action?) {
        super.nativeSetAction(action)
        native.setImeActionLabel(action?.title, KeyEvent.KEYCODE_ENTER)
        native.setOnEditorActionListener { v, actionId, event ->
            action?.startAction(this)
            action != null
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

