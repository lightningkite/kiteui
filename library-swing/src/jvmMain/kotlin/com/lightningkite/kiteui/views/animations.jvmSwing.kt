package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

actual fun RView.animateIn(transition: ScreenTransition, done: (() -> Unit)?) {
    // No animation support yet
    done?.invoke()
}

actual fun RView.animateOut(transition: ScreenTransition, done: (() -> Unit)?) {
    // No animation support yet - just call complete immediately
    done?.invoke()
}
