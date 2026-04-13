package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.*

actual class FloatingInfoHolder actual constructor(val source: Element) {
    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    actual var menuGenerator: Frame.() -> Unit = {}
    private var existingView: Element? = null

    actual fun open() {
        if (existingView != null) return
        val overlay = source.context.overlayFrame ?: return
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
        source.context.overlayFrame?.removeChild(v)
    }
}
