package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView

actual class ActivityIndicator actual constructor(context: ElementContext): RView(context) {
    init {
        native.tag = "span"
        native.classes.add("spinner")
    }
}