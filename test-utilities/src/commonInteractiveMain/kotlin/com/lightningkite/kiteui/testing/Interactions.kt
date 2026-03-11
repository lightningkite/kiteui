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
     * Types text into a text input view.
     *
     * @param view The text input view
     * @param text The text to type
     */
    fun typeText(view: RView, text: String)

    /**
     * Clears the text in a text input view.
     *
     * @param view The text input view to clear
     */
    fun clearText(view: RView)

    /**
     * Replaces all text in a text input view with new text.
     * This is more efficient than clearText + typeText.
     *
     * @param view The text input view
     * @param text The new text
     */
    fun setText(view: RView, text: String)

    /**
     * Scrolls a scrollable view by a given amount.
     *
     * @param view The scrollable view
     * @param deltaX Horizontal scroll amount (positive = scroll right)
     * @param deltaY Vertical scroll amount (positive = scroll down)
     */
    fun scroll(view: RView, deltaX: Int = 0, deltaY: Int = 0)

    /**
     * Performs a long press/long click on the view.
     *
     * @param view The view to long press
     */
    fun longClick(view: RView)

    /**
     * Performs a swipe gesture on the view.
     *
     * @param view The view to swipe on
     * @param startX Starting X coordinate (0-1, relative to view width)
     * @param startY Starting Y coordinate (0-1, relative to view height)
     * @param endX Ending X coordinate (0-1, relative to view width)
     * @param endY Ending Y coordinate (0-1, relative to view height)
     */
    fun swipe(view: RView, startX: Float, startY: Float, endX: Float, endY: Float)
}

// Extension functions for easier usage

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
 * Extension function to type text into this view.
 *
 * Example:
 * ```
 * val emailField = root.findByDebugName("email")!!
 * emailField.typeText("test@example.com")
 * ```
 */
fun RView.typeText(text: String) {
    Interactions.typeText(this, text)
}

/**
 * Extension function to clear text from this view.
 *
 * Example:
 * ```
 * val nameField = root.findByDebugName("name")!!
 * nameField.clearText()
 * ```
 */
fun RView.clearText() {
    Interactions.clearText(this)
}

/**
 * Extension function to set text in this view (replacing existing text).
 *
 * Example:
 * ```
 * val emailField = root.findByDebugName("email")!!
 * emailField.setText("newemail@example.com")
 * ```
 */
fun RView.setText(text: String) {
    Interactions.setText(this, text)
}

/**
 * Extension function to scroll this view.
 *
 * Example:
 * ```
 * val scrollView = root.findByDebugName("content")!!
 * scrollView.scroll(deltaY = 100)  // Scroll down 100 pixels
 * ```
 */
fun RView.scroll(deltaX: Int = 0, deltaY: Int = 0) {
    Interactions.scroll(this, deltaX, deltaY)
}

/**
 * Extension function to perform a long click on this view.
 *
 * Example:
 * ```
 * val item = root.findByDebugName("list-item")!!
 * item.longClick()
 * ```
 */
fun RView.longClick() {
    Interactions.longClick(this)
}

/**
 * Extension function to perform a swipe gesture on this view.
 *
 * Example:
 * ```
 * val card = root.findByDebugName("swipeable-card")!!
 * card.swipe(startX = 0.8f, startY = 0.5f, endX = 0.2f, endY = 0.5f)  // Swipe left
 * ```
 */
fun RView.swipe(startX: Float, startY: Float, endX: Float, endY: Float) {
    Interactions.swipe(this, startX, startY, endX, endY)
}
