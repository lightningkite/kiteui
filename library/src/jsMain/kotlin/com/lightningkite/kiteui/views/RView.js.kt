package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.direct.suppressMutationObserverForClass
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

public actual inline fun RView.withoutAnimation(action: () -> Unit) {
    (native.element as? HTMLElement)?.let { it.withoutAnimation(action) } ?: action()
}

var animationsEnabled: Boolean = true
public actual val RView.areAnimationsEnabled: Boolean get() = com.lightningkite.kiteui.views.animationsEnabled
inline fun HTMLElement.withoutAnimation(action: () -> Unit) {
    val animate = animationsEnabled
    try {
        if(animate) {
            animationsEnabled = false
            suppressMutationObserverForClass {
                classList.add("notransition")
            }
        }
        action()
    } finally {
        if(animate) {
            kotlinx.browser.window.setTimeout({
                suppressMutationObserverForClass {
                    classList.remove("notransition")
                }
            }, 100)
            animationsEnabled = true
        }
    }
}