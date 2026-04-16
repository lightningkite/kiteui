package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

actual class ActivityIndicator actual constructor(context: ElementContext): NativeElement(context) {
    init {
        native.tag = "span"
        native.classes.add("spinner")
        native.setAttribute("role", "status")
        native.setAttribute("aria-label", "Loading")
    }
}