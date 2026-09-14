package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.direct.suppressMutationObserverForClass
import org.w3c.dom.HTMLElement

public actual inline fun Element.withoutAnimation(action: () -> Unit) {
    (native.element as? HTMLElement)?.withoutAnimation(action) ?: action()
}

public var animationsEnabled: Boolean = true
public actual val Element.areAnimationsEnabled: Boolean get() = animationsEnabled

public inline fun HTMLElement.withoutAnimation(action: () -> Unit) {
    val animate = animationsEnabled
    try {
        if (animate) {
            animationsEnabled = false
            suppressMutationObserverForClass {
                classList.add("notransition")
            }
        }
        action()
    } finally {
        if (animate) {
            kotlinx.browser.window.setTimeout({
                suppressMutationObserverForClass {
                    classList.remove("notransition")
                }
            }, 100)
            animationsEnabled = true
        }
    }
}