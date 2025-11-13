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

    /**
     * Scrolls a scrollable view by the specified amount.
     *
     * Works with ScrollView and similar scrollable containers.
     *
     * @param view The scrollable view
     * @param dx Horizontal scroll offset (positive = scroll right)
     * @param dy Vertical scroll offset (positive = scroll down)
     */
    fun scrollBy(view: RView, dx: Int = 0, dy: Int = 0)

    /**
     * Scrolls a view to make a child view visible.
     *
     * Useful for scrolling to specific items in lists or long content.
     *
     * @param scrollView The scrollable parent view
     * @param targetView The child view to scroll to
     */
    fun scrollToView(scrollView: RView, targetView: RView)
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

/**
 * Extension function to scroll this view by an offset.
 *
 * Example:
 * ```
 * val scrollView = root.findByDebugName("content")!!
 * scrollView.scrollBy(dy = 500)  // Scroll down 500 pixels
 * ```
 */
fun RView.scrollBy(dx: Int = 0, dy: Int = 0) {
    Interactions.scrollBy(this, dx, dy)
}

/**
 * Extension function to scroll this view to make a target view visible.
 *
 * Example:
 * ```
 * val scrollView = root.findByDebugName("list")!!
 * val item = root.findByDebugName("item-10")!!
 * scrollView.scrollToView(item)
 * ```
 */
fun RView.scrollToView(targetView: RView) {
    Interactions.scrollToView(this, targetView)
}
