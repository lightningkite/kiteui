// by Claude - mockable wrapper for ExternalServicesAccess
package com.lightningkite.kiteui

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

/**
 * Delegating wrapper that queues mock responses and records calls.
 * When the queue is empty for a given method, falls through to the delegate.
 *
 * Used by:
 * - `uiTest {}` to pre-configure responses for file pickers, geolocation, etc.
 * - AI driver CLI (`./ui mock`) to inject mock responses over the wire.
 *
 * Example:
 * ```kotlin
 * val mock = MockExternalServices()
 * mock.pendingFileResponses.add(someFileReference)
 * uiTest(config = UiTestConfig(externalServices = mock), content = { ... }) {
 *     click("uploadButton")
 *     assert(mock.calls.any { it is MockExternalServices.Call.RequestFile })
 * }
 * ```
 */
public class MockExternalServices(
    private val delegate: ExternalServicesAccess? = null
) : ExternalServicesAccess {

    // --- Queued responses (FIFO) ---
    // by Claude - single queue for all file-related requests (picker, capture, multi-file)
    public val pendingFileResponses = ArrayDeque<FileReference?>()
    public val pendingGeolocation = ArrayDeque<GeolocationResult>()

    // --- Recorded calls ---
    public sealed class Call {
        public data class OpenLink(val url: String, val newTab: Boolean) : Call()
        public data class OpenMap(val latitude: Double, val longitude: Double, val label: String?, val zoom: Float?) : Call()
        public data class RequestFile(val mimeTypes: List<String>) : Call()
        public data class RequestFiles(val mimeTypes: List<String>) : Call()
        public data class RequestCaptureSelf(val mimeTypes: List<String>) : Call()
        public data class RequestCaptureEnvironment(val mimeTypes: List<String>) : Call()
        public data class SetClipboardText(val text: String) : Call()
        public data class ShareBlobs(val names: List<String>) : Call()
        public data class ShareText(val title: String, val message: String?, val url: String?) : Call()
        public data class OpenEvent(val title: String) : Call()
        public data class DownloadBlob(val name: String) : Call()
        public data class DownloadUrl(val name: String, val url: String) : Call()
        public data class GetCurrentPosition(val result: GeolocationResult?) : Call()
    }

    public val calls = mutableListOf<Call>()

    // --- ExternalLinksAccess ---

    override fun openLink(url: String, newTab: Boolean) {
        calls.add(Call.OpenLink(url, newTab))
        delegate?.openLink(url, newTab)
    }

    override fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
        calls.add(Call.OpenMap(latitude, longitude, label, zoom))
        delegate?.openMap(latitude, longitude, label, zoom)
    }

    // --- FilePickerAccess ---

    override suspend fun requestFile(mimeTypes: List<String>): FileReference? {
        calls.add(Call.RequestFile(mimeTypes))
        return if (pendingFileResponses.isNotEmpty()) pendingFileResponses.removeFirst()
        else delegate?.requestFile(mimeTypes)
    }

    override suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> {
        calls.add(Call.RequestFiles(mimeTypes))
        return if (pendingFileResponses.isNotEmpty()) listOfNotNull(pendingFileResponses.removeFirst())
        else delegate?.requestFiles(mimeTypes) ?: emptyList()
    }

    // by Claude - all file/capture requests share one queue
    override suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? {
        calls.add(Call.RequestCaptureSelf(mimeTypes))
        return if (pendingFileResponses.isNotEmpty()) pendingFileResponses.removeFirst()
        else delegate?.requestCaptureSelf(mimeTypes)
    }

    override suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? {
        calls.add(Call.RequestCaptureEnvironment(mimeTypes))
        return if (pendingFileResponses.isNotEmpty()) pendingFileResponses.removeFirst()
        else delegate?.requestCaptureEnvironment(mimeTypes)
    }

    // --- FileDownloadAccess ---

    override suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
        calls.add(Call.DownloadBlob(name))
        delegate?.download(name, blob, preferredDestination)
    }

    override suspend fun download(
        name: String,
        url: String,
        preferredDestination: DownloadLocation,
        onDownloadProgress: ((progress: Float) -> Unit)?
    ) {
        calls.add(Call.DownloadUrl(name, url))
        delegate?.download(name, url, preferredDestination, onDownloadProgress)
    }

    // --- SharingAccess ---

    override fun setClipboardText(value: String) {
        calls.add(Call.SetClipboardText(value))
        delegate?.setClipboardText(value)
    }

    override suspend fun share(namesToBlobs: List<Pair<String, Blob>>) {
        calls.add(Call.ShareBlobs(namesToBlobs.map { it.first }))
        delegate?.share(namesToBlobs)
    }

    override fun share(title: String, message: String?, url: String?) {
        calls.add(Call.ShareText(title, message, url))
        delegate?.share(title, message, url)
    }

    // --- CalendarAccess ---

    override fun openEvent(
        title: String,
        description: String,
        location: String,
        start: LocalDateTime,
        end: LocalDateTime,
        zone: TimeZone
    ) {
        calls.add(Call.OpenEvent(title))
        delegate?.openEvent(title, description, location, start, end, zone)
    }

    // --- GeolocationAccess ---

    override suspend fun getCurrentPosition(): GeolocationResult {
        // by Claude - record after resolving so Call.GetCurrentPosition captures the actual result
        val result = if (pendingGeolocation.isNotEmpty()) pendingGeolocation.removeFirst()
        else delegate?.getCurrentPosition()
        calls.add(Call.GetCurrentPosition(result))
        return result ?: throw UnsupportedOperationException(
            "No mock geolocation queued and no delegate available"
        )
    }
}
