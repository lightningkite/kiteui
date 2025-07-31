package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView

public actual class FloatingInfoHolder public actual constructor(source: RView) {
    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    public actual var menuGenerator: Frame.() -> Unit = {}

    public actual fun open() {
    }

    public actual fun block() {
    }

    public actual fun close() {
    }
}