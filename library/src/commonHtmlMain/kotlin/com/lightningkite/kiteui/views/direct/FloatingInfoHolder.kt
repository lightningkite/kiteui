package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter

expect class FloatingInfoHolder(source: RView) {
    var preferredDirection: PopoverPreferredDirection
    var menuGenerator: Stack.() -> Unit
    fun open()
    fun block()
    fun close()
}