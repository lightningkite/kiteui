package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.ElementContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** Downloads go into [home]'s `Downloads` or `Pictures` folder. */
public class DesktopExternalServices(private val home: File = File(System.getProperty("user.home"))) : ExternalServicesAccess {
    override fun openLink(url: String, newTab: Boolean) {
        Desktop.getDesktop().browse(URI(url))
    }

    // The web's URL, so every desktop shows the same map whatever its default apps are.
    override fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
        openLink("https://www.google.com/maps/@${latitude},${longitude},${zoom ?: 16}")
    }

    override suspend fun requestFile(mimeTypes: List<String>): FileReference? = chooseFiles(mimeTypes, multiple = false).firstOrNull()
    override suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> = chooseFiles(mimeTypes, multiple = true)

    // Desktop browsers ignore the web's capture attribute and show the file picker; so does this.
    override suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? = requestFile(mimeTypes)
    override suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = requestFile(mimeTypes)

    private fun chooseFiles(mimeTypes: List<String>, multiple: Boolean): List<FileReference> {
        val dialog = FileDialog(null as Frame?, if (multiple) "Choose files" else "Choose a file", FileDialog.LOAD)
        dialog.isMultipleMode = multiple
        // Windows' native dialog ignores this filter and shows every file.
        dialog.setFilenameFilter { folder, name -> fileMatches(File(folder, name), mimeTypes) }
        dialog.isVisible = true // Blocks, while still handling other events, until the user picks or cancels.
        return dialog.files.map(::FileReference)
    }

    override fun setClipboardText(value: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
    }

    override suspend fun download(name: String, url: String, preferredDestination: DownloadLocation, onDownloadProgress: ((progress: Float) -> Unit)?) {
        val response = fetch(url, onDownloadProgress = onDownloadProgress?.let { report ->
            { done, total -> if (total > 0) report(done.toFloat() / total) }
        })
        check(response.ok) { "Downloading $url failed with status ${response.status}" }
        download(name, response.blob(), preferredDestination)
    }

    // Saved straight into the user's folder, as a browser does, rather than asking where.
    override suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
        if (!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
        val folder = File(home, preferredDestination.name)
        val base = File(name).nameWithoutExtension
        val extension = File(name).extension
        withContext(Dispatchers.IO) {
            folder.mkdirs()
            // Never overwrite: like a browser, "report.pdf" becomes "report (1).pdf".
            var file = File(folder, name)
            var copy = 0
            while (!file.createNewFile()) {
                copy++
                file = File(folder, if (extension.isEmpty()) "$base ($copy)" else "$base ($copy).$extension")
            }
            file.writeBytes(blob.data)
        }
    }

    override suspend fun share(namesToBlobs: List<Pair<String, Blob>>): Unit =
        throw UnsupportedOperationException("Desktop has no share sheet; download the files instead")

    override fun share(title: String, message: String?, url: String?): Unit =
        throw UnsupportedOperationException("Desktop has no share sheet; copy to the clipboard instead")

    // The web downloads an .ics file; opening one hands it to the user's calendar app.
    override fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) {
        val file = File.createTempFile("event", ".ics")
        file.deleteOnExit()
        file.writeText(icsEvent(title, description, location, start.toInstant(zone), end.toInstant(zone), Clock.System.now()))
        Desktop.getDesktop().open(file)
    }

    override suspend fun getCurrentPosition(): GeolocationResult = throw UnsupportedOperationException("Geolocation is not available on desktop")
}

public actual fun externalServicesAccessDefault(context: ElementContext): ExternalServicesAccess = DesktopExternalServices()

private val validDownloadName = Regex("[a-zA-Z0-9.\\-_]+")

/** Whether [file] is one of [mimeTypes], read as an HTML file input's `accept` is: exact types, any subtype of a type, or `.extension`s. */
internal fun fileMatches(file: File, mimeTypes: List<String>): Boolean {
    val type = Files.probeContentType(file.toPath())
    return mimeTypes.any { accepted ->
        when {
            accepted == "*/*" -> true
            accepted.startsWith(".") -> file.name.endsWith(accepted, ignoreCase = true)
            accepted.endsWith("/*") -> type != null && type.startsWith(accepted.removeSuffix("*"))
            else -> type == accepted
        }
    }
}

internal fun icsEvent(title: String, description: String, location: String, start: Instant, end: Instant, now: Instant): String {
    val utc = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    fun Instant.format() = utc.format(toJavaInstant())
    fun String.escaped() = replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
    return listOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//Lightning Kite//KiteUI//EN",
        "METHOD:PUBLISH",
        "BEGIN:VEVENT",
        "UID:${UUID.randomUUID()}",
        "DTSTAMP:${now.format()}",
        "DTSTART:${start.format()}",
        "DTEND:${end.format()}",
        "SUMMARY:${title.escaped()}",
        "DESCRIPTION:${description.escaped()}",
        "LOCATION:${location.escaped()}",
        "END:VEVENT",
        "END:VCALENDAR",
    ).joinToString("\r\n", postfix = "\r\n")
}
