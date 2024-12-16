package com.lightningkite.kiteui.views.direct

import android.graphics.Paint
import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.lens
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.utils.repairFormatAndPosition
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction

actual class FormattedTextInput actual constructor(context: RContext) : RViewWithAction(context) {
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
        native.paintFlags = native.paintFlags and (Paint.UNDERLINE_TEXT_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).inv() or
                (if(theme.font.underline) Paint.UNDERLINE_TEXT_FLAG else 0) or
                (if(theme.font.strikethrough) Paint.STRIKE_THRU_TEXT_FLAG else 0)
        useAllCaps = theme.font.allCaps
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value.toFloat())
    }

    private var isRawData: (Char) -> Boolean = { true }
    private var formatter: (clean: String) -> String = { it }
    actual fun format(isRawData: (Char) -> Boolean, formatter: (clean: String) -> String) {
        this.isRawData = isRawData
        this.formatter = formatter
    }
    actual val content: ImmediateWritable<String> = native.contentProperty().lens(
        get = { it.filter(isRawData) },
        set = { formatter(it.filter(isRawData)) }
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

    actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
            useSensitiveDotMask = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
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

    init {
        keyboardHints = KeyboardHints(KeyboardCase.Sentences)
    }

}