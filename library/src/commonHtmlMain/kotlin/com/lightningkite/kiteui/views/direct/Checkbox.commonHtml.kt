package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*


actual class Checkbox actual constructor(context: ElementContext) : RView(context) {
    override val driverValue: String? get() = checkboxDriverValue()
    override val driverActions get() = super.driverActions + checkboxDriverActions()
    init {
        themeChoice += ClickableSemantic
        native.tag = "input"
        native.attributes.type = "checkbox"
        native.classes.add("checkbox")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }

    actual val checked: MutableReactiveValue<Boolean> = native.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value }
    )

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
