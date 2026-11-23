package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.reformatWithSelection
import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.kiteui.views.AiDriver

public actual class FormattedTextInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = formattedTextInputDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + formattedTextInputDriverActions()
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

    public actual var format: TextFormat = TextFormat.None
        set(value) {
            val data = content.value
            field = value
            content.value = data
        }

    public actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String>, BaseListenable() {
        private var lastValue = ""

        private fun notifyIfChanged() {
            val now = value
            if (now == lastValue) return
            lastValue = now
            invokeAllListeners()
        }

        private fun reformatTyped() {
            val typed = native.attributes.valueString ?: ""
            val result = format.reformatWithSelection(typed, (selectionStart ?: typed.length) until (selectionEnd ?: typed.length))
            if (result.text != typed) native.attributes.valueString = result.text
            setSelectionRange(result.selection.first, result.selection.last + 1)
            notifyIfChanged()
        }

        init {
            // Claude: rewriting the value mid-composition breaks IME input, so wait for the composition to finish.
            var composing = false
            native.addEventListener("compositionstart") { composing = true }
            native.addEventListener("compositionend") {
                composing = false
                reformatTyped()
            }
            native.addEventListener("input") { if (!composing) reformatTyped() }
        }

        override var value: String
            get() = native.attributes.valueString?.filter(format::isRawData) ?: ""
            set(value) {
                val formatted = format.format(value.filter(format::isRawData))
                if (native.attributes.valueString != formatted) native.attributes.valueString = formatted
                notifyIfChanged()
            }
    }
    public actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            native.applyKeyboardHints(value)
        }

    public actual inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }

    private var _align: Align? = null

    public actual var align: Align?
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

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        applyAlign(_align ?: theme.theme.font.align)
    }
}

public expect val FormattedTextInput.selectionStart: Int?
public expect val FormattedTextInput.selectionEnd: Int?
public expect fun FormattedTextInput.setSelectionRange(start: Int, end: Int)