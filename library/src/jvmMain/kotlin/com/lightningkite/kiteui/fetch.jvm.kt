package com.lightningkite.kiteui

import java.io.File
import java.nio.file.Files

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
actual suspend fun fetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((Int, Int) -> Unit)?,
    onDownloadProgress: ((Int, Int) -> Unit)?,
): RequestResponse = TODO()
actual inline fun httpHeaders(map: Map<String, String>): HttpHeaders = HttpHeaders()
actual inline fun httpHeaders(headers: HttpHeaders): HttpHeaders =  HttpHeaders()
actual inline fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders = HttpHeaders()
actual inline fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders = HttpHeaders()
actual class HttpHeaders {
    actual fun append(name: String, value: String): Unit = TODO()
    actual fun delete(name: String): Unit = TODO()
    actual fun get(name: String): String?= TODO()
    actual fun has(name: String): Boolean = TODO()

    actual fun set(name: String, value: String): Unit = TODO()
}
actual class RequestResponse {
    actual val status: Short get() = TODO()
    actual val ok: Boolean get() = TODO()
    actual suspend fun text(): String = TODO()
    actual suspend fun blob(): Blob = TODO()
    actual val headers: HttpHeaders = TODO()
}

actual class Blob(val data: ByteArray, val type: String)
actual typealias FileReference = File

actual fun FileReference.mimeType(): String {
    return Files.probeContentType(this.toPath())
}
actual fun Blob.mimeType(): String = type
actual fun FileReference.fileName(): String {
    return this.name
}

actual fun websocket(url: String): WebSocket = TODO()
actual fun Blob.bytes(): Long = -1L
actual fun FileReference.bytes(): Long = -1L

//actual suspend fun Blob.byteArray(): ByteArray = data
/*actual suspend fun FileReference.byteArray(): ByteArray = TODO()*/ /*withContext(Dispatchers.IO) {
    AndroidAppContext.applicationCtx.contentResolver.openInputStream(uri)!!.readBytes()
}*/

actual suspend fun Blob.text(): String = data.toString(Charsets.UTF_8)
actual suspend fun FileReference.text(): String = TODO() /*withContext(Dispatchers.IO) {
    AndroidAppContext.applicationCtx.contentResolver.openInputStream(uri)!!.reader(Charsets.UTF_8).readText()
}*/