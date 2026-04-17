package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.popoverWriter

actual class FloatingInfoHolder actual constructor(val source: RView) {
    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    actual var menuGenerator: Frame.() -> Unit = {}
    private var existingView: RView? = null

    actual fun open() {
        if (existingView != null) return
        val overlay = source.overlayFrame ?: return
        with(source.popoverWriter(overlay) { close() }) {
            frame {
                this@FloatingInfoHolder.existingView = this
                menuGenerator(this)
            }
        }
    }

    actual fun block() {}

    actual fun close() {
        val v = existingView ?: return
        existingView = null
        source.overlayFrame?.removeChild(v)
    }
}
