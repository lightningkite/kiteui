package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

actual class RadioButton actual constructor(context: RContext) : RView(context) {
    override val driverValue: String? get() = radioDriverValue()
    override val driverActions get() = super.driverActions + radioDriverActions()
    init {
        themeChoice += ClickableSemantic
        native.tag = "input"
        native.attributes.type = "radio"
        native.classes.add("radio")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        native.classes.add("transition")
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
