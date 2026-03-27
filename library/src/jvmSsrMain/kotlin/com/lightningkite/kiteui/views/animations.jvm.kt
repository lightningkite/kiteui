package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

actual fun Element.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}
actual fun Element.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}