package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

@InternalKiteUi
public actual fun RContext.openTab(url: String): Unit = Unit
public actual suspend fun RContext.requestFile(mimeTypes: List<String>): FileReference? = TODO()
public actual suspend fun RContext.requestFiles(mimeTypes: List<String>): List<FileReference> = TODO()
public actual suspend fun RContext.requestCaptureSelf(mimeTypes: List<String>): FileReference? = TODO()
@InternalKiteUi
public actual suspend fun RContext.requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = TODO()
public actual fun RContext.setClipboardText(value: String) : Unit = TODO()
public actual suspend fun RContext.share(namesToBlobs: List<Pair<String, Blob>>) {

}
public actual fun RContext.share(title: String, message: String?, url: String?){

}
public actual fun RContext.openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone){

}

public actual suspend fun RContext.download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
}

public actual suspend fun RContext.download(
    name: String,
    url: String,
    preferredDestination: DownloadLocation,
    onDownloadProgress: ((progress: Float) -> Unit)?
) {

}

@InternalKiteUi
public actual fun RContext.openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
}