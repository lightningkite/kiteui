package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView

/**
 * Android implementation of Interactions.
 */
actual object Interactions {
    actual fun click(view: RView) {
        // Call performClick on the native Android View
        view.native.performClick()
    }
}
