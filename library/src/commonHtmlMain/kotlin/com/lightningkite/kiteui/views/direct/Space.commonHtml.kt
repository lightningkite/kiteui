package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.*


@InternalKiteUi
public actual class Space actual constructor(context: RContext, multiplier: Double) : RView(context) {
    init {
        native.tag = "span"
        native.classes.add("kiteui-space")
        native.setStyleProperty("--space-multiplier", multiplier.toString())
    }
}
