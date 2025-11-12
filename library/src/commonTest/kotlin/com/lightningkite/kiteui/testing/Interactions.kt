package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView

/**
 * Cross-platform interaction helpers for testing.
 *
 * Platform-specific implementations handle the actual interaction simulation.
 * For views with Actions (like buttons), implementations should trigger the action directly.
 */
expect object Interactions {
    /**
     * Simulates a click/tap on the given view.
     *
     * For views with Actions (like buttons), this triggers the action directly.
     * For other views, this simulates a platform-specific click event.
     *
     * @param view The view to click
     */
    fun click(view: RView)
}

/**
 * Extension function to click this view.
 *
 * Example:
 * ```
 * val button = root.findByDebugName("submit")!!
 * button.click()
 * ```
 */
fun RView.click() {
    Interactions.click(this)
}
