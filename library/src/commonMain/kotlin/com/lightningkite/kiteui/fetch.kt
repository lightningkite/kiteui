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
expect suspend fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: RequestBody? = null,
    onUploadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)? = null,
    onDownloadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)? = null,
): RequestResponse

class ConnectionException(message: String, cause: Exception? = null): Exception(message, cause)

enum class HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD }

fun httpHeaders(vararg entries: Pair<String, String>) = httpHeaders(entries.toList())
expect inline fun httpHeaders(map: Map<String, String> = mapOf()): HttpHeaders
expect inline fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders
expect inline fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders
expect inline fun httpHeaders(headers: HttpHeaders): HttpHeaders
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

expect fun Blob.mimeType(): String
expect fun Blob.bytes(): Long
expect fun FileReference.mimeType():String
expect fun FileReference.bytes():Long
expect fun FileReference.fileName():String

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

interface WebSocket: Cancellable {
    fun close(code: Short, reason: String)
    fun send(data: String)
    fun send(data: Blob)
    fun onOpen(action: ()->Unit)
    fun onMessage(action: (String)->Unit)
    fun onBinaryMessage(action: (Blob)->Unit)
    fun onClose(action: (Short)->Unit)
    override fun cancel() { close(1000, "Closed normally") }
}

/*

retry {
    val ws = websocket(url)
    ws.send("asdf")
    val msg = ws.incoming.receive()
    ws.close()
    while(true) {
        ws.receive()
    }
}
 */