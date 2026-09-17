package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.valueDouble

public actual class ProgressBar actual constructor(context: ElementContext): NativeElement(context) {
    init {
        native.tag = "progress"
        native.setAttribute("aria-label", "Progress")
    }

    public actual var ratio: Float
        get() = native.attributes.valueDouble?.toFloat() ?: 0f
        set(value) {
            native.attributes.valueDouble = value.toDouble()
        }
}
