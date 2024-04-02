package com.lightningkite.kiteui

actual object ExternalServices {
    actual fun openTab(url: String) = Unit
    actual suspend fun requestFile(mimeTypes: List<String>): FileReference? = TODO()
    actual suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> = TODO()
    actual suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? = TODO()
    actual suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? = TODO()
    actual fun setClipboardText(value: String) : Unit = TODO()
}