package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.kiteui.views.AiDriver


public actual class TextInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = textInputDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + textInputDriverActions()
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    public actual val content: MutableReactiveValue<String> = native.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
    public actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            native.applyKeyboardHints(value)

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

}