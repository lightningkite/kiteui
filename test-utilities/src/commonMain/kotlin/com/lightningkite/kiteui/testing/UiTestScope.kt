package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.DriverActionException
import kotlinx.coroutines.delay

/**
 * Test DSL built on text-based driver commands.
 * Provides typed wrappers for common operations like clicking, setting values, and asserting.
 *
 * Commands use tab separation so values containing spaces are preserved exactly.
 */
class UiTestScope(val backend: UiTestBackend) {

    private fun cmd(vararg parts: String): String = parts.joinToString("\t")

    /** Get a text snapshot of the view tree. */
    suspend fun snapshot(target: String = "root"): String =
        backend.command(cmd(target, "snapshot"))

    /** Get a snapshot showing only interactive elements. */
    suspend fun interactiveSnapshot(target: String = "root"): String =
        backend.command(cmd(target, "snapshot", "--interactive"))

    /** Get a base64 PNG screenshot. */
    suspend fun screenshot(target: String = "root"): String =
        backend.command(cmd(target, "screenshot"))

    /** Click a button or interactive view. */
    suspend fun click(target: String): String =
        backend.command(cmd(target, "click"))

    /** Long-click a view. */
    suspend fun longClick(target: String): String =
        backend.command(cmd(target, "longClick"))

    /** Set the value of a text input, slider, checkbox, etc. */
    suspend fun setValue(target: String, value: String): String =
        backend.command(cmd(target, "setValue", value))

    /** Toggle a checkbox, switch, or toggle button. */
    suspend fun toggle(target: String): String =
        backend.command(cmd(target, "toggle"))

    /** Select a radio button. */
    suspend fun select(target: String): String =
        backend.command(cmd(target, "select"))

    /** Submit a text input's action. */
    suspend fun submit(target: String): String =
        backend.command(cmd(target, "submit"))

    /** Scroll a view by dx/dy pixels. */
    suspend fun scroll(target: String, dx: Double = 0.0, dy: Double = 0.0): String =
        backend.command(cmd(target, "scroll", dx.toString(), dy.toString()))

    /** Scroll a view into the visible area. */
    suspend fun scrollIntoView(target: String): String =
        backend.command(cmd(target, "scrollIntoView"))

    /** Get serialized drag data from a view (base64). */
    suspend fun getDragData(target: String): String =
        backend.command(cmd(target, "getDragData"))

    /** Drop serialized drag data onto a view. */
    suspend fun drop(target: String, dragDataBase64: String): String =
        backend.command(cmd(target, "drop", dragDataBase64))

    /** Navigate to a route. */
    suspend fun navigate(route: String): String =
        backend.command(cmd("navigate", route))

    /** Go back in navigation. */
    suspend fun back(): String =
        backend.command(cmd("back", "back"))

    /** Search the view tree for views matching a query. */
    suspend fun find(query: String, target: String = "root"): String =
        backend.command(cmd(target, "find", query))

    /** Get recent log entries. */
    suspend fun logs(count: Int = 50): String =
        backend.command(cmd("logs", count.toString()))

    /** Get alignment of a view. Returns "horizontal=X vertical=Y". */
    suspend fun getAlignment(target: String): String =
        backend.command(cmd(target, "getAlignment"))

    /** Send a raw command string. */
    suspend fun raw(command: String): String =
        backend.command(command)

    // --- Assertions ---

    /** Assert a command result equals expected value. */
    suspend fun assertResult(command: String, expected: String) {
        val result = backend.command(command)
        if (result != expected) {
            throw AssertionError("Expected '$expected' but got '$result' for command: $command")
        }
    }

    /** Assert that a view's driver value matches. */
    suspend fun assertValue(target: String, expected: String) {
        val snapshot = snapshot(target)
        if (!snapshot.contains("= \"$expected\"")) {
            throw AssertionError("Expected value '$expected' for '$target' but snapshot:\n$snapshot")
        }
    }

    /** Assert that a view exists and is visible in the snapshot. */
    suspend fun assertVisible(target: String) {
        val result = try {
            snapshot(target)
        } catch (e: DriverActionException) {
            throw AssertionError("View '$target' not found: ${e.message}")
        }
        if (result.isBlank() || result.contains("(hidden)") || result.contains("(invisible)")) {
            throw AssertionError("View '$target' is not visible")
        }
    }

    /** Assert that a view is hidden or not present. */
    suspend fun assertNotVisible(target: String) {
        try {
            val result = snapshot(target)
            // Empty snapshot means the view was hidden and pruned from output
            if (result.isBlank()) return
            if (!result.contains("(hidden)") && !result.contains("(invisible)")) {
                throw AssertionError("View '$target' is visible but expected hidden")
            }
        } catch (_: DriverActionException) {
            return  // View not found = not visible, which is expected
        }
    }

    /** Wait for a condition to become true. */
    suspend fun waitFor(
        timeoutMs: Long = 5000,
        intervalMs: Long = 100,
        description: String = "condition",
        condition: suspend () -> Boolean,
    ) {
        val deadline = com.lightningkite.kiteui.clockMillis() + timeoutMs
        while (!condition()) {
            if (com.lightningkite.kiteui.clockMillis() > deadline) {
                throw AssertionError("Timed out waiting for: $description")
            }
            delay(intervalMs)
        }
    }

    /** Wait until a specific text appears in the snapshot. */
    suspend fun waitForText(text: String, target: String = "root", timeoutMs: Long = 5000) {
        waitFor(timeoutMs, description = "text '$text' in $target") {
            snapshot(target).contains(text)
        }
    }
}
