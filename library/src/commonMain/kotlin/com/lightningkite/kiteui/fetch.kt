package com.lightningkite.kiteui

public suspend inline fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    type: String = "text/plain",
    body: String
): RequestResponse = fetch(url = url, method = method, headers = headers, body = RequestBodyText(body, type))
public suspend inline fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: Blob
): RequestResponse = fetch(url = url, method = method, headers = headers, body = RequestBodyBlob(body))
public suspend inline fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: FileReference
): RequestResponse = fetch(url = url, method = method, headers = headers, body = RequestBodyFile(body))

/** Interceptor that wraps HTTP requests. Call [proceed] to continue the chain. */
public typealias FetchInterceptor = suspend (
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse,
) -> RequestResponse

/** Interceptors applied to all [fetch] calls, in order. Each wraps the next in the chain. */
public val fetchInterceptors: MutableList<FetchInterceptor> = mutableListOf()

public suspend fun fetch(
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

public expect suspend fun fetchRaw(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: RequestBody? = null,
    onUploadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
    onDownloadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
): RequestResponse

public class ConnectionException(message: String, cause: Exception? = null): Exception(message, cause)

public enum class HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD }

public fun httpHeaders(vararg entries: Pair<String, String>): HttpHeaders = httpHeaders(entries.toList())
public expect fun httpHeaders(map: Map<String, String> = mapOf()): HttpHeaders
public expect fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders
public expect fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders
public expect fun httpHeaders(headers: HttpHeaders): HttpHeaders
public expect class HttpHeaders {
    public fun append(name: String, value: String)
    public fun delete(name: String)
    public fun get(name: String): String?
    public fun has(name: String): Boolean
    public fun set(name: String, value: String)
}

public expect class RequestResponse {
    public val status: Short
    public val ok: Boolean
    public val headers: HttpHeaders
    public suspend fun text(): String
    public suspend fun blob(): Blob
}

public expect class Blob
public expect class FileReference

public expect fun createFileReferenceFromBytes(bytes: ByteArray, mimeType: String, fileName: String): FileReference

public expect fun String.toBlob(contentType: String = "text/plain"): Blob
public expect fun ByteArray.toBlob(contentType: String = "text/plain"): Blob
public expect fun Blob.mimeType(): String
public expect fun Blob.bytes(): Long
public expect suspend fun Blob.toByteArray(): ByteArray
public expect suspend fun Blob.text(): String
public expect fun FileReference.mimeType():String
public expect fun FileReference.bytes():Long
public expect fun FileReference.fileName():String
public expect suspend fun FileReference.text(): String

public sealed interface RequestBody {
    public val type: String
    public val bytes: Long
}
public data class RequestBodyText(val content: String, override val type: String): RequestBody {
    override val bytes: Long get() = content.encodeToByteArray().size.toLong()
}
public data class RequestBodyBlob(val content: Blob): RequestBody {
    override val type: String get() = content.mimeType()
    override val bytes: Long get() = content.bytes()
}
public data class RequestBodyFile(val content: FileReference): RequestBody {
    override val type: String get() = content.mimeType()
    override val bytes: Long get() = content.bytes()
}

public expect fun websocket(url: String): WebSocket

public interface WebSocket {
    public fun close(code: Short, reason: String)
    public fun send(data: String)
    public fun send(data: Blob)
    public fun onOpen(action: ()->Unit)
    public fun onMessage(action: (String)->Unit)
    public fun onBinaryMessage(action: (Blob)->Unit)
    public fun onClose(action: (Short)->Unit)
    public fun cancel() { close(1000, "Closed normally") }
}