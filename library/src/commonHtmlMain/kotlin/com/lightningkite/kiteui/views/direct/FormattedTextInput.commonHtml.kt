package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.utils.repairFormatAndPosition
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

actual class FormattedTextInput actual constructor(context: RContext) : RViewWithAction(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }

    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }

    private var formatter: (String) -> String = { it }
    private var isRawData: (Char) -> Boolean = { true }
    actual fun format(
        isRawData: (Char) -> Boolean,
        formatter: (clean: String) -> String,
    ) {
        this.formatter = formatter
        this.isRawData = isRawData
    }

    actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String>, BaseListenable() {
        init {
            native.addEventListener("input") {
                repairFormatAndPosition(
                    dirty = native.attributes.valueString ?: "",
                    selectionStart = selectionStart,
                    selectionEnd = selectionEnd,
                    setResult = {
                        native.attributes.valueString = it
                    },
                    setSelectionRange = { start, end, -> setSelectionRange(start, end) },
                    isRawData = isRawData,
                    formatter = formatter,
                )
                invokeAllListeners()
            }
        }

        override var value: String
            get() = native.attributes.valueString?.filter(isRawData) ?: ""
            set(value) {
                val clean = value.filter(isRawData)
                val formatted = formatter(clean)
                if (native.attributes.valueString != formatted)
                    native.attributes.valueString = formatted
                    invokeAllListeners()
            }
    }
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            native.applyKeyboardHints(value)
        }

    actual inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }

    private var _align: Align? = null

    actual var align: Align?
        get() = _align
        set(value) {
            _align = value
            applyAlign(value ?: theme.font.align)
        }

    private fun applyAlign(value: Align) {
        native.style.textAlign = when (value) {
            Align.Start -> "start"
            Align.Center -> "center"
            Align.End -> "end"
            Align.Stretch -> "justify"
        }
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        applyAlign(_align ?: theme.theme.font.align)
    }

    actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) { native.attributes.disabled = !value }

    // by Claude
    override var accessibilityValue: String?
        get() = readStringValue(content)
        set(value) = writeStringValue(content, value)
    override val accessibilityActions get() = CLICK_AND_SET_VALUE_ACTIONS
    override fun performAccessibilityAction(action: String, value: String?) =
        performStringSetValueAction(content, action, value) { a, v -> super.performAccessibilityAction(a, v) }
}

expect val FormattedTextInput.selectionStart: Int?
expect val FormattedTextInput.selectionEnd: Int?
expect fun FormattedTextInput.setSelectionRange(start: Int, end: Int)