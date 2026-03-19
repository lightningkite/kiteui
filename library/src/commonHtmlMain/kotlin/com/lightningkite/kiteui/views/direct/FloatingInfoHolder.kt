package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.Element

expect class FloatingInfoHolder(source: Element) {
    var preferredDirection: PopoverPreferredDirection
    var menuGenerator: Frame.() -> Unit
    fun open()
    fun block()
    fun close()
}