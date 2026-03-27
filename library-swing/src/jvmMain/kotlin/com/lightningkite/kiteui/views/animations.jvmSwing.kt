package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

actual fun com.lightningkite.kiteui.views.Element.animateIn(transition: ScreenTransition, done: (() -> Unit)?) {
    // No animation support yet
    done?.invoke()
}

actual fun com.lightningkite.kiteui.views.Element.animateOut(transition: ScreenTransition, done: (() -> Unit)?) {
    // No animation support yet - just call complete immediately
    done?.invoke()
}
