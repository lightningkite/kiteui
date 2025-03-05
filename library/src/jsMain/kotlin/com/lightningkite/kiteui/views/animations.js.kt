package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import org.w3c.dom.HTMLElement

actual fun RView.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    val keyframeName = context.kiteUiCss.transition(transition)
    val transitionTime = theme.transitionDuration
    native.onElement { (it as HTMLElement).style.animation = "${keyframeName}-enter $transitionTime forwards" }
    done?.let { afterTimeout(transitionTime.inWholeMilliseconds, it) }
}
actual fun RView.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    val keyframeName = context.kiteUiCss.transition(transition)
    val transitionTime = theme.transitionDuration
    native.onElement { (it as HTMLElement).style.animation = "${keyframeName}-exit $transitionTime forwards" }
    done?.let { afterTimeout(transitionTime.inWholeMilliseconds, it) }
}