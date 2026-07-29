package com.lightningkite.kiteui.views.direct

import android.graphics.Paint
import android.text.method.PasswordTransformationMethod
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
import com.lightningkite.kiteui.utils.repairFormatAndPosition
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

public actual class FormattedTextInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = formattedTextInputDriverValue()
    override val driverActions get() = super.driverActions + formattedTextInputDriverActions()

    override val native = EditText(context.activity).focusIsKeyboard().apply {
        var block = false
        doAfterTextChanged { _ ->
            if(block) return@doAfterTextChanged
            block = true
            post {
                val str = this.text.toString()
                try {
                    if (str == null) return@post
                    repairFormatAndPosition(
                        dirty = str,
                        selectionStart = selectionStart,
                        selectionEnd = selectionEnd,
                        setResult = {
                            setText(it)
                        },
                        setSelectionRange = { start, end ->
                            setSelection(start, end)
                        },
                        isRawData = isRawData,
                        formatter = formatter,
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
        useAllCaps = theme.font.allCaps
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value.toFloat())
        applyAlign(_align ?: theme.font.align)
    }

    private var isRawData: (Char) -> Boolean = { true }
    private var formatter: (clean: String) -> String = { it }
    public actual fun format(isRawData: (Char) -> Boolean, formatter: (clean: String) -> String) {
        this.isRawData = isRawData
        this.formatter = formatter
    }
    public actual val content: MutableReactiveValue<String> = native.contentProperty().lens(
        get = { it.filter(isRawData) },
        set = { formatter(it.filter(isRawData)) }
    )

    private var useSensitiveDotMask = false
        set(value) {
            field = value
            updateTransformationMethod()
        }
    private var useAllCaps = false
        set(value) {
            field = value
            updateTransformationMethod()
        }
    private fun updateTransformationMethod() {
        // Calling EditText.setAllCaps() calls EditText.setTransformationMethod() under the hood which leads to side
        // effects when we are using the transformationMethod for password masking; ONLY enforce all caps when we are
        // not masking passwords as password masking takes priority
        if (useSensitiveDotMask) {
            native.transformationMethod = PasswordTransformationMethod.getInstance()
        } else {
            native.isAllCaps = theme.font.allCaps
        }
    }

    public actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
            useSensitiveDotMask = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
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
        keyboardHints = KeyboardHints(KeyboardCase.Sentences)
    }

}