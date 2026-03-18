package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.DriverActionException
import kotlinx.coroutines.delay
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Structured result from a `find` or `findClickable` query.
 * Fields are parsed from the driver's text output format.
 */
data class FindResult(
    /** Driver path usable with click(), setValue(), etc. */
    val path: String,
    /** debugName or child index. */
    val name: String,
    /** View type name, e.g. "TextInput", "Button". */
    val type: String,
    /** driverValue if present, null otherwise. */
    val value: String?,
    /** Non-base actions available, e.g. {"click", "setValue"}. */
    val actions: Set<String>,
    /** Full unparsed line from the driver. */
    val rawLine: String,
)

private val valuePattern = Regex(""" = "(.*)"""")
private val actionsPattern = Regex("""\[(.+?)]$""")

/**
 * Parses a single line from `find` / `findClickable` output into a [FindResult].
 * Format: `path: name: Type = "value" [action1, action2]`
 */
fun parseFindLine(line: String): FindResult? {
    val colonIdx = line.indexOf(": ")
    if (colonIdx < 0) return null
    val path = line.substring(0, colonIdx)
    val display = line.substring(colonIdx + 2)

    val displayColonIdx = display.indexOf(": ")
    if (displayColonIdx < 0) return null
    val name = display.substring(0, displayColonIdx)
    val rest = display.substring(displayColonIdx + 2)

    val spaceIdx = rest.indexOf(' ')
    val type = if (spaceIdx >= 0) rest.substring(0, spaceIdx) else rest

    val value = valuePattern.find(rest)?.groupValues?.get(1)
    val actions = actionsPattern.find(rest)?.groupValues?.get(1)?.split(", ")?.toSet() ?: emptySet()

    return FindResult(path, name, type, value, actions, line)
}

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

    /** Get the current page URL. */
    suspend fun url(): String =
        backend.command(cmd("url"))

    /** Go back in navigation. */
    suspend fun back(): String =
        backend.command(cmd("back", "back"))

    /** Search the view tree for views matching a query. */
    suspend fun find(query: String, target: String = "root"): String =
        backend.command(cmd(target, "find", query))

    /** Search the view tree and return structured [FindResult]s. */
    suspend fun findAll(query: String, target: String = "root"): List<FindResult> =
        find(query, target).lines().filter { it.isNotBlank() }.mapNotNull { parseFindLine(it) }

    /** Find the first view matching [query] that has the given [action]. */
    suspend fun findWithAction(query: String, action: String, target: String = "root"): FindResult? =
        findAll(query, target).firstOrNull { action in it.actions }

    /**
     * Find views matching [query] and walk each up to the nearest clickable ancestor.
     * Returns deduplicated clickable views. Useful when `find("Login")` matches a Text
     * inside a Button — this returns the Button's path directly.
     */
    suspend fun findClickable(query: String, target: String = "root"): List<FindResult> =
        backend.command(cmd(target, "findClickable", query)).lines().filter { it.isNotBlank() }.mapNotNull { parseFindLine(it) }

    /** Get recent log entries. */
    suspend fun logs(count: Int = 50): String =
        backend.command(cmd("logs", count.toString()))

    /** Get alignment of a view. Returns "horizontal=X vertical=Y". */
    suspend fun getAlignment(target: String): String =
        backend.command(cmd(target, "getAlignment"))

    /** Send a raw command string. */
    suspend fun raw(command: String): String =
        backend.command(command)

    // --- Mock external services ---

    /**
     * Queue a mock file for the next `requestFile()` / `requestFiles()` / `requestCapture*()` call.
     * The file is created from [bytes] with the given [mimeType] and [fileName].
     *
     * Lazily installs a [MockExternalServices] wrapper if one isn't already present.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun mockFile(bytes: ByteArray, mimeType: String = "application/octet-stream", fileName: String = "mock-file"): String =
        backend.command(cmd("mock", "file", Base64.encode(bytes), mimeType, fileName))

    /**
     * Queue a null response for the next file picker call (simulates user cancellation).
     */
    suspend fun mockFileCancel(): String =
        backend.command(cmd("mock", "fileNull"))

    /**
     * Queue a mock geolocation response for the next `getCurrentPosition()` call.
     */
    suspend fun mockGeolocation(latitude: Double, longitude: Double, accuracyMeters: Double = 0.0): String =
        backend.command(cmd("mock", "geolocation", latitude.toString(), longitude.toString(), accuracyMeters.toString()))

    /** Clear the recorded call log on [MockExternalServices]. */
    suspend fun mockClearCalls(): String =
        backend.command(cmd("mock", "clearCalls"))

    /** Get the recorded call log from [MockExternalServices]. */
    suspend fun mockCalls(): String =
        backend.command(cmd("mock", "calls"))

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

    /** Wait until a view with the given [id] (debugName) exists and is reachable. */
    suspend fun waitForId(id: String, timeoutMs: Long = 5000) {
        waitFor(timeoutMs, description = "view '$id' to exist") {
            try { snapshot(id); true } catch (_: DriverActionException) { false }
        }
    }

    /** Assert that text appears somewhere in the snapshot of [target]. */
    suspend fun assertTextVisible(text: String, target: String = "root") {
        val snap = snapshot(target)
        if (!snap.contains(text)) {
            throw AssertionError("Expected text '$text' visible in '$target' but snapshot:\n$snap")
        }
    }

    /** Assert that a view with the given [id] (debugName) exists. */
    suspend fun assertIdExists(id: String) {
        try {
            snapshot(id)
        } catch (e: DriverActionException) {
            throw AssertionError("Expected view '$id' to exist but it was not found", e)
        }
    }
}
