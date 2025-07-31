package com.lightningkite.kiteui.views

import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
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