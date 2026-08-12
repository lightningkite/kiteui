package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.core.Release

public actual fun ElementContext.overlay(
    modal: Boolean,
    navClosable: Boolean,
    transition: ScreenTransitions,
    body: ContainerElement.(remove: () -> Unit) -> Unit
) {
    with(overlayFrame) {
        var willRemove: Element? = null
        var unregister: Release? = null

        val close: () -> Unit = {
            willRemove?.let {
                it.animateOut(transition.reverse) {
                    this@with.removeChild(it)
                    willRemove = null
                }
                unregister?.invoke()
                unregister = null
            }
        }

        withoutAnimation {
            if (navClosable) unregister = pushDismissableDialog(close)

            beforeSetupContainer {
                if (willRemove != null) throw IllegalStateException("overlay writer produced more than one element, when only one is allowed.")
                animateIn(transition.forward)
                willRemove = this
            }.body(close)
        }
    }
}