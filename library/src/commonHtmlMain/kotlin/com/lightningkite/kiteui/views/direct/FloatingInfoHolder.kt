package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView

expect class FloatingInfoHolder(source: RView, anchor: RView? = null) {
    var preferredDirection: PopoverPreferredDirection
    var menuGenerator: Frame.() -> Unit
    fun open()
    fun block()
    fun close()
}
