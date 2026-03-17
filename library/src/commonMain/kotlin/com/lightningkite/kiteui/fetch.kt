package com.lightningkite.kiteui

suspend inline fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    type: String = "text/plain",
    body: String
) = fetch(url = url, method = method, headers = headers, body = RequestBodyText(body, type))
suspend inline fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: Blob
) = fetch(url = url, method = method, headers = headers, body = RequestBodyBlob(body))
suspend inline fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: FileReference
) = fetch(url = url, method = method, headers = headers, body = RequestBodyFile(body))

/** Interceptor that wraps HTTP requests. Call [proceed] to continue the chain. */
typealias FetchInterceptor = suspend (
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse,
) -> RequestResponse

/** Interceptors applied to all [fetch] calls, in order. Each wraps the next in the chain. */
val fetchInterceptors: MutableList<FetchInterceptor> = mutableListOf()

suspend fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: RequestBody? = null,
    onUploadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
    onDownloadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
): RequestResponse {
    val interceptors = fetchInterceptors.toList()
    var proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse = { u, m, h, b ->
        fetchRaw(u, m, h, b, onUploadProgress, onDownloadProgress)
    }
    for (interceptor in interceptors.asReversed()) {
        val next = proceed
        proceed = { u, m, h, b -> interceptor(u, m, h, b, next) }
    }
    return proceed(url, method, headers, body)
}

expect suspend fun fetchRaw(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: RequestBody? = null,
    onUploadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
    onDownloadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
): RequestResponse

class ConnectionException(message: String, cause: Exception? = null): Exception(message, cause)

enum class HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD }

fun httpHeaders(vararg entries: Pair<String, String>) = httpHeaders(entries.toList())
expect fun httpHeaders(map: Map<String, String> = mapOf()): HttpHeaders
expect fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders
expect fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders
expect fun httpHeaders(headers: HttpHeaders): HttpHeaders
expect class HttpHeaders {
    fun append(name: String, value: String)
    fun delete(name: String)
    fun get(name: String): String?
    fun has(name: String): Boolean
    fun set(name: String, value: String)
}

expect class RequestResponse {
    val status: Short
    val ok: Boolean
    val headers: HttpHeaders
    suspend fun text(): String
    suspend fun blob(): Blob
}

expect class Blob
expect class FileReference

expect fun createFileReferenceFromBytes(bytes: ByteArray, mimeType: String, fileName: String): FileReference

expect fun String.toBlob(contentType: String = "text/plain"): Blob
expect fun ByteArray.toBlob(contentType: String = "text/plain"): Blob
expect fun Blob.mimeType(): String
expect fun Blob.bytes(): Long
expect suspend fun Blob.toByteArray(): ByteArray
expect suspend fun Blob.text(): String
expect fun FileReference.mimeType():String
expect fun FileReference.bytes():Long
expect fun FileReference.fileName():String
expect suspend fun FileReference.text(): String

sealed interface RequestBody {
    val type: String
    val bytes: Long
}
data class RequestBodyText(val content: String, override val type: String): RequestBody {
    override val bytes: Long get() = content.encodeToByteArray().size.toLong()
}
data class RequestBodyBlob(val content: Blob): RequestBody {
    override val type: String get() = content.mimeType()
    override val bytes: Long get() = content.bytes()
}
data class RequestBodyFile(val content: FileReference): RequestBody {
    override val type: String get() = content.mimeType()
    override val bytes: Long get() = content.bytes()
}

expect fun websocket(url: String): WebSocket

interface WebSocket {
    fun close(code: Short, reason: String)
    fun send(data: String)
    fun send(data: Blob)
    fun onOpen(action: ()->Unit)
    fun onMessage(action: (String)->Unit)
    fun onBinaryMessage(action: (Blob)->Unit)
    fun onClose(action: (Short)->Unit)
    fun cancel() { close(1000, "Closed normally") }
}