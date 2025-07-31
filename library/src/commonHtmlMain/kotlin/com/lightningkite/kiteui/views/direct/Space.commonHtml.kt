package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.*


public actual class Space public actual constructor(context: RContext, multiplier: Double) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    init {
        native.tag = "span"
        native.classes.add("kiteui-space")
        native.setStyleProperty("--space-multiplier", multiplier.toString())
    }
}
