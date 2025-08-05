package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
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

@InternalKiteUi
public actual class FormattedTextInput public actual constructor(context: RContext) : RViewWithAction(context) {
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
    public actual fun format(
        isRawData: (Char) -> Boolean,
        formatter: (clean: String) -> String,
    ) {
        this.formatter = formatter
        this.isRawData = isRawData
    }

    public actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String>, BaseListenable() {
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
    public actual var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }

    public actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) { native.attributes.disabled = !value }
}

public expect val FormattedTextInput.selectionStart: Int?
public expect val FormattedTextInput.selectionEnd: Int?
public expect fun FormattedTextInput.setSelectionRange(start: Int, end: Int)