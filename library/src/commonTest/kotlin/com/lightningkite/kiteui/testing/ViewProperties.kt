package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView

/**
 * Helper object for accessing common view properties in tests.
 *
 * These helpers provide a cross-platform way to read view state for assertions.
 */
expect object ViewProperties {
    /**
     * Gets the text content of a text view.
     *
     * Works with TextView, Button, and other views that display text.
     * Returns null if the view doesn't have text content.
     */
    fun getText(view: RView): String?

    /**
     * Checks if a view is currently visible.
     *
     * Returns true if the view is visible, false otherwise.
     */
    fun isVisible(view: RView): Boolean

    /**
     * Checks if a view is currently enabled.
     *
     * Returns true if the view is enabled and can receive interactions,
     * false if it's disabled.
     */
    fun isEnabled(view: RView): Boolean

    /**
     * Gets the hint/placeholder text of an input field.
     *
     * Works with TextInput and similar views.
     * Returns null if the view doesn't have a hint.
     */
    fun getHint(view: RView): String?
}

/**
 * Extension function to get text content from this view.
 *
 * Example:
 * ```
 * val label = root.findByDebugName("title")!!
 * assertEquals("Hello", label.textContent)
 * ```
 */
val RView.textContent: String?
    get() = ViewProperties.getText(this)

/**
 * Extension function to check if this view is visible.
 *
 * Example:
 * ```
 * val errorMessage = root.findByDebugName("error")!!
 * assertFalse(errorMessage.isVisibleInTest)
 * ```
 */
val RView.isVisibleInTest: Boolean
    get() = ViewProperties.isVisible(this)

/**
 * Extension function to check if this view is enabled.
 *
 * Example:
 * ```
 * val submitButton = root.findByDebugName("submit")!!
 * assertTrue(submitButton.isEnabledInTest)
 * ```
 */
val RView.isEnabledInTest: Boolean
    get() = ViewProperties.isEnabled(this)

/**
 * Extension function to get hint text from this view.
 *
 * Example:
 * ```
 * val emailInput = root.findByDebugName("email")!!
 * assertEquals("Enter email", emailInput.hintText)
 * ```
 */
val RView.hintText: String?
    get() = ViewProperties.getHint(this)
