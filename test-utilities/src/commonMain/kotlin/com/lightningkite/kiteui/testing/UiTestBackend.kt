// by Claude - pluggable backend for UiTestScope, enabling local and remote test execution
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.aidriver.ActionDispatchResult
import com.lightningkite.kiteui.aidriver.LogEntry
import com.lightningkite.kiteui.aidriver.UiAction
import com.lightningkite.kiteui.aidriver.UiSnapshot

/**
 * Backend abstraction for [UiTestScope].
 *
 * Local mode ([LocalUiTestBackend]) renders UI in-process and tests against it.
 * Remote mode ([RemoteUiTestBackend]) sends commands to a live app via the ai-driver daemon.
 */
interface UiTestBackend {
    /** Capture the current UI tree. */
    suspend fun snapshot(): UiSnapshot

    /** Dispatch a UI action (click, setValue, navigate, etc.). */
    suspend fun perform(action: UiAction): ActionDispatchResult

    /** Read the last [lines] log entries from the app. */
    suspend fun logs(lines: Int): List<LogEntry>

    /**
     * Capture a screenshot as PNG bytes from the app.
     * Returns null if screenshots are not supported by this backend.
     */
    // by Claude - screenshot support for remote testing (app store screenshots, etc.)
    suspend fun screenshot(): ByteArray? = null
}
