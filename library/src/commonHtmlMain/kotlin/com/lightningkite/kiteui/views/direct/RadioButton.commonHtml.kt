package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.ReadableState
import com.lightningkite.readable.Writable
import com.lightningkite.kiteui.views.*

actual class RadioButton actual constructor(context: RContext) : RView(context), InputAccessibility {
    init {
        themeChoice += ClickableSemantic
        native.tag = "input"
        native.attributes.type = "radio"
        native.classes.add("radio")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }

    actual val checked: ImmediateWritable<Boolean> = native.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value })

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }

    override var ariaRequired: Boolean? = null
        set(value) {
            field = value
            if (value == null) {
                native.setAttribute("aria-required", null)
            } else {
                native.setAttribute("aria-required", value.toString())
            }
        }
}
