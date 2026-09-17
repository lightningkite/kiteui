package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import org.w3c.dom.HTMLElement

public actual fun Element.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    val keyframeName = context.kiteUiCss.transition(transition)
    val transitionTime = theme.transitionDuration
    val easing = transition.easing
    val easingCss = "cubic-bezier(${easing.x1}, ${easing.y1}, ${easing.x2}, ${easing.y2})"
    native.onElement { (it as HTMLElement).style.animation = "${keyframeName}-enter $transitionTime $easingCss forwards" }
    done?.let { afterTimeout(transitionTime.inWholeMilliseconds, it) }
}
public actual fun Element.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    val keyframeName = context.kiteUiCss.transition(transition)
    val transitionTime = theme.transitionDuration
    val easing = transition.easing
    val easingCss = "cubic-bezier(${easing.x1}, ${easing.y1}, ${easing.x2}, ${easing.y2})"
    native.onElement { (it as HTMLElement).style.animation = "${keyframeName}-exit $transitionTime $easingCss forwards" }
    done?.let { afterTimeout(transitionTime.inWholeMilliseconds, it) }
}