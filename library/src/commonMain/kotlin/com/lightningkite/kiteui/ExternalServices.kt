package com.lightningkite.kiteui

import com.lightningkite.signal.AppScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.js.JsName

public enum class DownloadLocation { Downloads, Pictures }
public expect object ExternalServices {
    public fun openTab(url: String)
    public suspend fun requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference?
    public suspend fun requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference>
    public suspend fun requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference?
    public suspend fun requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference?
    public fun setClipboardText(value: String)
    @JsName("downloadBlob")
    public suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation = DownloadLocation.Downloads)
    public suspend fun download(name: String, url: String, preferredDestination: DownloadLocation = DownloadLocation.Downloads, onDownloadProgress: ((progress: Float) -> Unit)? = null)

    @JsName("shareBlob")
    public suspend fun share(namesToBlobs: List<Pair<String, Blob>>)
    public fun share(title: String, message: String? = null, url: String? = null)
    public fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone)
    public fun openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null)
//    fun download(blob: Blob)
//    fun download(url: String)
}

public fun ExternalServices.requestFile(mimeTypes: List<String> = listOf("*/*"), onResult: (FileReference?) -> Unit) =
    AppScope.launch { onResult(try { requestFile(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }
public fun ExternalServices.requestFiles(mimeTypes: List<String> = listOf("*/*"), onResult: (List<FileReference>) -> Unit) =
    AppScope.launch { onResult(try { requestFiles(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); listOf() }) }
public fun ExternalServices.requestCaptureSelf(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?) -> Unit) =
    AppScope.launch { onResult(try { requestCaptureSelf(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }
public fun ExternalServices.requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?) -> Unit) =
    AppScope.launch { onResult(try { requestCaptureEnvironment(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }