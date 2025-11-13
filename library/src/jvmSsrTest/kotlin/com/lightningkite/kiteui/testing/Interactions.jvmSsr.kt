package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView

/**
 * JVM SSR implementation of Interactions.
 * Not supported - JVM SSR doesn't have interactive UI.
 */
actual object Interactions {
    actual fun click(view: RView) {
        throw UnsupportedOperationException("Interactions are not supported on JVM SSR")
    }

    actual fun typeText(view: RView, text: String, append: Boolean) {
        throw UnsupportedOperationException("Interactions are not supported on JVM SSR")
    }
}
