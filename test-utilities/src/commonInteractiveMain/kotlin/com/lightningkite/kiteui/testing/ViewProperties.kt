package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView

/**
 * Cross-platform property accessors for testing.
 *
 * Platform-specific implementations extract view properties for assertions.
 */
expect object ViewProperties {
    /**
     * Gets the text content from a view.
     * Works with text views, text inputs, buttons, etc.
     *
     * @param view The view to get text from
     * @return The text content, or null if the view doesn't have text
     */
    fun getText(view: RView): String?

    /**
     * Checks if a view is currently visible to the user.
     *
     * @param view The view to check
     * @return true if visible, false otherwise
     */
    fun isVisible(view: RView): Boolean

    /**
     * Checks if a view is currently enabled (can be interacted with).
     *
     * @param view The view to check
     * @return true if enabled, false otherwise
     */
    fun isEnabled(view: RView): Boolean

    /**
     * Checks if a view is currently focused.
     *
     * @param view The view to check
     * @return true if focused, false otherwise
     */
    fun isFocused(view: RView): Boolean

    /**
     * Checks if a view is currently clickable.
     *
     * @param view The view to check
     * @return true if clickable, false otherwise
     */
    fun isClickable(view: RView): Boolean

    /**
     * Gets the content description of a view (for accessibility).
     *
     * @param view The view to get content description from
     * @return The content description, or null if not set
     */
    fun getContentDescription(view: RView): String?

    /**
     * Gets the width of a view in pixels.
     *
     * @param view The view to measure
     * @return Width in pixels
     */
    fun getWidth(view: RView): Int

    /**
     * Gets the height of a view in pixels.
     *
     * @param view The view to measure
     * @return Height in pixels
     */
    fun getHeight(view: RView): Int

    /**
     * Checks if a view is currently checked (for checkboxes, radio buttons, etc.).
     *
     * @param view The view to check
     * @return true if checked, false otherwise or if not applicable
     */
    fun isChecked(view: RView): Boolean
}

// Extension functions for easier usage

/**
 * Gets the text content from this view.
 *
 * Example:
 * ```
 * val label = root.findByDebugName("status-label")!!
 * assertEquals("Success", label.text)
 * ```
 */
val RView.text: String?
    get() = ViewProperties.getText(this)

/**
 * Checks if this view is currently visible.
 *
 * Example:
 * ```
 * val errorMessage = root.findByDebugName("error")!!
 * assertTrue(errorMessage.isVisible)
 * ```
 */
val RView.isVisible: Boolean
    get() = ViewProperties.isVisible(this)

/**
 * Checks if this view is currently enabled.
 *
 * Example:
 * ```
 * val submitButton = root.findByDebugName("submit")!!
 * assertTrue(submitButton.isEnabled)
 * ```
 */
val RView.isEnabled: Boolean
    get() = ViewProperties.isEnabled(this)

/**
 * Checks if this view is currently focused.
 *
 * Example:
 * ```
 * val emailField = root.findByDebugName("email")!!
 * assertTrue(emailField.isFocused)
 * ```
 */
val RView.isFocused: Boolean
    get() = ViewProperties.isFocused(this)

/**
 * Checks if this view is currently clickable.
 *
 * Example:
 * ```
 * val button = root.findByDebugName("action")!!
 * assertTrue(button.isClickable)
 * ```
 */
val RView.isClickable: Boolean
    get() = ViewProperties.isClickable(this)

/**
 * Gets the content description of this view.
 *
 * Example:
 * ```
 * val icon = root.findByDebugName("search-icon")!!
 * assertEquals("Search", icon.contentDescription)
 * ```
 */
val RView.contentDescription: String?
    get() = ViewProperties.getContentDescription(this)

/**
 * Gets the width of this view in pixels.
 *
 * Example:
 * ```
 * val card = root.findByDebugName("card")!!
 * assertTrue(card.width > 0)
 * ```
 */
val RView.width: Int
    get() = ViewProperties.getWidth(this)

/**
 * Gets the height of this view in pixels.
 *
 * Example:
 * ```
 * val card = root.findByDebugName("card")!!
 * assertTrue(card.height > 0)
 * ```
 */
val RView.height: Int
    get() = ViewProperties.getHeight(this)

/**
 * Checks if this view is currently checked.
 *
 * Example:
 * ```
 * val checkbox = root.findByDebugName("terms-checkbox")!!
 * assertTrue(checkbox.isChecked)
 * ```
 */
val RView.isChecked: Boolean
    get() = ViewProperties.isChecked(this)
