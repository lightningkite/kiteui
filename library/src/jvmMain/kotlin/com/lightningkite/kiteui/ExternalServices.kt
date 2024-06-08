package com.lightningkite.kiteui

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

actual object ExternalServices {
    actual fun openTab(url: String) = Unit
    actual suspend fun requestFile(mimeTypes: List<String>): FileReference? = TODO()
    actual suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> = TODO()
    actual suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? = TODO()
    actual suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = TODO()
    actual fun setClipboardText(value: String) : Unit = TODO()
    actual suspend fun share(namesToBlobs: List<Pair<String, Blob>>) {

    }
    actual fun share(title: String, message: String?, url: String?){

    }
    actual fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone){

    }

    actual suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
    }

    actual suspend fun download(
        name: String,
        url: String,
        preferredDestination: DownloadLocation,
        onDownloadProgress: ((progress: Float) -> Unit)?
    ) {

    }

    actual fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
    }
}