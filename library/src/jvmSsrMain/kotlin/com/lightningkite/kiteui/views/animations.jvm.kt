package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

actual fun RView.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}
actual fun RView.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}