package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class CircularProgress actual constructor(context: RContext) :
    RView(context) {
    actual var ratio: Float
        get() = TODO("Not yet implemented")
        set(value) {}

    // by Claude
    override var accessibilityValue: String?
        get() = ratio.toString()
        set(value) { super.accessibilityValue = value }
}