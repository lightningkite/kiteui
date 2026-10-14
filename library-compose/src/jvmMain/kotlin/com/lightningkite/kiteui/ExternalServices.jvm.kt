package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.ElementContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

public class DesktopExternalServices : ExternalServicesAccess {
    override fun openLink(url: String, newTab: Boolean) {
        Desktop.getDesktop().browse(URI(url))
    }

    override fun setClipboardText(value: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
    }

    // TODO: the rest of these (phase 3 of plans/compose-desktop-backend.md).
    override fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?): Unit = TODO("Compose backend: openMap")
    override suspend fun requestFile(mimeTypes: List<String>): FileReference? = TODO("Compose backend: requestFile")
    override suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> = TODO("Compose backend: requestFiles")
    override suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? = TODO("Compose backend: requestCaptureSelf")
    override suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = TODO("Compose backend: requestCaptureEnvironment")
    override suspend fun share(namesToBlobs: List<Pair<String, Blob>>): Unit = TODO("Compose backend: share")
    override fun share(title: String, message: String?, url: String?): Unit = TODO("Compose backend: share")
    override fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone): Unit =
        TODO("Compose backend: openEvent")
    override suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation): Unit = TODO("Compose backend: download")
    override suspend fun download(name: String, url: String, preferredDestination: DownloadLocation, onDownloadProgress: ((progress: Float) -> Unit)?): Unit =
        TODO("Compose backend: download")
    override suspend fun getCurrentPosition(): GeolocationResult = throw UnsupportedOperationException("Geolocation is not available on desktop")
}

public actual fun externalServicesAccessDefault(context: ElementContext): ExternalServicesAccess = DesktopExternalServices()
