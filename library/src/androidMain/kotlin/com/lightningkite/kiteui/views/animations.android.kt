package com.lightningkite.kiteui.views

import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.ScreenTransition

@InternalKiteUi
public actual fun RView.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}
@InternalKiteUi
public actual fun RView.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    done?.invoke()
}