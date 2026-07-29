package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementWriter

public expect class FloatingInfoHolder(source: Element) {
    public var preferredDirection: PopoverPreferredDirection
    public var menuGenerator: Frame.() -> Unit
    public fun open()
    public fun block()
    public fun close()
}