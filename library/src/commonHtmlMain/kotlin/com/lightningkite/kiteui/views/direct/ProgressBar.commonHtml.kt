package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.valueDouble

public actual class ProgressBar public actual constructor(context: RContext): RView(context) {
    init {
        native.tag = "progress"
    }

    public actual var ratio: Float
        get() = native.attributes.valueDouble?.toFloat() ?: 0f
        set(value) {
            native.attributes.valueDouble = value.toDouble()
        }
}
