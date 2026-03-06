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
class MockExternalServices(
    private val delegate: ExternalServicesAccess? = null
) : ExternalServicesAccess {

    // --- Queued responses (FIFO) ---
    val pendingFileResponses = mutableListOf<FileReference?>()
    val pendingFilesResponses = mutableListOf<List<FileReference>>()
    val pendingCaptureResponses = mutableListOf<FileReference?>()
    val pendingGeolocation = mutableListOf<GeolocationResult>()

    // --- Recorded calls ---
    sealed class Call {
        data class OpenLink(val url: String, val newTab: Boolean) : Call()
        data class OpenMap(val latitude: Double, val longitude: Double, val label: String?, val zoom: Float?) : Call()
        data class RequestFile(val mimeTypes: List<String>) : Call()
        data class RequestFiles(val mimeTypes: List<String>) : Call()
        data class RequestCaptureSelf(val mimeTypes: List<String>) : Call()
        data class RequestCaptureEnvironment(val mimeTypes: List<String>) : Call()
        data class SetClipboardText(val text: String) : Call()
        data class ShareBlobs(val names: List<String>) : Call()
        data class ShareText(val title: String, val message: String?, val url: String?) : Call()
        data class OpenEvent(val title: String) : Call()
        data class DownloadBlob(val name: String) : Call()
        data class DownloadUrl(val name: String, val url: String) : Call()
        data class GetCurrentPosition(val result: GeolocationResult?) : Call()
    }

    val calls = mutableListOf<Call>()

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
        return if (pendingFileResponses.isNotEmpty()) pendingFileResponses.removeAt(0)
        else delegate?.requestFile(mimeTypes)
    }

    override suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> {
        calls.add(Call.RequestFiles(mimeTypes))
        return if (pendingFilesResponses.isNotEmpty()) pendingFilesResponses.removeAt(0)
        else delegate?.requestFiles(mimeTypes) ?: emptyList()
    }

    // by Claude - fall back to pendingFileResponses so mockFile() works for capture too
    override suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? {
        calls.add(Call.RequestCaptureSelf(mimeTypes))
        return if (pendingCaptureResponses.isNotEmpty()) pendingCaptureResponses.removeAt(0)
        else if (pendingFileResponses.isNotEmpty()) pendingFileResponses.removeAt(0)
        else delegate?.requestCaptureSelf(mimeTypes)
    }

    // by Claude - fall back to pendingFileResponses so mockFile() works for capture too
    override suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? {
        calls.add(Call.RequestCaptureEnvironment(mimeTypes))
        return if (pendingCaptureResponses.isNotEmpty()) pendingCaptureResponses.removeAt(0)
        else if (pendingFileResponses.isNotEmpty()) pendingFileResponses.removeAt(0)
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
        val result = if (pendingGeolocation.isNotEmpty()) pendingGeolocation.removeAt(0)
        else delegate?.getCurrentPosition()
        calls.add(Call.GetCurrentPosition(result))
        return result ?: throw UnsupportedOperationException(
            "No mock geolocation queued and no delegate available"
        )
    }
}
