package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

// Animations are out of scope for this backend for now: everything happens instantly.

public actual val Element.areAnimationsEnabled: Boolean get() = false
public actual inline fun Element.withoutAnimation(action: () -> Unit) { action() }

public actual fun Element.animateIn(transition: ScreenTransition, done: (() -> Unit)?) {
    done?.invoke()
}

public actual fun Element.animateOut(transition: ScreenTransition, done: (() -> Unit)?) {
    done?.invoke()
}
