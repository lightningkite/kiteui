package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.signal.Writable
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.kiteui.views.*


public actual class Checkbox public actual constructor(context: RContext) : RView(context) {
    init {
        themeChoice += ClickableSemantic
        native.tag = "input"
        native.attributes.type = "checkbox"
        native.classes.add("checkbox")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }

    public actual val checked: ImmediateWritable<Boolean> = native.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value }
    )

    public actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
