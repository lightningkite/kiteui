package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

public actual class ActivityIndicator public actual constructor(context: RContext): RView(context) {
    init {
        native.tag = "span"
        native.classes.add("spinner")
    }
}