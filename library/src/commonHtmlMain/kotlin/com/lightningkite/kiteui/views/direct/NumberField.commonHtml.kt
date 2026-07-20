package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

public actual class NumberInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = numberInputDriverValue()
    override val driverActions: Map<String, suspend (List<String>) -> String> get() = super.driverActions + numberInputDriverActions()
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    public actual val content: MutableReactiveValue<Double?> = object : MutableReactiveValue<Double?>, BaseListenable() {
        init {
            native.addEventListener("input") {
                numberAutocommaRepair(
                    dirty = native.attributes.valueString ?: "",
                    selectionStart = selectionStart,
                    selectionEnd = selectionEnd,
                    allowDecimal = keyboardHints.type.allowDecimal,
                    setResult = {
                        native.attributes.valueString = it
                    },
                    setSelectionRange = {start, end, -> setSelectionRange(start, end)}
                )
                invokeAllListeners()
            }
        }

        override var value: Double?
            get() = native.attributes.valueString?.filter { it.isDigit() || it in setOf('-', '.') }?.toDoubleOrNull()
            set(value) {
                if(native.attributes.valueString != value?.commaString()) {
                    native.attributes.valueString = value?.commaString()
                    invokeAllListeners()
                }
            }
    }

    public actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            native.attributes.type = when (value.type) {
                KeyboardType.Text -> "text"
                KeyboardType.Decimal -> "text"
                KeyboardType.Integer -> "text"
                KeyboardType.Phone -> "tel"
                KeyboardType.Email -> "text"
                KeyboardType.IntegerWithNegative -> "text"
                KeyboardType.DecimalWithNegative -> "text"
            }
            native.attributes.inputMode = when (value.type) {
                KeyboardType.Text -> "text"
                KeyboardType.Decimal -> "decimal"
                KeyboardType.Integer -> "numeric"
                KeyboardType.Phone -> "tel"
                KeyboardType.Email -> "email"
                // Number inputs are not guaranteed to include the '-' sign as an option, fall back to regular text input to ensure negative sign is accessible.
                KeyboardType.IntegerWithNegative -> if (usingWebOnMobile()) "text" else "numeric"
                KeyboardType.DecimalWithNegative -> if (usingWebOnMobile()) "text" else "decimal"
            }

            when (value.autocomplete) {
                AutoComplete.Email -> {
                    native.attributes.type = "email"
                    native.attributes.autocomplete = "email"
                }

                AutoComplete.Password -> {
                    native.attributes.type = "password"
                    native.attributes.autocomplete = "current-password"
                }

                AutoComplete.NewPassword -> {
                    native.attributes.type = "password"
                    native.attributes.autocomplete = "new-password"
                }

                AutoComplete.Phone -> {
                    native.attributes.autocomplete = "tel"
                }

                AutoComplete.OneTimeCode,null -> {
                    native.attributes.autocomplete = "off"
                }
            }
        }
    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
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

    public actual var range: ClosedRange<Double>? = null
        set(value) {
            field = value
            value?.let {
                native.attributes.maxDouble = it.start
                native.attributes.maxDouble = it.endInclusive
            } ?: run {
                native.attributes.maxDouble = null
                native.attributes.maxDouble = null
            }
        }
}

public expect val NumberInput.selectionStart: Int?
public expect val NumberInput.selectionEnd: Int?
public expect fun NumberInput.setSelectionRange(start: Int, end: Int)

public expect fun usingWebOnMobile(): Boolean