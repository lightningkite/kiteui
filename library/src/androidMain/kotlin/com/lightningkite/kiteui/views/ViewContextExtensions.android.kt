package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.l2.overlayFrame

actual fun ElementContext.overlay(
    modal: Boolean,
    transition: ScreenTransitions,
    body: ContainerElement.(remove: () -> Unit) -> Unit
) {
    var willRemove: Element? = null
    with(overlayFrame ?: run {
        println("WARN!! overlay abandoned because no overlayFrame set")
        return
    }) {
        withoutAnimation {
            beforeSetupContainer {
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