package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*


actual class Switch actual constructor(context: ElementContext) : RView(context) {
    override val driverValue: String? get() = switchDriverValue()
    override val driverActions get() = super.driverActions + switchDriverActions()
    init {
        native.tag = "input"
        native.attributes.type = "checkbox"
        native.classes.add("switch")
        native.classes.add("checkResponsive")
    }
    actual val checked: MutableReactiveValue<Boolean> = native.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value })

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}