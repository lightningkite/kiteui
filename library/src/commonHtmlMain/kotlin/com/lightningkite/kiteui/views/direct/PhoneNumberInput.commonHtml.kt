package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.BaseListenable
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.utils.USPhoneNumberRepair
import com.lightningkite.kiteui.utils.formatUSPhoneNumber
import com.lightningkite.kiteui.views.*

actual class PhoneNumberInput actual constructor(context: RContext) : RViewWithAction(context) {
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

    actual val content: ImmediateWritable<String> = object : ImmediateWritable<String>, BaseListenable() {
        init {
            native.addEventListener("input") {
                // TODO: Need to fix selection issues
                native.attributes.valueString = native.attributes.valueString?.formatUSPhoneNumber()
                invokeAllListeners()
            }
        }
        override var value: String
            get() = native.attributes.valueString?.filter { it.isDigit() } ?: ""
            set(value) {
                val phone = value.formatUSPhoneNumber()
                if (native.attributes.valueString != phone)
                    native.attributes.valueString = phone
                    invokeAllListeners()
            }
    }

    // Keyboard hint phone - always
    init {
        native.attributes.type = "tel"
        native.attributes.inputMode = "tel"
        native.attributes.autocomplete = "tel"
    }

    actual inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }

    actual var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }

    actual var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value
        }

    actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) { native.attributes.disabled = !value }
}

expect val PhoneNumberInput.selectionStart: Int?
expect val PhoneNumberInput.selectionEnd: Int?
expect fun PhoneNumberInput.setSelectionRange(start: Int, end: Int)