package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

public actual fun RView.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}
public actual fun RView.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}