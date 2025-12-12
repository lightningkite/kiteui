package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Assertion helpers for KiteUI testing.
 *
 * These provide readable, fluent assertion syntax for common test scenarios.
 */

/**
 * Asserts that a view exists (is not null).
 *
 * Example:
 * ```
 * val button = root.findByDebugName("submit")
 * button.assertExists("Submit button should exist")
 * ```
 */
fun RView?.assertExists(message: String? = null): RView {
    assertNotNull(this, message ?: "View should exist")
    return this
}

/**
 * Asserts that a view does not exist (is null).
 *
 * Example:
 * ```
 * val errorMsg = root.findByDebugName("error")
 * errorMsg.assertNotExists("Error message should not be shown")
 * ```
 */
fun RView?.assertNotExists(message: String? = null) {
    assertNull(this, message ?: "View should not exist")
}

/**
 * Asserts that a view is visible.
 *
 * Example:
 * ```
 * val modal = root.findByDebugName("modal")!!
 * modal.assertVisible("Modal should be visible")
 * ```
 */
fun RView.assertVisible(message: String? = null): RView {
    assertTrue(this.isVisible, message ?: "View should be visible")
    return this
}

/**
 * Asserts that a view is not visible.
 *
 * Example:
 * ```
 * val spinner = root.findByDebugName("loading")!!
 * spinner.assertNotVisible("Loading spinner should be hidden")
 * ```
 */
fun RView.assertNotVisible(message: String? = null): RView {
    assertFalse(this.isVisible, message ?: "View should not be visible")
    return this
}

/**
 * Asserts that a view is enabled.
 *
 * Example:
 * ```
 * val submitBtn = root.findByDebugName("submit")!!
 * submitBtn.assertEnabled("Submit button should be enabled")
 * ```
 */
fun RView.assertEnabled(message: String? = null): RView {
    assertTrue(this.isEnabled, message ?: "View should be enabled")
    return this
}

/**
 * Asserts that a view is disabled.
 *
 * Example:
 * ```
 * val submitBtn = root.findByDebugName("submit")!!
 * submitBtn.assertDisabled("Submit button should be disabled until form is valid")
 * ```
 */
fun RView.assertDisabled(message: String? = null): RView {
    assertFalse(this.isEnabled, message ?: "View should be disabled")
    return this
}

/**
 * Asserts that a view has the expected text content.
 *
 * Example:
 * ```
 * val label = root.findByDebugName("status")!!
 * label.assertHasText("Success", "Status label should show success")
 * ```
 */
fun RView.assertHasText(expected: String, message: String? = null): RView {
    val actual = this.text
    assertEquals(expected, actual, message ?: "View should have text: $expected")
    return this
}

/**
 * Asserts that a view's text contains the expected substring.
 *
 * Example:
 * ```
 * val errorMsg = root.findByDebugName("error")!!
 * errorMsg.assertTextContains("invalid", "Error should mention invalid input")
 * ```
 */
fun RView.assertTextContains(substring: String, message: String? = null): RView {
    val actual = this.text
    assertNotNull(actual, "View should have text")
    assertTrue(
        actual.contains(substring, ignoreCase = false),
        message ?: "View text should contain: $substring, but was: $actual"
    )
    return this
}

/**
 * Asserts that a view has any text content (not null or empty).
 *
 * Example:
 * ```
 * val label = root.findByDebugName("username")!!
 * label.assertHasAnyText("Username should be displayed")
 * ```
 */
fun RView.assertHasAnyText(message: String? = null): RView {
    val actual = this.text
    assertTrue(
        !actual.isNullOrEmpty(),
        message ?: "View should have non-empty text"
    )
    return this
}

/**
 * Asserts that a view has no text content (null or empty).
 *
 * Example:
 * ```
 * val input = root.findByDebugName("search")!!
 * input.assertHasNoText("Search field should be empty initially")
 * ```
 */
fun RView.assertHasNoText(message: String? = null): RView {
    val actual = this.text
    assertTrue(
        actual.isNullOrEmpty(),
        message ?: "View should have no text, but had: $actual"
    )
    return this
}

/**
 * Asserts that a view is clickable.
 *
 * Example:
 * ```
 * val button = root.findByDebugName("action")!!
 * button.assertClickable("Button should be clickable")
 * ```
 */
fun RView.assertClickable(message: String? = null): RView {
    assertTrue(this.isClickable, message ?: "View should be clickable")
    return this
}

/**
 * Asserts that a view is not clickable.
 *
 * Example:
 * ```
 * val label = root.findByDebugName("info")!!
 * label.assertNotClickable("Label should not be clickable")
 * ```
 */
fun RView.assertNotClickable(message: String? = null): RView {
    assertFalse(this.isClickable, message ?: "View should not be clickable")
    return this
}

/**
 * Asserts that a view is focused.
 *
 * Example:
 * ```
 * val emailField = root.findByDebugName("email")!!
 * emailField.assertFocused("Email field should have focus")
 * ```
 */
