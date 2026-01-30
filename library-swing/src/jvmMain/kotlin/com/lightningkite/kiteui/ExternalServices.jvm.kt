package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

actual suspend fun RContext.requestFile(mimeTypes: List<String>): FileReference? = TODO()
actual suspend fun RContext.requestFiles(mimeTypes: List<String>): List<FileReference> = TODO()
actual suspend fun RContext.requestCaptureSelf(mimeTypes: List<String>): FileReference? = TODO()
actual suspend fun RContext.requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = TODO()
actual fun RContext.setClipboardText(value: String) : Unit = TODO()
actual suspend fun RContext.share(namesToBlobs: List<Pair<String, Blob>>) {

}
actual fun RContext.share(title: String, message: String?, url: String?){

}
actual fun RContext.openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone){

}

actual suspend fun RContext.download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
}

actual suspend fun RContext.download(
    name: String,
    url: String,
    preferredDestination: DownloadLocation,
    onDownloadProgress: ((progress: Float) -> Unit)?
) {

}

actual fun RContext.openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
}