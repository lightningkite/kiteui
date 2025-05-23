package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.ReadableState
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.*

actual class RadioButton actual constructor(context: RContext) : RView(context) {
    init {
        themeChoice += ClickableSemantic
        native.tag = "input"
        native.attributes.type = "radio"
        native.classes.add("radio")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
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
