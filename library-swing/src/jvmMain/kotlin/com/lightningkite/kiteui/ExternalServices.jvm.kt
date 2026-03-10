// by Claude - migrated from old actual extension functions to ExternalServicesAccess interface
package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class SwingExternalServices : ExternalServicesAccess {
    override fun openLink(url: String, newTab: Boolean) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
    override fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
        // by Claude - open map URL in browser
        val query = if (label != null) "$latitude,$longitude($label)" else "$latitude,$longitude"
        openLink("https://www.google.com/maps/search/?api=1&query=$query")
    }
    // by Claude - Swing file picker via JFileChooser
    override suspend fun requestFile(mimeTypes: List<String>): FileReference? {
        val chooser = JFileChooser()
        chooser.isMultiSelectionEnabled = false
        applyMimeFilter(chooser, mimeTypes)
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            FileReference(chooser.selectedFile)
        } else null
    }
    override suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> {
        val chooser = JFileChooser()
        chooser.isMultiSelectionEnabled = true
        applyMimeFilter(chooser, mimeTypes)
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFiles.map { FileReference(it) }
        } else emptyList()
    }
    override suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? =
        throw UnsupportedOperationException("Camera capture is not available on Swing")
    override suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? =
        throw UnsupportedOperationException("Camera capture is not available on Swing")
    override fun setClipboardText(value: String) {
        // by Claude - use AWT clipboard
        val selection = StringSelection(value)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }
    override suspend fun share(namesToBlobs: List<Pair<String, Blob>>) {}
    override fun share(title: String, message: String?, url: String?) {}
    override fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) {}
    override suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {}
    override suspend fun download(name: String, url: String, preferredDestination: DownloadLocation, onDownloadProgress: ((progress: Float) -> Unit)?) {}
    override suspend fun getCurrentPosition(): GeolocationResult = throw UnsupportedOperationException("Geolocation is not available on Swing")

    // by Claude - convert MIME types to Swing file extensions where possible
    private fun applyMimeFilter(chooser: JFileChooser, mimeTypes: List<String>) {
        if (mimeTypes.isEmpty() || mimeTypes.contains("*/*")) return
        val extensions = mimeTypes.flatMap { mime ->
            when {
                mime == "image/*" -> listOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
                mime == "video/*" -> listOf("mp4", "avi", "mov", "mkv", "webm")
                mime == "audio/*" -> listOf("mp3", "wav", "ogg", "flac", "aac")
                mime.contains("/") -> listOf(mime.substringAfter("/"))
                else -> listOf(mime)
            }
        }
        if (extensions.isNotEmpty()) {
            chooser.fileFilter = FileNameExtensionFilter("Allowed files (${extensions.joinToString()})", *extensions.toTypedArray())
        }
    }
}

actual fun externalServicesAccessDefault(context: RContext): ExternalServicesAccess = SwingExternalServices()
