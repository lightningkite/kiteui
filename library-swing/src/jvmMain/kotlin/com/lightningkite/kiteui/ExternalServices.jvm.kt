// by Claude - migrated from old actual extension functions to ExternalServicesAccess interface
package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import java.awt.Desktop
import java.net.URI

class SwingExternalServices : ExternalServicesAccess {
    override fun openLink(url: String, newTab: Boolean) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
    override fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {}
    override suspend fun requestFile(mimeTypes: List<String>): FileReference? = TODO()
    override suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> = TODO()
    override suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? = TODO()
    override suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = TODO()
    override fun setClipboardText(value: String) { TODO() }
    override suspend fun share(namesToBlobs: List<Pair<String, Blob>>) {}
    override fun share(title: String, message: String?, url: String?) {}
    override fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) {}
    override suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {}
    override suspend fun download(name: String, url: String, preferredDestination: DownloadLocation, onDownloadProgress: ((progress: Float) -> Unit)?) {}
    override suspend fun getCurrentPosition(): GeolocationResult = throw UnsupportedOperationException("Geolocation is not available on Swing")
}

actual fun externalServicesAccessDefault(context: RContext): ExternalServicesAccess = SwingExternalServices()
