package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.js.JsName
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

public enum class DownloadLocation { Downloads, Pictures }
public object ExternalServices {
    public lateinit var baseContext: RContext
    fun openTab(url: String) = baseContext.openTab(url)
    @Deprecated("Use RContext.requestFile instead") suspend fun requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference? = baseContext.requestFile(mimeTypes)
    @Deprecated("Use RContext.requestFiles instead") suspend fun requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference> = baseContext.requestFiles(mimeTypes)
    @Deprecated("Use RContext.requestCaptureSelf instead") suspend fun requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference? = baseContext.requestCaptureSelf(mimeTypes)
    @Deprecated("Use RContext.requestCaptureEnvironment instead") suspend fun requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference? = baseContext.requestCaptureEnvironment(mimeTypes)
    fun setClipboardText(value: String) = baseContext.setClipboardText(value)
    @JsName("downloadBlob")
    suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads) = baseContext.download(name, blob, preferredDestination)
    suspend fun download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null) = baseContext.download(name, url, preferredDestination, onDownloadProgress)

    @JsName("shareBlob")
    @Deprecated("Use RContext.share instead") suspend fun share(namesToBlobs: List<Pair<String, Blob>>) = baseContext.share(namesToBlobs)
    @Deprecated("Use RContext.share( instead") fun share(title: String, message: String? = null, url: String? = null) = baseContext.share(title, message, url)
    fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) = baseContext.openEvent(title, description, location, start, end, zone)
    fun openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null) = baseContext.openMap(latitude, longitude, label, zoom)
}

@Deprecated("Use RContext and suspend instead.") fun ExternalServices.requestFile(mimeTypes: List<String> = listOf("*/*"), onResult: (FileReference?) -> Unit) =
    AppScope.launch { onResult(try { requestFile(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }
@Deprecated("Use RContext and suspend instead.") fun ExternalServices.requestFiles(mimeTypes: List<String> = listOf("*/*"), onResult: (List<FileReference>) -> Unit) =
    AppScope.launch { onResult(try { requestFiles(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); listOf() }) }
@Deprecated("Use RContext and suspend instead.") fun ExternalServices.requestCaptureSelf(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?) -> Unit) =
    AppScope.launch { onResult(try { requestCaptureSelf(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }
@Deprecated("Use RContext and suspend instead.") fun ExternalServices.requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?) -> Unit) =
    AppScope.launch { onResult(try { requestCaptureEnvironment(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }



expect fun RContext.openTab(url: String)
expect suspend fun RContext.requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference?
expect suspend fun RContext.requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference>
expect suspend fun RContext.requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference?
expect suspend fun RContext.requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference?
expect fun RContext.setClipboardText(value: String)
@JsName("downloadBlob")
expect suspend fun RContext.download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads)
expect suspend fun RContext.download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null)
@JsName("shareBlob")
expect suspend fun RContext.share(namesToBlobs: List<Pair<String, Blob>>)
expect fun RContext.share(title: String, message: String? = null, url: String? = null)
expect fun RContext.openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone)
expect fun RContext.openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null)
