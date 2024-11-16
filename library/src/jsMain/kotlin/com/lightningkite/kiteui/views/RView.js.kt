package com.lightningkite.kiteui.views

import kotlinx.browser.window
import org.w3c.dom.HTMLElement

actual inline fun RView.withoutAnimation(action: () -> Unit) {
    (native.element as? HTMLElement)?.let { it.withoutAnimation(action) } ?: action()
}

var animationsEnabled: Boolean = true
inline fun HTMLElement.withoutAnimation(action: () -> Unit) {
    val animate = animationsEnabled
    try {
        if(animate) {
            animationsEnabled = false
            clientWidth
            classList.add("notransition")
            clientWidth
        }
        action()
    } finally {
        if(animate) {
            offsetHeight  // force layout calculation
            kotlinx.browser.window.setTimeout({
                classList.remove("notransition")
            }, 100)
            animationsEnabled = true
        }
    }
}