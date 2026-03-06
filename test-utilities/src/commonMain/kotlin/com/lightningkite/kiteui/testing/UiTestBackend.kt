// by Claude - pluggable backend for UiTestScope, enabling local and remote test execution
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.GeolocationResult
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

    // by Claude - mock file and geolocation support for testing file pickers and location
    /**
     * Queue a mock file to be returned by the next file-related call (requestFile, requestCaptureSelf, etc.).
     * @param bytes raw file content
     * @param mimeType MIME type (e.g. "image/png")
     * @param fileName suggested file name (e.g. "photo.png")
     */
    suspend fun mockFile(bytes: ByteArray, mimeType: String, fileName: String)

    // by Claude - explicit capture mock for precision testing (targets pendingCaptureResponses only)
    /**
     * Queue a mock file to be returned specifically by the next capture call
     * (requestCaptureSelf or requestCaptureEnvironment).
     * Use [mockFile] if you don't care whether it's consumed by a file picker or camera capture.
     */
    suspend fun mockCapture(bytes: ByteArray, mimeType: String, fileName: String) =
        mockFile(bytes, mimeType, fileName)

    /**
     * Queue a mock geolocation result to be returned by the next [ExternalServicesAccess.getCurrentPosition] call.
     */
    suspend fun mockGeolocation(latitude: Double, longitude: Double, accuracyInMeters: Double = 10.0)
}
