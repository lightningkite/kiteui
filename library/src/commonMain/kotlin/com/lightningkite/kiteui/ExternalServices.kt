package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.lazyContextAddon
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.js.JsName

// by Claude

public enum class DownloadLocation { Downloads, Pictures }

public interface ExternalLinksAccess {
    public fun openLink(url: String, newTab: Boolean = true)
    public fun openTab(url: String) = openLink(url, true)
    public fun openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null)
}

public interface FilePickerAccess {
    public suspend fun requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference?
    public suspend fun requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference>
    public suspend fun requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference?
    public suspend fun requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference?
}

public interface FileDownloadAccess {
    @JsName("downloadBlob")
    public suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads)
    public suspend fun download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null)
}

public interface SharingAccess {
    public fun setClipboardText(value: String)
    @JsName("shareBlob")
    public suspend fun share(namesToBlobs: List<Pair<String, Blob>>)
    public fun share(title: String, message: String? = null, url: String? = null)
}

public interface CalendarAccess {
    public fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone)
}

// by Claude
public interface GeolocationAccess {
    public suspend fun getCurrentPosition(): GeolocationResult
}

// Combined interface for backward compatibility
public interface ExternalServicesAccess : ExternalLinksAccess, FilePickerAccess, FileDownloadAccess, SharingAccess, CalendarAccess, GeolocationAccess

// by Claude - deprecated singleton for migration; callers should use RContext.externalServices instead
@Deprecated("Use RContext.externalServices or ViewWriter.externalServices instead. This singleton will be removed.", level = DeprecationLevel.WARNING)
public object ExternalServices {
    @Deprecated("Use RContext.externalServices instead", level = DeprecationLevel.WARNING)
    public lateinit var baseContext: ElementContext

    @Suppress("DEPRECATION")
    private val ctx get() = baseContext

    @Deprecated("Use RContext.openLink instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.openLink(url)", "com.lightningkite.kiteui.openLink"))
    public fun openTab(url: String) = ctx.openLink(url)

    @Deprecated("Use RContext.requestFile instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.requestFile(mimeTypes)", "com.lightningkite.kiteui.requestFile"))
    public suspend fun requestFile(mimeTypes: kotlin.collections.List<String> = listOf("*/*")): FileReference? = ctx.requestFile(mimeTypes)

    @Deprecated("Use RContext.requestFiles instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.requestFiles(mimeTypes)", "com.lightningkite.kiteui.requestFiles"))
    public suspend fun requestFiles(mimeTypes: kotlin.collections.List<String> = listOf("*/*")): kotlin.collections.List<FileReference> = ctx.requestFiles(mimeTypes)

    @Deprecated("Use RContext.requestCaptureSelf instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.requestCaptureSelf(mimeTypes)", "com.lightningkite.kiteui.requestCaptureSelf"))
    public suspend fun requestCaptureSelf(mimeTypes: kotlin.collections.List<String> = listOf("image/*")): FileReference? = ctx.requestCaptureSelf(mimeTypes)

    @Deprecated("Use RContext.requestCaptureEnvironment instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.requestCaptureEnvironment(mimeTypes)", "com.lightningkite.kiteui.requestCaptureEnvironment"))
    public suspend fun requestCaptureEnvironment(mimeTypes: kotlin.collections.List<String> = listOf("image/*")): FileReference? = ctx.requestCaptureEnvironment(mimeTypes)

    @Deprecated("Use RContext.setClipboardText instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.setClipboardText(value)", "com.lightningkite.kiteui.setClipboardText"))
    public fun setClipboardText(value: String) = ctx.setClipboardText(value)

    @Deprecated("Use RContext.download instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.download(name, blob, preferredDestination)", "com.lightningkite.kiteui.download"))
    @JsName("downloadBlob")
    public suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads) = ctx.download(name, blob, preferredDestination)

    @Deprecated("Use RContext.download instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.download(name, url, preferredDestination, onDownloadProgress)", "com.lightningkite.kiteui.download"))
    public suspend fun download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null) = ctx.download(name, url, preferredDestination, onDownloadProgress)

    @Deprecated("Use RContext.share instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.share(namesToBlobs)", "com.lightningkite.kiteui.share"))
    @JsName("shareBlob")
    public suspend fun share(namesToBlobs: kotlin.collections.List<Pair<String, Blob>>) = ctx.share(namesToBlobs)

    @Deprecated("Use RContext.share instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.share(title, message, url)", "com.lightningkite.kiteui.share"))
    public fun share(title: String, message: String? = null, url: String? = null) = ctx.share(title, message, url)

    @Deprecated("Use RContext.openEvent instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.openEvent(title, description, location, start, end, zone)", "com.lightningkite.kiteui.openEvent"))
    public fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) = ctx.openEvent(title, description, location, start, end, zone)

    @Deprecated("Use RContext.openMap instead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("context.openMap(latitude, longitude, label, zoom)", "com.lightningkite.kiteui.openMap"))
    public fun openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null) = ctx.openMap(latitude, longitude, label, zoom)
}

public val ElementContext.externalServices: ExternalServicesAccess by lazyContextAddon { externalServicesAccessDefault(it) }

// by Claude - rContextAddonGenerate uses property.name as the ChainMap key.
// The RContext extension below must use the same key to share the same instance.
@Deprecated("Use directly through context", ReplaceWith("context.externalServices"))
public val ViewWriter.externalServices: ExternalServicesAccess get() = context.externalServices

// Convenience extensions on RContext delegating to externalServices
public fun ElementContext.openLink(url: String, newTab: Boolean = true) = externalServices.openLink(url, newTab)
public fun ElementContext.openTab(url: String) = externalServices.openTab(url)
public fun ElementContext.openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null) = externalServices.openMap(latitude, longitude, label, zoom)
public suspend fun ElementContext.requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference? = externalServices.requestFile(mimeTypes)
public suspend fun ElementContext.requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference> = externalServices.requestFiles(mimeTypes)
public suspend fun ElementContext.requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference? = externalServices.requestCaptureSelf(mimeTypes)
public suspend fun ElementContext.requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference? = externalServices.requestCaptureEnvironment(mimeTypes)
public fun ElementContext.setClipboardText(value: String) = externalServices.setClipboardText(value)
public suspend fun ElementContext.download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads) = externalServices.download(name, blob, preferredDestination)
public suspend fun ElementContext.download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null) = externalServices.download(name, url, preferredDestination, onDownloadProgress)
public suspend fun ElementContext.share(namesToBlobs: List<Pair<String, Blob>>) = externalServices.share(namesToBlobs)
public fun ElementContext.share(title: String, message: String? = null, url: String? = null) = externalServices.share(title, message, url)
public fun ElementContext.openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) = externalServices.openEvent(title, description, location, start, end, zone)
public suspend fun ElementContext.getCurrentPosition(): GeolocationResult = externalServices.getCurrentPosition()

public expect fun externalServicesAccessDefault(context: ElementContext): ExternalServicesAccess
