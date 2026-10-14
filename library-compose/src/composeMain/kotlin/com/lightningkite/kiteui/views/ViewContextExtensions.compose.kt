package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.core.Release

public actual fun ElementContext.overlay(
    modal: Boolean,
    navClosable: Boolean,
    transition: ScreenTransitions,
    body: ContainerElement.(remove: () -> Unit) -> Unit
) {
    with(overlayFrame ?: run {
        Log.warn("WARN!! overlay abandoned because no overlayFrame set")
        return
    }) {
        var willRemove: Element? = null
        var unregister: Release? = null

        val close: () -> Unit = {
            willRemove?.let {
                // Clear state before starting the exit animation so a second close() call
                // (e.g. a double tap on a dismiss button) during the animation is a no-op
                // instead of animating/removing the same element twice.
                willRemove = null
                unregister?.invoke()
                unregister = null
                it.animateOut(transition.reverse) {
                    this@with.removeChild(it)
                }
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