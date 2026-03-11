package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

// by Claude
// No-op stubs for SSR: these operations are not available server-side.

class JvmExternalServices : ExternalServicesAccess {
    override fun openLink(url: String, newTab: Boolean) {}
    override fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {}
    override suspend fun requestFile(mimeTypes: List<String>): FileReference? = null
    override suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> = emptyList()
    override suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? = null
    override suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = null
    override fun setClipboardText(value: String) {}
    override suspend fun share(namesToBlobs: List<Pair<String, Blob>>) {}
    override fun share(title: String, message: String?, url: String?) {}
    override fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) {}
    override suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {}
    override suspend fun download(name: String, url: String, preferredDestination: DownloadLocation, onDownloadProgress: ((progress: Float) -> Unit)?) {}
    override suspend fun getCurrentPosition(): GeolocationResult = throw UnsupportedOperationException("Geolocation is not available in SSR")
}

actual fun externalServicesAccessDefault(context: RContext): ExternalServicesAccess = JvmExternalServices()
