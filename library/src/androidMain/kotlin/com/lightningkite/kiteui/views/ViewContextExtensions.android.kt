package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.l2.overlayFrame

actual fun ViewWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    body: ViewWriter.(remove: () -> Unit) -> Unit
) {
    var willRemove: RView? = null
    with(overlayFrame ?: return) {
        withoutAnimation {
            beforeNextElementSetup {
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