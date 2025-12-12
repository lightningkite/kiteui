package com.lightningkite.kiteui.views

/**
 * Test identifier for views - used for locating views in automated tests.
 *
 * This property uses the view's debugName field and does not affect production behavior.
 * Test IDs should be unique within a screen/page for reliable testing.
 *
 * Example usage in production code:
 * ```kotlin
 * "login-button".testId - button {
 *     text("Log In")
 *     onClick { /* ... */ }
 * }
 * ```
 *
 * Example usage in tests:
 * ```kotlin
 * scope.findByTestId("login-button").click()
 * ```
 */
var RView.testId: String?
    get() = debugName
    set(value) {
        debugName = value
    }

/**
 * Modifier to set test ID on the next view element.
 *
 * Example:
 * ```kotlin
 * "submit-button".testId - button {
 *     text("Submit")
 * }
 * ```
 */
@ViewModifierDsl3
operator fun String.minus(writer: ViewWriter): ViewWriter = writer.also {
    it.beforeNextElementSetup {
        testId = this@minus
    }
}
