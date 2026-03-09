// by Claude - cross-platform UI test entry point using AI driver primitives
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.ExternalServicesAccess
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.ViewWriter

/**
 * Configuration for a UI test.
 *
 * @param theme The theme to render with. Null (default) uses `Theme(id = "test")`, resolved
 *   inside each platform's [uiTest] actual so that platform context (e.g. Robolectric on Android)
 *   is set up before [Theme] is first referenced.
 * @param navigator Optional page navigator for navigation testing
 * @param externalServices Optional mock for external services (file pickers, geolocation, etc.)
 */
// by Claude
data class UiTestConfig(
    val theme: Theme? = null,
    val navigator: PageNavigator? = null,
    val externalServices: ExternalServicesAccess? = null,
)

/**
 * Main entry point for cross-platform UI tests.
 *
 * Uses the same primitives as the AI driver CLI (`./ui`):
 * - `snapshot()` = `./ui snapshot`
 * - `click("id")` = `./ui perform <app> click <id>`
 * - `setValue("id", "val")` = `./ui perform <app> setValue <id> <val>`
 * - `waitForPage("Page")` = `./ui wait <app> --page Page`
 *
 * Example:
 * ```kotlin
 * @Test
 * fun testLogin() = uiTest(content = {
 *     "email".testId - textInput { content bind email }
 *     "submit".testId - button { text("Submit") }
 * }) {
 *     setValue("email", "test@example.com")
 *     click("submit")
 * }
 * ```
 *
 * Runs on all platforms: Android (Robolectric), iOS (native), JS (Karma), JVM SSR.
 */
expect fun uiTest(
    config: UiTestConfig = UiTestConfig(),
    content: ViewWriter.() -> Unit = {},
    block: suspend UiTestScope.() -> Unit
)
