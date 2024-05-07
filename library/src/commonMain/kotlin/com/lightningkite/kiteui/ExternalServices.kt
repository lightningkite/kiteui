package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageSource
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

expect object ExternalServices {
    fun openTab(url: String)
    suspend fun requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference?
    suspend fun requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference>
    suspend fun requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference?
    suspend fun requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference?
    fun setClipboardText(value: String)
    fun download(name: String, url: String, onProgress: (Double) -> Unit = {})
    fun share(title: String, message: String? = null, url: String? = null)
    fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone)
    fun openMap(latitude: Double, longitude: Double, label: String? = null, zoom: Float? = null)
//    fun download(blob: Blob)
//    fun download(url: String)
}

fun ExternalServices.requestFile(mimeTypes: List<String> = listOf("*/*"), onResult: (FileReference?) -> Unit) = launchGlobal { onResult(try { requestFile(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }
fun ExternalServices.requestFiles(mimeTypes: List<String> = listOf("*/*"), onResult: (List<FileReference>) -> Unit) = launchGlobal { onResult(try { requestFiles(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); listOf() }) }
fun ExternalServices.requestCaptureSelf(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?) -> Unit) = launchGlobal { onResult(try { requestCaptureSelf(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }
fun ExternalServices.requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?) -> Unit) = launchGlobal { onResult(try { requestCaptureEnvironment(mimeTypes) } catch(e: Exception) { e.printStackTrace2(); null }) }