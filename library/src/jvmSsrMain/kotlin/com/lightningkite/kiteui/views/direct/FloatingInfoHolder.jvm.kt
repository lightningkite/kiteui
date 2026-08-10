package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.*

public actual class FloatingInfoHolder actual constructor(public val source: Element, public val anchor: Element?) {
    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    public actual var menuGenerator: Frame.() -> Unit = {}
    private var existingView: Element? = null

    public actual fun open() {
        if (existingView != null) return
        val overlay = source.context.overlayFrame ?: return
        with(source.popoverWriter(overlay) { close() }) {
            frame {
                this@FloatingInfoHolder.existingView = this
                menuGenerator(this)
            }
        }
    }

    public actual fun block() {}

    public actual fun close() {
        val v = existingView ?: return
        existingView = null
        source.context.overlayFrame?.removeChild(v)
    }
}
