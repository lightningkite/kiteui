package com.lightningkite.kiteui

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

public actual object ExternalServices {
    public actual fun openTab(url: String): Unit = Unit
    public actual suspend fun requestFile(mimeTypes: List<String>): FileReference? = TODO()
    public actual suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> = TODO()
    public actual suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? = TODO()
    public actual suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = TODO()
    public actual fun setClipboardText(value: String) : Unit = TODO()
    public actual suspend fun share(namesToBlobs: List<Pair<String, Blob>>) {

    }
    public actual fun share(title: String, message: String?, url: String?){

    }
    public actual fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone){

    }

    public actual suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
    }

    public actual suspend fun download(
        name: String,
        url: String,
        preferredDestination: DownloadLocation,
        onDownloadProgress: ((progress: Float) -> Unit)?
    ) {

    }

    public actual fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
    }
}