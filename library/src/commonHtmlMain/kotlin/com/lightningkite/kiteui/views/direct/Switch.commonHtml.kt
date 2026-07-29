package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*


public actual class Switch actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = switchDriverValue()
    override val driverActions get() = super.driverActions + switchDriverActions()

    init {
        native.tag = "input"
        native.attributes.type = "checkbox"
        native.setAttribute("role", "switch")
        native.classes.add("switch")
        native.classes.add("checkResponsive")
    }

    public actual val checked: MutableReactiveValue<Boolean> = native.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value }
    )
}