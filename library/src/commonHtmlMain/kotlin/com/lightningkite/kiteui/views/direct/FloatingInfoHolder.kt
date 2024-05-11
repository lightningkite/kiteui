package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter

expect class FloatingInfoHolder(source: RView) {
    var preferredDirection: PopoverPreferredDirection
    var menuGenerator: ViewWriter.() -> Unit
    fun open()
    fun close()
}