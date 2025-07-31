package com.lightningkite.kiteui.views.direct

import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.*


public actual class Switch public actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "input"
        native.attributes.type = "checkbox"
        native.classes.add("switch")
        native.classes.add("checkResponsive")
    }
    public actual val checked: ImmediateWritable<Boolean> = native.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value })

    public actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}