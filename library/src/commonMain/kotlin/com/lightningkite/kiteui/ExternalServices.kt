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

// by Claude - deprecated singleton for migration; callers should use RContext.externalServices instead
@Deprecated("Use RContext.externalServices or ViewWriter.externalServices instead. This singleton will be removed.", level = DeprecationLevel.WARNING)
object ExternalServices {
    @Deprecated("Use RContext.externalServices instead", level = DeprecationLevel.WARNING)
    lateinit var baseContext: RContext

    @Suppress("DEPRECATION")
    private val ctx get() = baseContext

    @Deprecated("Use RContext.openLink instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.openLink(url)", "com.lightningkite.kiteui.openLink"))
    fun openTab(url: String) = ctx.openLink(url)

    @Deprecated("Use RContext.requestFile instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.requestFile(mimeTypes)", "com.lightningkite.kiteui.requestFile"))
    suspend fun requestFile(mimeTypes: kotlin.collections.List<String> = listOf("*/*")): FileReference? = ctx.requestFile(mimeTypes)

    @Deprecated("Use RContext.requestFiles instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.requestFiles(mimeTypes)", "com.lightningkite.kiteui.requestFiles"))
    suspend fun requestFiles(mimeTypes: kotlin.collections.List<String> = listOf("*/*")): kotlin.collections.List<FileReference> = ctx.requestFiles(mimeTypes)

    @Deprecated("Use RContext.requestCaptureSelf instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.requestCaptureSelf(mimeTypes)", "com.lightningkite.kiteui.requestCaptureSelf"))
    suspend fun requestCaptureSelf(mimeTypes: kotlin.collections.List<String> = listOf("image/*")): FileReference? = ctx.requestCaptureSelf(mimeTypes)

    @Deprecated("Use RContext.requestCaptureEnvironment instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.requestCaptureEnvironment(mimeTypes)", "com.lightningkite.kiteui.requestCaptureEnvironment"))
    suspend fun requestCaptureEnvironment(mimeTypes: kotlin.collections.List<String> = listOf("image/*")): FileReference? = ctx.requestCaptureEnvironment(mimeTypes)

    @Deprecated("Use RContext.setClipboardText instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.setClipboardText(value)", "com.lightningkite.kiteui.setClipboardText"))
    fun setClipboardText(value: String) = ctx.setClipboardText(value)

    @Deprecated("Use RContext.download instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.download(name, blob, preferredDestination)", "com.lightningkite.kiteui.download"))
    @JsName("downloadBlob")
    suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads) = ctx.download(name, blob, preferredDestination)

    @Deprecated("Use RContext.download instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.download(name, url, preferredDestination, onDownloadProgress)", "com.lightningkite.kiteui.download"))
    suspend fun download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null) = ctx.download(name, url, preferredDestination, onDownloadProgress)

    @Deprecated("Use RContext.share instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.share(namesToBlobs)", "com.lightningkite.kiteui.share"))
    @JsName("shareBlob")
    suspend fun share(namesToBlobs: kotlin.collections.List<Pair<String, Blob>>) = ctx.share(namesToBlobs)

    @Deprecated("Use RContext.share instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.share(title, message, url)", "com.lightningkite.kiteui.share"))
    fun share(title: String, message: String? = null, url: String? = null) = ctx.share(title, message, url)

    @Deprecated("Use RContext.openEvent instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.openEvent(title, description, location, start, end, zone)", "com.lightningkite.kiteui.openEvent"))
    fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) = ctx.openEvent(title, description, location, start, end, zone)

    @Deprecated("Use RContext.openMap instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.openMap(latitude, longitude, label, zoom)", "com.lightningkite.kiteui.openMap"))
    fun openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null) = ctx.openMap(latitude, longitude, label, zoom)
}

// by Claude - rContextAddonGenerate uses property.name as the ChainMap key.
// The RContext extension below must use the same key to share the same instance.
var ViewWriter.externalServices: ExternalServicesAccess by rContextAddonGenerate { externalServicesAccessDefault(context) }

// by Claude - convenience extension on RContext for use outside ViewWriter scope.
// Uses getOrPut so the default is shared across the context tree and picks up mocks.
// Key must match ViewWriter.externalServices property name used by rContextAddonGenerate.
val RContext.externalServices: ExternalServicesAccess
    get() = addons.getOrPut(ViewWriter::externalServices.name) { externalServicesAccessDefault(this) } as ExternalServicesAccess

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
