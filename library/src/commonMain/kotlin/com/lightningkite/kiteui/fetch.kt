package com.lightningkite.kiteui

import kotlin.time.Duration

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

/**
 * Issues a request through [HttpFetcher.default], applying the global [fetchInterceptors].
 *
 * Prefer taking an [HttpFetcher] and calling [HttpFetcher.fetch] on it. This function can only ever
 * reach the one process-wide chain, so code that calls it cannot be pointed at a fake and cannot be
 * tested against connectivity failures.
 */
public suspend fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: RequestBody? = null,
    onUploadProgress: ProgressCallback? = null,
    onDownloadProgress: ProgressCallback? = null,
): RequestResponse = HttpFetcher.default.fetch(url, method, headers, body, onUploadProgress, onDownloadProgress)

public expect suspend fun fetchRaw(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: RequestBody? = null,
    onUploadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
    onDownloadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
): RequestResponse

/** Any failure to complete an HTTP request. Retry [ConnectionException]; do not retry [RequestBlockedException]. */
public sealed class FetchException(message: String, cause: Exception? = null): Exception(message, cause)

/**
 * The request could not reach the server: offline, DNS failure, connection refused, or timeout.
 * Transient, so [ConnectivityGate] retries these with backoff.
 *
 * Open so that failures which *did* reach a server can refine it. Every subtype is still retryable,
 * which keeps existing `catch (e: ConnectionException)` correct; the subtype only tells
 * [ConnectivityGate] how to schedule the retry.
 *
 * A bare [ConnectionException] means no server was reached, so the fault is local and uncorrelated
 * across clients. Recovery is driven by the platform reporting connectivity again rather than by the
 * backoff timer, and needs no jitter — one device's network returning says nothing about anyone
 * else's.
 */
public open class ConnectionException(message: String, cause: Exception? = null): FetchException(message, cause)

/**
 * A server answered and reported itself unable to serve the request (502, 503).
 *
 * Distinct from a bare [ConnectionException] because the fault is shared: a deploy or a crash fails
 * every client at once, so their retries are synchronized and arrive as one spike on a server that is
 * still coming up. [ConnectivityGate] jitters these to spread the herd, and no platform signal can
 * announce the recovery, so the timer is the only way back.
 */
public class ServerUnavailableException(message: String, cause: Exception? = null): ConnectionException(message, cause)

/**
 * A server answered and asked us to slow down (429, 420).
 *
 * [retryAfter] carries the server's own instruction from the `Retry-After` header when it sent one,
 * which takes precedence over computed backoff — the server knows when it will be ready and we do
 * not. Jittered like [ServerUnavailableException], and for the same reason: a shared limit trips for
 * many clients at once, and `Retry-After` hands them all an identical deadline to pile onto.
 *
 * Unlike the other cases the app is not broken here, merely throttled, so this is worth surfacing
 * without blocking the UI.
 */
public class RateLimitedException(
    message: String,
    public val retryAfter: Duration? = null,
    cause: Exception? = null,
): ConnectionException(message, cause)

/**
 * The platform refused to issue the request on our behalf: a CORS rejection or mixed content in the
 * browser, App Transport Security on iOS, the cleartext policy or a missing INTERNET permission on
 * Android, or a certificate the platform would not accept.
 *
 * Deliberately not a [ConnectionException]. The refusal follows from the app's configuration and the
 * platform's policy rather than from the state of the network, so it will fail identically forever;
 * retrying spends requests to leave the user watching a spinner that can never resolve.
 * [ConnectivityGate] therefore lets it propagate to the caller, who should surface it as the
 * configuration error it is.
 */
public class RequestBlockedException(message: String, cause: Exception? = null): FetchException(message, cause)

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