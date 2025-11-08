package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement

actual fun SwapView.nativeSwap(
    transition: ScreenTransition,
    createNewView: ViewWriter.() -> Unit?
) {
    val keyframeName = context.kiteUiCss.transition(transition)

    val transitionTime = theme.transitionDuration
    previousLast?.let { view ->
        view.native.onElement { (it as HTMLElement).style.animation = "${keyframeName}-exit $transitionTime forwards" }
        launch {
            delay(theme.transitionDuration)
            removeChild(view)
        }
    }
    withoutAnimation {
        createNewView()
    }
    children.lastOrNull().takeUnless { it == previousLast }?.let { newView ->
        previousLast = newView
        shown = true
        newView.native.onElement { (it as HTMLElement).style.animation = "${keyframeName}-enter $transitionTime forwards" }
    } ?: run {
        previousLast = null
        if (shown) {
            afterTimeout(transitionTime.inWholeMilliseconds) {
                shown = false
            }
        }
    }
}