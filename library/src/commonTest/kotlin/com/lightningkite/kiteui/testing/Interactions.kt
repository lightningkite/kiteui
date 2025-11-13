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

    /**
     * Types text into an input field.
     *
     * Works with TextInput, NumberInput, and other input fields that have a 'content' property.
     * This updates the content reactively, triggering any bindings and change listeners.
     *
     * @param view The input view to type into
     * @param text The text to type
     * @param append If true, appends to existing text; if false (default), replaces it
     */
    fun typeText(view: RView, text: String, append: Boolean = false)
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

/**
 * Extension function to type text into this input view.
 *
 * Example:
 * ```
 * val emailInput = root.findByDebugName("email")!!
 * emailInput.typeText("user@example.com")
 * ```
 *
 * @param text The text to type
 * @param append If true, appends to existing text; if false (default), replaces it
 */
fun RView.typeText(text: String, append: Boolean = false) {
    Interactions.typeText(this, text, append)
}
