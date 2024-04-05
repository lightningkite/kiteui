package com.lightningkite.kiteui

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.DataTransfer
import org.w3c.dom.HTMLInputElement
import kotlin.coroutines.resume

actual object ExternalServices {
    actual fun openTab(url: String) {
        window.open(url, "_blank")
    }
    suspend fun requestFileInput(mimeTypes: List<String>, setup: HTMLInputElement.()->Unit): List<FileReference> = suspendCoroutineCancellable {
        (document.createElement("input") as HTMLInputElement).apply {
            type = "file"
            accept = mimeTypes.joinToString(",")
            setup()
            onchange = { e -> it.resume(files?.let { (0 until it.length).map { index -> it.item(index)!! } } ?: listOf()) }
            oncancel = { e -> it.resume(listOf()) }
        }.click()
        return@suspendCoroutineCancellable { }
    }
    actual suspend fun requestFile(mimeTypes: List<String>) = requestFileInput(mimeTypes, {}).firstOrNull()
    actual suspend fun requestFiles(mimeTypes: List<String>) = requestFileInput(mimeTypes, { multiple = true })
    actual suspend fun requestCaptureSelf(mimeTypes: List<String>) = requestFileInput(mimeTypes, { setAttribute("capture", "user") }).firstOrNull()
    actual suspend fun requestCaptureEnvironment(mimeTypes: List<String>) = requestFileInput(mimeTypes, { setAttribute("capture", "environment") }).firstOrNull()
    actual fun setClipboardText(value: String) { window.navigator.clipboard.writeText(value) }
}