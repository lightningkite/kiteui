package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.views.*


actual class Checkbox actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "input"
        native.attributes.type = "checkbox"
        native.classes.add("checkbox")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }

    override fun hasAlternateBackedStates(): Boolean = true

    actual val checked: ImmediateWritable<Boolean> = native.vprop(
        "input",
        { attributes.checked != false },
        { value -> attributes.checked = value }
    )

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
