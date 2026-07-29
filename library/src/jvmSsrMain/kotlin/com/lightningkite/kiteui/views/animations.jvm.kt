package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

public actual fun Element.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}
public actual fun Element.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}