package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.kiteui.views.l2.overlayFrame

actual fun ElementWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    body: ViewWriter.(remove: () -> Unit) -> Unit
) {
    var willRemove: Element? = null
    with(context.overlayFrame ?: return) {
        withoutAnimation {
            beforeSetup {
                animateIn(transition.forward)
                willRemove = this
            }.body {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        this@with.removeChild(it)
                    }
                }
            }
        }
    }
}