fun RView.assertFocused(message: String? = null): RView {
    assertTrue(this.isFocused, message ?: "View should be focused")
    return this
}

/**
 * Asserts that a view is not focused.
 *
 * Example:
 * ```
 * val passwordField = root.findByDebugName("password")!!
 * passwordField.assertNotFocused("Password field should not have focus initially")
 * ```
 */
fun RView.assertNotFocused(message: String? = null): RView {
    assertFalse(this.isFocused, message ?: "View should not be focused")
    return this
}

/**
 * Asserts that a view is checked (for checkboxes, radio buttons, switches).
 *
 * Example:
 * ```
 * val checkbox = root.findByDebugName("terms")!!
 * checkbox.assertChecked("Terms checkbox should be checked")
 * ```
 */
fun RView.assertChecked(message: String? = null): RView {
    assertTrue(this.isChecked, message ?: "View should be checked")
    return this
}

/**
 * Asserts that a view is not checked (for checkboxes, radio buttons, switches).
 *
 * Example:
 * ```
 * val checkbox = root.findByDebugName("newsletter")!!
 * checkbox.assertNotChecked("Newsletter checkbox should not be checked by default")
 * ```
 */
fun RView.assertNotChecked(message: String? = null): RView {
    assertFalse(this.isChecked, message ?: "View should not be checked")
    return this
}

/**
 * Asserts that a view has the expected content description.
 *
 * Example:
 * ```
 * val icon = root.findByDebugName("search-icon")!!
 * icon.assertContentDescription("Search", "Icon should have accessibility label")
 * ```
 */
fun RView.assertContentDescription(expected: String, message: String? = null): RView {
    val actual = this.contentDescription
    assertEquals(expected, actual, message ?: "View should have content description: $expected")
    return this
}

/**
 * Asserts that a view has a specific width in pixels.
 *
 * Example:
 * ```
 * val card = root.findByDebugName("card")!!
 * card.assertWidth(300, "Card should be 300px wide")
 * ```
 */
fun RView.assertWidth(expected: Int, message: String? = null): RView {
    assertEquals(expected, this.width, message ?: "View should have width: $expected")
    return this
}

/**
 * Asserts that a view has a specific height in pixels.
 *
 * Example:
 * ```
 * val banner = root.findByDebugName("banner")!!
 * banner.assertHeight(100, "Banner should be 100px tall")
 * ```
 */
fun RView.assertHeight(expected: Int, message: String? = null): RView {
    assertEquals(expected, this.height, message ?: "View should have height: $expected")
    return this
}

/**
 * Asserts that a list/collection has the expected number of items.
 *
 * Example:
 * ```
 * val items = root.findAllByDebugName("list-item")
 * items.assertCount(5, "Should have 5 list items")
 * ```
 */
fun List<RView>.assertCount(expected: Int, message: String? = null) {
    assertEquals(expected, this.size, message ?: "Should have $expected items, but found ${this.size}")
}

/**
 * Asserts that a list/collection is not empty.
 *
 * Example:
 * ```
 * val results = root.findAllByDebugName("search-result")
 * results.assertNotEmpty("Should have search results")
 * ```
 */
fun List<RView>.assertNotEmpty(message: String? = null) {
    assertTrue(this.isNotEmpty(), message ?: "List should not be empty")
}

/**
 * Asserts that a list/collection is empty.
 *
 * Example:
 * ```
 * val errors = root.findAllByDebugName("error-message")
 * errors.assertEmpty("Should have no error messages")
 * ```
 */
fun List<RView>.assertEmpty(message: String? = null) {
    assertTrue(this.isEmpty(), message ?: "List should be empty")
}

/**
 * Finds a view by debug name and asserts it exists.
 * This is a convenience method that combines find + assert.
 *
 * Example:
 * ```
 * val button = root.requireByDebugName("submit")
 * button.click()
 * ```
 */
fun RView.requireByDebugName(debugName: String): RView {
    return this.findByDebugName(debugName).assertExists("View with debugName '$debugName' should exist")
}

/**
 * Finds a view by text and asserts it exists.
 * This is a convenience method that combines find + assert.
 *
 * Example:
 * ```
 * val loginBtn = root.requireByText("Log In")
 * loginBtn.click()
 * ```
 */
fun RView.requireByText(text: String, ignoreCase: Boolean = false): RView {
    return this.findByText(text, ignoreCase).assertExists("View with text '$text' should exist")
}

/**
 * Finds a view by text containing substring and asserts it exists.
 * This is a convenience method that combines find + assert.
 *
 * Example:
 * ```
 * val error = root.requireByTextContaining("failed")
 * error.assertVisible()
 * ```
 */
fun RView.requireByTextContaining(text: String, ignoreCase: Boolean = false): RView {
    return this.findByTextContaining(text, ignoreCase).assertExists("View containing text '$text' should exist")
}
