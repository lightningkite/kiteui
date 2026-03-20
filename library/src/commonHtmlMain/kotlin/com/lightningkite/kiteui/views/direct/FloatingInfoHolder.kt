package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementWriter

expect class FloatingInfoHolder(source: Element) {
    var preferredDirection: PopoverPreferredDirection
    var menuGenerator: Frame.() -> Unit
    fun open()
    fun block()
    fun close()
}