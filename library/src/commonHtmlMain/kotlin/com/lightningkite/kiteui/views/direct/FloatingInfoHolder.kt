package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView

public expect class FloatingInfoHolder(source: RView) {
    public var preferredDirection: PopoverPreferredDirection
    public var menuGenerator: Frame.() -> Unit
    public fun open()
    public fun block()
    public fun close()
}