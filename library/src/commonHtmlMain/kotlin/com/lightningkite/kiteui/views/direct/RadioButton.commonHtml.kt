package com.lightningkite.kiteui.views.direct

import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.ReadableState
import com.lightningkite.readable.Writable
import com.lightningkite.kiteui.views.*

actual class RadioButton actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "input"
        native.attributes.type = "radio"
        native.classes.add("checkbox")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }
    override fun hasAlternateBackedStates(): Boolean = true

    actual val checked: ImmediateWritable<Boolean> = native.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value })

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
