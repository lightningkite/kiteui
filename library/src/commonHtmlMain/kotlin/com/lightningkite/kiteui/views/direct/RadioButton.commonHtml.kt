package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.kiteui.views.AiDriver

public actual class RadioButton actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    actual override val underlyingNativeElement: RadioButton get() = this

    override val driverValue: String? get() = radioDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + radioDriverActions()
    init {
        themeChoice += ClickableSemantic
        native.tag = "input"
        native.attributes.type = "radio"
        native.classes.add("radio")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }

    public actual val checked: MutableReactiveValue<Boolean> = native.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value }
    )
}
