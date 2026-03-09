// by Claude - shared walkthrough steps for cross-platform integration tests
package com.lightningkite.mppexampleapp.integration

import com.lightningkite.kiteui.testing.UiTestScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Exercises the main interactive features of the KiteUI example app.
 *
 * Covers:
 * - HomePage: counter increment/decrement
 * - ClickTestPage: button grid, click tracking, clear log
 * - ReactivityPage: reactive text bindings
 * - SampleLogInPage: email/password form input
 * - Navigation between all tested pages
 *
 * Works with both local and remote backends. For remote tests (through ai-driver-server),
 * pass a longer [initialTimeout] to account for app startup time.
 *
 * Prerequisite: the app must be on the home page (navigate("/") before calling).
 */
// by Claude
suspend fun UiTestScope.exampleAppWalkthrough(initialTimeout: Duration = 30.seconds) {
    // ---- Home Page: counter ----
    // Navigate away and back to ensure a fresh page instance (counter resets to 0).
    waitForPage("HomePage", timeout = initialTimeout)
    navigate("/click-test")
    waitForPage("ClickTestPage")
    navigate("/")
    waitForPage("HomePage")
    assertValue("counter", "0")
    click("increment")
    assertValue("counter", "1")
    click("increment")
    assertValue("counter", "2")
    click("decrement")
    assertValue("counter", "1")

    // ---- Click Test Page: button grid + tracking ----
    navigate("/click-test")
    waitForPage("ClickTestPage")
    click("clearLog") // reset singleton state from any previous usage
    assertValue("lastClicked", "(none)")
    click("button1")
    assertValue("lastClicked", "Button 1")
    click("button5")
    assertValue("lastClicked", "Button 5")
    click("button9")
    assertValue("lastClicked", "Button 9")
    click("clearLog")
    assertValue("lastClicked", "(none)")

    // ---- Reactivity Page: reactive bindings ----
    navigate("/reactivity")
    waitForPage("ReactivityPage")
    assertValue("localInput", "Local")
    assertValue("localDisplay", "local = Local")
    setValue("localInput", "Integration Test")
    assertValue("localDisplay", "local = Integration Test")

    // ---- Sample Login Page: form input ----
    navigate("/sample/login")
    waitForPage("SampleLogInPage")
    assertVisible("emailInput")
    assertVisible("passwordInput")
    assertVisible("loginButton")
    setValue("emailInput", "test@example.com")
    setValue("passwordInput", "secret123")
    assertValue("emailInput", "test@example.com")
    assertValue("passwordInput", "secret123")

    // ---- Navigate back to Home ----
    navigate("/")
    waitForPage("HomePage")
    // Web navigator caches page instances per URL (PageNavigatorBehavior.Separate),
    // so counter may retain its previous value. JVM SSR creates fresh instances.
    // Use relative assertion: read current value, click increment, verify it changed.
    val counterBefore = require("counter").value?.toIntOrNull() ?: error("counter has no numeric value")
    click("increment")
    assertValue("counter", (counterBefore + 1).toString())
}
