package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.AriaRole
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Writable
import com.lightningkite.kiteui.views.*


actual class Switch actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "input"
        native.attributes.type = "checkbox"
        native.classes.add("switch")
        native.classes.add("checkResponsive")
        ariaRole = AriaRole.Switch
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
}
