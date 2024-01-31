package com.lightningkite.rock

import java.io.File
import java.nio.file.Files

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
actual suspend fun fetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?
): RequestResponse = TODO()
actual inline fun httpHeaders(map: Map<String, String>): HttpHeaders = TODO()
actual inline fun httpHeaders(headers: HttpHeaders): HttpHeaders =  TODO()
actual inline fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders = TODO()
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
    actual suspend fun headers(): Map<String, List<String>> = TODO()
}

actual class Blob(val file: File, val type: String) {

}
actual typealias FileReference = File

actual fun FileReference.mimeType(): String {
    return Files.probeContentType(this.toPath())
}
actual fun FileReference.fileName(): String {
    return this.name
}

actual fun websocket(url: String): WebSocket = TODO()