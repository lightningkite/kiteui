package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.rContextAddonGenerate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.js.JsName

// by Claude

enum class DownloadLocation { Downloads, Pictures }

interface ExternalLinksAccess {
    fun openLink(url: String, newTab: Boolean = true)
    fun openTab(url: String) = openLink(url, true)
    fun openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null)
}

interface FilePickerAccess {
    suspend fun requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference?
    suspend fun requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference>
    suspend fun requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference?
    suspend fun requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference?
}

interface FileDownloadAccess {
    @JsName("downloadBlob")
    suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads)
    suspend fun download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null)
}

interface SharingAccess {
    fun setClipboardText(value: String)
    @JsName("shareBlob")
    suspend fun share(namesToBlobs: List<Pair<String, Blob>>)
    fun share(title: String, message: String? = null, url: String? = null)
}

interface CalendarAccess {
    fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone)
}

// by Claude
interface GeolocationAccess {
    suspend fun getCurrentPosition(): GeolocationResult
}

// Combined interface for backward compatibility
interface ExternalServicesAccess : ExternalLinksAccess, FilePickerAccess, FileDownloadAccess, SharingAccess, CalendarAccess, GeolocationAccess

var ViewWriter.externalServices: ExternalServicesAccess by rContextAddonGenerate { externalServicesAccessDefault(context) }

// by Claude - convenience extension on RContext for use outside ViewWriter scope.
// Uses same addons cache key as the ViewWriter property to pick up mocks.
val RContext.externalServices: ExternalServicesAccess
    get() = addons.getOrPut("externalServices") { externalServicesAccessDefault(this) } as ExternalServicesAccess

// Convenience extensions on RContext delegating to externalServices
fun RContext.openLink(url: String, newTab: Boolean = true) = externalServices.openLink(url, newTab)
fun RContext.openTab(url: String) = externalServices.openTab(url)
fun RContext.openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null) = externalServices.openMap(latitude, longitude, label, zoom)
suspend fun RContext.requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference? = externalServices.requestFile(mimeTypes)
suspend fun RContext.requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference> = externalServices.requestFiles(mimeTypes)
suspend fun RContext.requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference? = externalServices.requestCaptureSelf(mimeTypes)
suspend fun RContext.requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference? = externalServices.requestCaptureEnvironment(mimeTypes)
fun RContext.setClipboardText(value: String) = externalServices.setClipboardText(value)
suspend fun RContext.download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads) = externalServices.download(name, blob, preferredDestination)
suspend fun RContext.download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null) = externalServices.download(name, url, preferredDestination, onDownloadProgress)
suspend fun RContext.share(namesToBlobs: List<Pair<String, Blob>>) = externalServices.share(namesToBlobs)
fun RContext.share(title: String, message: String? = null, url: String? = null) = externalServices.share(title, message, url)
fun RContext.openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) = externalServices.openEvent(title, description, location, start, end, zone)
suspend fun RContext.getCurrentPosition(): GeolocationResult = externalServices.getCurrentPosition()

expect fun externalServicesAccessDefault(context: RContext): ExternalServicesAccess
