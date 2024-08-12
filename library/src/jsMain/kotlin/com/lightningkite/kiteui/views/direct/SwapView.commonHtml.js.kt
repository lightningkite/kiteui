package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import org.w3c.dom.HTMLElement

actual fun SwapView.nativeSwap(
    transition: ScreenTransition,
    createNewView: ViewWriter.() -> Unit
) {
    val keyframeName = context.kiteUiCss.transition(transition)

    val transitionTime = theme.transitionDuration
    previousLast?.let { view ->
        view.shutdown()
        view.native.onElement { (it as HTMLElement).style.animation = "${keyframeName}-exit $transitionTime forwards" }
        afterTimeout(transitionTime.inWholeMilliseconds) {
            removeChild(view)
        }
    }
    previousLast = null
    withoutAnimation {
        createNewView()
    }
    children.lastOrNull().takeUnless { it == previousLast }?.let { newView ->
        previousLast = newView
        exists = true
        opacity = 1.0
        newView.native.onElement { (it as HTMLElement).style.animation = "${keyframeName}-enter $transitionTime forwards" }
    } ?: run {
        if (exists) {
            opacity = 0.0
            afterTimeout(transitionTime.inWholeMilliseconds) {
                exists = false
            }
        }
    }
}