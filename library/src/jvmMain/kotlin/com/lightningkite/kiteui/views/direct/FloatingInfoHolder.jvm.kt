package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter

actual class FloatingInfoHolder actual constructor(source: RView) {
    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    actual var menuGenerator: ViewWriter.() -> Unit = {}

    actual fun open() {
    }

    actual fun block() {
    }

    actual fun close() {
    }
}