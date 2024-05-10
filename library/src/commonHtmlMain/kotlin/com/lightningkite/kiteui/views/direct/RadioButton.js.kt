package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.UseBackground

actual class RadioButton actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "input"
        native.attributes["type"] = "radio"
        native.classes.add("checkbox")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }

    init {
        useBackground = UseBackground.Yes
    }

    actual val checked: Writable<Boolean> = native.vprop(
        "input",
        { attributes["checked"] != null },
        { value -> attributes["checked"] = "true".takeIf { value } })

    actual inline var enabled: Boolean
        get() = native.attributes["disabled"] == null
        set(value) {
            native.attributes["disabled"] = "true".takeUnless { value }
        }
}
