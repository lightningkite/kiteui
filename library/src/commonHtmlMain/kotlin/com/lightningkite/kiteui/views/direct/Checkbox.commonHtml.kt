package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


actual class Checkbox actual constructor(context: RContext) : RView(context) {
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

    // by Claude
    override var accessibilityValue: String?
        get() = readBooleanValue(checked)
        set(value) = writeBooleanValue(checked, value)
    override val accessibilityActions get() = CLICK_AND_SET_VALUE_ACTIONS
    override fun performAccessibilityAction(action: String, value: String?) =
        performBooleanAction(checked, action, value) { a, v -> super.performAccessibilityAction(a, v) }
}
