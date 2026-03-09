// by Claude - unified UI test scope using AI driver primitives
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.aidriver.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A test scope that uses the same primitives as the AI driver CLI.
 *
 * Mental model equivalence:
 * ```
 * CLI:  ./ui snapshot web-1          Kotlin: snapshot()
 * CLI:  ./ui perform web-1 click btn Kotlin: click("btn")
 * CLI:  ./ui wait web-1 --page Home  Kotlin: waitForPage("Home")
 * ```
 *
 * Constructed by [uiTest] (local) or [remoteUiTest] (remote) — not meant to be created directly.
 */
// by Claude - refactored to use UiTestBackend for pluggable local/remote execution
class UiTestScope(
    val backend: UiTestBackend,
) {
    /** Backward-compatible constructor for local testing. */
    constructor(
        root: RView,
        navigator: PageNavigator?,
        idle: suspend () -> Unit = {},
    ) : this(LocalUiTestBackend(root, navigator, idle))

    // by Claude - convenience accessor for the MockExternalServices if one was injected via UiTestConfig
    val mockExternalServices: MockExternalServices?
        get() = (backend as? LocalUiTestBackend)?.root?.context?.addons?.get("externalServices") as? MockExternalServices

    // ---- Snapshot (read state) ----

    /** Full snapshot of the current UI tree. Same as CLI `./ui snapshot`. */
    suspend fun snapshot(): UiSnapshot = backend.snapshot()

    /** Find a component by its path ID in the current snapshot. */
    suspend fun find(id: String): UiComponent? = snapshot().findById(id)

    /** Assert a component exists and return it. Throws if not found. */
    // by Claude - cache snapshot to avoid double-fetch and show consistent state in error
    suspend fun require(id: String): UiComponent {
        val snap = snapshot()
        return snap.findById(id) ?: throw AssertionError(
            "Component '$id' not found.\nCurrent snapshot:\n${snap.renderText()}"
        )
    }

    // ---- Perform (mutate state) ----

    /** Dispatch a UiAction against the view tree. Same as CLI `./ui perform`. */
    suspend fun perform(action: UiAction): ActionDispatchResult {
        val result = backend.perform(action)
        if (!result.success) {
            throw AssertionError("Action failed: $action: ${result.error}")
        }
        return result
    }

    /** Click a component by path ID. Same as `./ui perform <app> click <id>`. */
    suspend fun click(targetId: String) = perform(UiAction.Click(targetId))

    /** Set a value on a component. Same as `./ui perform <app> setValue <id> <value>`. */
    suspend fun setValue(targetId: String, value: String) =
        perform(UiAction.SetValue(targetId, value))

    /** Navigate to a URL-like route. Same as `./ui perform <app> navigate <route>`. */
    suspend fun navigate(route: String) = perform(UiAction.Navigate(route))

    /** Go back in the navigation stack. Same as `./ui perform <app> back`. */
    suspend fun back() = perform(UiAction.Back)

    /** Read the last [lines] log entries captured by the AI driver log buffer. */
    // by Claude - convenience for reading logs in tests
    suspend fun logs(lines: Int = 200): List<LogEntry> = backend.logs(lines)

    // ---- Wait (poll for state) ----

    /**
     * Wait until a condition on the snapshot is true.
     * Throws [AssertionError] on timeout.
     */
    suspend fun waitFor(
        timeout: Duration = 5.seconds,
        description: String = "condition",
        condition: (UiSnapshot) -> Boolean
    ) {
        try {
            withTimeout(timeout) {
                while (true) {
                    val snap = backend.snapshot()
                    if (condition(snap)) return@withTimeout
                    // by Claude - delay to avoid tight spin on Dispatchers.Unconfined
                    delay(50)
                }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            val snap = backend.snapshot()
            throw AssertionError(
                "Timed out waiting for $description after $timeout.\n" +
                "Final snapshot:\n${snap.renderText()}"
            )
        }
    }

    /** Wait for the current page to match. Same as CLI `./ui wait --page`. */
    suspend fun waitForPage(pageName: String, timeout: Duration = 5.seconds) =
        waitFor(timeout, description = "page=$pageName") { it.page == pageName }

    /** Wait for a component to appear. Same as CLI `./ui wait --component`. */
    suspend fun waitForComponent(id: String, timeout: Duration = 5.seconds) =
        waitFor(timeout, description = "component=$id") { it.findById(id) != null }

    /** Wait for a component to disappear. Same as CLI `./ui wait --componentGone`. */
    suspend fun waitForComponentGone(id: String, timeout: Duration = 5.seconds) =
        waitFor(timeout, description = "componentGone=$id") { it.findById(id) == null }

    /** Wait for a component to become enabled. Same as CLI `./ui wait --enabled`. */
    suspend fun waitForEnabled(id: String, timeout: Duration = 5.seconds) =
        waitFor(timeout, description = "enabled=$id") {
            it.findById(id)?.enabled == true
        }

    // ---- Assertions (convenience) ----

    /** Assert the current page name matches. */
    suspend fun assertPage(expected: String) {
        val snap = snapshot()
        if (snap.page != expected) {
            throw AssertionError("Expected page '$expected' but was '${snap.page}'")
        }
    }

    /** Assert a component has the expected value. */
    suspend fun assertValue(id: String, expected: String) {
        val component = require(id)
        if (component.value != expected) {
            throw AssertionError(
                "Expected '$id' to have value '$expected' but was '${component.value}'"
            )
        }
    }

    /** Assert a component is visible. */
    suspend fun assertVisible(id: String) {
        val component = require(id)
        if (!component.visible) throw AssertionError("Expected '$id' to be visible")
    }

    /** Assert a component is enabled. */
    suspend fun assertEnabled(id: String) {
        val component = require(id)
        if (!component.enabled) throw AssertionError("Expected '$id' to be enabled")
    }

    /** Assert a component is disabled. */
    suspend fun assertDisabled(id: String) {
        val component = require(id)
        if (component.enabled) throw AssertionError("Expected '$id' to be disabled")
    }

    /** Print the current snapshot as text (for debugging). */
    suspend fun dumpSnapshot() {
        println(snapshot().renderText())
    }

    // ---- Screenshots ----

    /**
     * Capture a screenshot as PNG bytes from the app.
     * Returns null if the backend doesn't support screenshots (e.g. local SSR tests).
     * Supported by [RemoteUiTestBackend] for capturing from live Android/iOS/web apps.
     */
    // by Claude - screenshot support for remote testing (app store screenshots, etc.)
    suspend fun screenshot(): ByteArray? = backend.screenshot()

    // ---- Mock external services ----

    // by Claude - queue a mock file to be returned by the next requestFile() call
    /**
     * Queue a mock file to be returned by the next `externalServices.requestFile()` call in the app.
     * Works for both local and remote tests.
     *
     * Example:
     * ```kotlin
     * mockFile("hello".encodeToByteArray(), "text/plain", "test.txt")
     * click("uploadButton")  // triggers requestFile() which returns the mock
     * ```
     */
    suspend fun mockFile(bytes: ByteArray, mimeType: String, fileName: String) =
        backend.mockFile(bytes, mimeType, fileName)

    // by Claude - queue a mock geolocation to be returned by the next getCurrentPosition() call
    /**
     * Queue a mock geolocation result to be returned by the next `externalServices.getCurrentPosition()` call.
     * Works for both local and remote tests.
     */
    suspend fun mockGeolocation(latitude: Double, longitude: Double, accuracyInMeters: Double = 10.0) =
        backend.mockGeolocation(latitude, longitude, accuracyInMeters)
}
