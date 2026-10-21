package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition
import kotlin.coroutines.CoroutineContext

public var animationsEnabled: Boolean = true
public actual val Element.areAnimationsEnabled: Boolean get() = animationsEnabled

public actual inline fun Element.withoutAnimation(action: () -> Unit) {
    if (!animationsEnabled) return action()
    animationsEnabled = false
    try {
        action()
    } finally {
        animationsEnabled = true
    }
}

/**
 * The root composition's frame clock and dispatcher.  Animations started outside composition run on it, as
 * Android's run on its Choreographer, so they follow the display (and a test's clock).  Null until the root
 * is composed, when there is nothing on screen to animate.
 */
internal var animationContext: CoroutineContext? = null

// Like the web, these play even inside withoutAnimation, which only makes property changes instant.
public actual fun Element.animateIn(transition: ScreenTransition, done: (() -> Unit)?) {
    underlyingNativeElement.animateScreenTransition(transition, entering = true, collapses = false, done)
}

public actual fun Element.animateOut(transition: ScreenTransition, done: (() -> Unit)?) {
    underlyingNativeElement.animateScreenTransition(transition, entering = false, collapses = false, done)
}
