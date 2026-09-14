package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*


public actual class Space actual constructor(context: ElementContext, multiplier: Double) : NativeElement(context) {
    init {
        native.tag = "span"
        native.classes.add("kiteui-space")
        native.setStyleProperty("--space-multiplier", multiplier.toString())
    }
}
