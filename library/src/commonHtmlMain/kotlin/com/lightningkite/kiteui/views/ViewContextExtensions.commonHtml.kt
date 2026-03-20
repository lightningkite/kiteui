package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.kiteui.views.l2.overlayFrame

actual fun ViewWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    body: ViewWriter.(remove: () -> Unit) -> Unit
) {
    var willRemove: Element? = null
    with(overlayFrame ?: return) {
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