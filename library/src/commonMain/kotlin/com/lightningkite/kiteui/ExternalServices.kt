package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.Job
import kotlin.js.JsName
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

public enum class DownloadLocation { Downloads, Pictures }
public object ExternalServices {
    public lateinit var baseContext: RContext
    public fun openTab(url: String): Unit = baseContext.openTab(url)
    @Deprecated("Use RContext.requestFile instead")
    public suspend fun requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference? = baseContext.requestFile(mimeTypes)
    @Deprecated("Use RContext.requestFiles instead")
    public suspend fun requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference> = baseContext.requestFiles(mimeTypes)
    @Deprecated("Use RContext.requestCaptureSelf instead")
    public suspend fun requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference? = baseContext.requestCaptureSelf(mimeTypes)
    @Deprecated("Use RContext.requestCaptureEnvironment instead")
    public suspend fun requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference? = baseContext.requestCaptureEnvironment(mimeTypes)
    public fun setClipboardText(value: String): Unit = baseContext.setClipboardText(value)
    @JsName("downloadBlob")
    public suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads): Unit = baseContext.download(name, blob, preferredDestination)
    public suspend fun download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null): Unit = baseContext.download(name, url, preferredDestination, onDownloadProgress)

    @JsName("shareBlob")
    @Deprecated("Use RContext.share instead")
    public suspend fun share(namesToBlobs: List<Pair<String, Blob>>): Unit = baseContext.share(namesToBlobs)
    @Deprecated("Use RContext.share( instead")
    public fun share(title: String, message: String? = null, url: String? = null): Unit = baseContext.share(title, message, url)
    public fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone): Unit = baseContext.openEvent(title, description, location, start, end, zone)
    public fun openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null): Unit = baseContext.openMap(latitude, longitude, label, zoom)
}

@Deprecated("Use RContext and suspend instead.")
public fun ExternalServices.requestFile(mimeTypes: List<String> = listOf("*/*"), onResult: (FileReference?) -> Unit): Job =
    AppScope.launch { onResult(try { requestFile(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }
@Deprecated("Use RContext and suspend instead.")
public fun ExternalServices.requestFiles(mimeTypes: List<String> = listOf("*/*"), onResult: (List<FileReference>) -> Unit): Job =
    AppScope.launch { onResult(try { requestFiles(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); listOf() }) }
@Deprecated("Use RContext and suspend instead.")
public fun ExternalServices.requestCaptureSelf(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?) -> Unit): Job =
    AppScope.launch { onResult(try { requestCaptureSelf(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }
@Deprecated("Use RContext and suspend instead.")
public fun ExternalServices.requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?) -> Unit): Job =
    AppScope.launch { onResult(try { requestCaptureEnvironment(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }



public expect fun RContext.openTab(url: String)
public expect suspend fun RContext.requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference?
public expect suspend fun RContext.requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference>
public expect suspend fun RContext.requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference?
public expect suspend fun RContext.requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference?
public expect fun RContext.setClipboardText(value: String)
@JsName("downloadBlob")
public expect suspend fun RContext.download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads)
public expect suspend fun RContext.download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null)
@JsName("shareBlob")
public expect suspend fun RContext.share(namesToBlobs: List<Pair<String, Blob>>)
public expect fun RContext.share(title: String, message: String? = null, url: String? = null)
public expect fun RContext.openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone)
public expect fun RContext.openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null)
