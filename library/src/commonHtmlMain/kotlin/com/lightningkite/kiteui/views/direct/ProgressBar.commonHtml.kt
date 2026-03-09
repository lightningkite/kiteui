package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.valueDouble

actual class ProgressBar actual constructor(context: RContext): RView(context) {
    init {
        native.tag = "progress"
    }

    actual var ratio: Float
        get() = native.attributes.valueDouble?.toFloat() ?: 0f
        set(value) {
            native.attributes.valueDouble = value.toDouble()
        }

    // by Claude
    override var accessibilityValue: String?
        get() = ratio.toString()
        set(value) { super.accessibilityValue = value }

    // by Claude
    override val accessibilityType: String get() = "ProgressBar"
}
