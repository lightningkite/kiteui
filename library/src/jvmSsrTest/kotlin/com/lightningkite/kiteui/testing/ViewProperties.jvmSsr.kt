package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView

/**
 * JVM SSR implementation of ViewProperties.
 * Not supported - JVM SSR doesn't have interactive UI.
 */
actual object ViewProperties {
    actual fun getText(view: RView): String? {
        throw UnsupportedOperationException("ViewProperties are not supported on JVM SSR")
    }

    actual fun isVisible(view: RView): Boolean {
        throw UnsupportedOperationException("ViewProperties are not supported on JVM SSR")
    }

    actual fun isEnabled(view: RView): Boolean {
        throw UnsupportedOperationException("ViewProperties are not supported on JVM SSR")
    }

    actual fun getHint(view: RView): String? {
        throw UnsupportedOperationException("ViewProperties are not supported on JVM SSR")
    }
}
