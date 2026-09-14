package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.CloseEvent
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.fetch.Headers
import org.w3c.fetch.NO_CORS
import org.w3c.fetch.NO_STORE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit
import org.w3c.fetch.RequestMode
import org.w3c.fetch.Response
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FilePropertyBag
import org.w3c.files.File
import org.w3c.xhr.BLOB
import org.w3c.xhr.ProgressEvent
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
public actual suspend fun platformFetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)?,
    onDownloadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)?,
): RequestResponse {
    watchCspViolations()
    return suspendCancellableCoroutine { cont ->
        val request = XMLHttpRequest()
        var cancelled = false
        onUploadProgress?.let { p ->
            request.upload.addEventListener("progress", { event ->
                event as ProgressEvent
                p(event.loaded.toLong(), event.total.toLong().let { if(it == 0L) -1 else it })
            })
        }
        onDownloadProgress?.let { p ->
            request.addEventListener("progress", { event ->
                event as ProgressEvent
                p(event.loaded.toLong(), event.total.toLong().let { if(it == 0L) -1 else it })
            })
        }
        request.responseType = XMLHttpRequestResponseType.BLOB
        request.open(method.name, url)
        headers.forEach { key, value -> request.setRequestHeader(key, value) }
        request.onloadend = { ev ->
            if(request.status >= 100)
                cont.resume(RequestResponse(request))
            else if(!cancelled)
                // Diagnosing costs a round trip, so it happens only once the request has already failed.
                AppScope.launch { cont.resumeWithException(diagnoseFailure(url)) }
        }
        when (body) {
            null -> request.send()
            is RequestBodyBlob -> {
                request.setRequestHeader("Content-Type", body.content.type)
                request.send(body.content)
            }
            is RequestBodyFile -> {
                request.setRequestHeader("Content-Type", body.content.type)
                request.send(body.content)
            }
            is RequestBodyText -> {
                request.setRequestHeader("Content-Type", body.type)
                request.send(body.content)
            }
        }
        cont.invokeOnCancellation {
            cancelled = true
            request.abort()
        }
    }
}

/** How long [serverReachable] waits before concluding that nothing is answering. */
private val reachabilityProbeTimeout = 5.seconds

/** How many recent Content-Security-Policy refusals to remember; only the request in hand is ever matched. */
private const val cspViolationsRemembered = 20

/** Origins the page's Content-Security-Policy has refused to connect to, newest last. */
private val cspRefusedOrigins: MutableList<String> = mutableListOf()
private var watchingCspViolations = false

/**
 * Begins recording `connect-src` refusals, if not already doing so.
 *
 * A Content-Security-Policy refusal is the one platform block a browser announces outright, making
 * it both definitive and free to consult. The event fires before the refused request's own loadend,
 * so the listener has to be in place before the request goes out rather than once it has failed.
 */
private fun watchCspViolations() {
    if (watchingCspViolations) return
    watchingCspViolations = true
    document.addEventListener("securitypolicyviolation", { event ->
        val violation = event.asDynamic()
        if (violation.effectiveDirective == "connect-src") {
            if (cspRefusedOrigins.size >= cspViolationsRemembered) cspRefusedOrigins.removeAt(0)
            cspRefusedOrigins.add(originOf(violation.blockedURI as String))
        }
    })
}

/**
 * Works out why a request failed with no status.
 *
 * A browser reports a CORS rejection and a dead network identically - status 0, no headers, nothing
 * in the exception - by design, so that a page cannot use failures to probe origins it has no access
 * to. The real reason reaches the devtools console and nowhere a script can read it. These checks
 * recover the distinction from the outside instead, cheapest and most certain first.
 */
private suspend fun diagnoseFailure(url: String): FetchException {
    val origin = originOf(url)
    if (origin in cspRefusedOrigins) return RequestBlockedException(
        "This page's Content-Security-Policy does not allow connections to $origin"
    )
    if (mixedContentBlocked(url)) return RequestBlockedException(
        "A page served over https may not request the insecure url $url"
    )
    // Only meaningful when false; `true` merely means an interface exists, not that it carries traffic.
    if (!window.navigator.onLine) return ConnectionException("The device reports that it is offline")
    return if (serverReachable(url)) RequestBlockedException(
        "$origin is reachable but the browser refused the request, which points at a CORS policy that " +
                "does not permit this origin (${window.location.origin}). The devtools console has the specifics."
    ) else ConnectionException("Could not reach $url")
}

/** The scheme and authority of [url], resolved against the page so that relative urls work. */
private fun originOf(url: String): String =
    runCatching { URL(url, window.location.href).origin }.getOrDefault(url)

/**
 * Whether the server answers at all, ignoring whether we are allowed to read what it says.
 *
 * A `no-cors` request is exempt from the CORS check and yields an opaque response, so it completes
 * whenever the server is reachable. It cannot report a status - a 500 resolves just as a 200 does -
 * but reachability is the only question being asked. HEAD with no custom headers also guarantees no
 * preflight, so the probe cannot fail for the same reason the original request did.
 */
private suspend fun serverReachable(url: String): Boolean = withTimeoutOrNull(reachabilityProbeTimeout) {
    try {
        window.fetch(url, RequestInit(method = "HEAD", mode = RequestMode.NO_CORS, cache = RequestCache.NO_STORE)).await()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        false
    }
} ?: false

/**
 * Whether the browser will refuse [url] as mixed content. Worth checking separately because it is
 * decided before anything is sent, so the probe above would report the server as unreachable.
 */
private fun mixedContentBlocked(url: String): Boolean {
    if (window.location.protocol != "https:") return false
    val parsed = runCatching { URL(url, window.location.href) }.getOrNull() ?: return false
    if (parsed.protocol != "http:") return false
    // Loopback counts as trustworthy and is exempt, which keeps local development working.
    val host = parsed.hostname.lowercase()
    return host != "localhost" && !host.endsWith(".localhost") && host != "127.0.0.1" && host != "[::1]" && host != "::1"
}

public actual fun httpHeaders(map: Map<String, String>): HttpHeaders = HttpHeaders().apply {
    for (entry in map) {
        append(entry.key, entry.value)
    }
}

public actual fun httpHeaders(headers: HttpHeaders): HttpHeaders = HttpHeaders(init = headers)
public actual fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders = HttpHeaders().apply {
    for (entry in list) {
        append(entry.first, entry.second)
    }
}
public actual fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders = HttpHeaders().apply {
    for (entry in sequence) {
        append(entry.first, entry.second)
    }
}
public actual typealias HttpHeaders = Headers
public fun HttpHeaders.forEach(action: (String, String) -> Unit) {
    val keys = this.asDynamic().keys()
    var nextKey: dynamic
    do {
        nextKey = keys.next()
        if (nextKey.value != null && nextKey.value != undefined) {
            val nextValue: String = this.asDynamic().get(nextKey.value).unsafeCast<String>()
            action(nextKey.value, nextValue)
        }
    } while (!nextKey.done)
}

//actual class RequestResponse(val wraps: Response) {
//    actual val status: Short get() = wraps.status
//    actual val ok: Boolean get() = wraps.ok
//    actual suspend fun text(): String = wraps.text().await()
//    actual suspend fun blob(): Blob = wraps.blob().await()
//    actual val headers: HttpHeaders get() = wraps.headers
//}
public actual class RequestResponse(public val wraps: XMLHttpRequest) {
    public actual val status: Short get() = wraps.status
    public actual val ok: Boolean get() = wraps.status / 100 == 2
    public actual suspend fun text(): String {
        if(wraps.readyState == XMLHttpRequest.DONE)
            return ((wraps.response as Blob).asDynamic().text() as Promise<String>).await()
        else
            return suspendCancellableCoroutine { cont ->
                val handler: (Event)->Unit = { ev ->
                    AppScope.launch {
                        cont.resume(
                            ((wraps.response as Blob).asDynamic().text() as Promise<String>).await()
                        )
                    }
                }
                wraps.addEventListener("loadend", handler)
                cont.invokeOnCancellation {
                    wraps.removeEventListener("loadend", handler)
                }
            }
    }
    public actual suspend fun blob(): Blob {
        if(wraps.readyState == XMLHttpRequest.DONE)
            return wraps.response as Blob
        else
            return suspendCancellableCoroutine { cont ->
                val handler: (Event)->Unit = { ev ->
                    cont.resume(wraps.response as Blob)
                }
                wraps.addEventListener("loadend", handler)
                cont.invokeOnCancellation {
                    wraps.removeEventListener("loadend", handler)
                }
            }
    }
    public actual val headers: HttpHeaders by lazy {
        httpHeaders(wraps.getAllResponseHeaders().splitToSequence("\r\n").filter { it.contains(':') }.map {
            val s = it.split(":", limit = 2)
            s[0].trim() to s[1].trim()
        })
    }
}

public actual typealias Blob = org.w3c.files.Blob
public actual typealias FileReference = File

public actual fun createFileReferenceFromBytes(bytes: ByteArray, mimeType: String, fileName: String): FileReference {
    // ByteArray in Kotlin/JS is backed by Int8Array; wrap in a Blob first, then File
    val blob = Blob(arrayOf(bytes.asDynamic()), BlobPropertyBag(type = mimeType))
    return File(arrayOf(blob), fileName, FilePropertyBag(type = mimeType))
}

public actual fun Blob.mimeType(): String {
    return this.type
}
public actual fun FileReference.mimeType(): String {
    return this.type
}

public actual fun FileReference.fileName(): String {
    return this.name
}

private val killAllSockets = BasicListenable().also {
    window.asDynamic().killAllSockets = { ->
        println("Killing all sockets")
        it.invokeAll()
    }
}
public actual fun platformWebSocket(url: String): WebSocket {
    return WebSocketWrapper(org.w3c.dom.WebSocket(url))
}

public class WebSocketWrapper(public val native: org.w3c.dom.WebSocket, public val log: Log? = Log.tag("WS to ${native.url}").infoOrAbove()) : WebSocket {
    private val opened = Clock.System.now()
    private val stopListeningToDebugKill = killAllSockets.addListener {
        println("Killing webSocket to ${native.url} opened at $opened")
        native.close(3008)
    }
    override fun close(code: Short, reason: String): Unit = native.close(code, reason)
    override fun send(data: String): Unit = native.send(data)
    override fun send(data: Blob): Unit = native.send(data)
    override fun onOpen(action: () -> Unit) {
        native.addEventListener("open", { action() })
    }

    override fun onMessage(action: (String) -> Unit) {
        native.addEventListener("message", { it as MessageEvent; (it.data as? String)?.let { action(it) } })
    }

    override fun onBinaryMessage(action: (Blob) -> Unit) {
        native.addEventListener("message", { it as MessageEvent; (it.data as? Blob)?.let { action(it) } })
    }

    override fun onClose(action: (Short) -> Unit) {
        native.addEventListener("close", { action((it as CloseEvent).code) })
    }

    init {
        onClose { stopListeningToDebugKill() }
        log?.let { log ->
            onOpen { log.info("Opened.") }
            onMessage { log.log("onMessage $it") }
            onBinaryMessage { log.log("onBinaryMessage $it") }
            onClose { log.info("Closed with code $it.") }
        }
    }
}

public actual fun Blob.bytes(): Long = size.toLong()
public actual fun FileReference.bytes(): Long = size.toLong()

public fun jsTextBlob(blob: Blob): Promise<String> = js("blob.text()") as Promise<String>
public actual suspend fun Blob.text(): String = jsTextBlob(this).await()
public actual suspend fun FileReference.text(): String = jsTextBlob(this).await()
public actual fun String.toBlob(contentType: String): Blob = Blob(arrayOf(this), BlobPropertyBag(type = contentType))
public actual fun ByteArray.toBlob(contentType: String): Blob = Blob(arrayOf(this), options = BlobPropertyBag(type = contentType))
public actual suspend fun Blob.toByteArray(): ByteArray = Int8Array((asDynamic().arrayBuffer() as Promise<ArrayBuffer>).await()).toByteArray()

    /** Returns a new [ByteArray] containing all the elements of this [Int8Array]. */
private fun Int8Array.toByteArray(): ByteArray =
    ByteArray(this.length) { this[it] }

/** Returns a new [Int8Array] containing all the elements of this [ByteArray]. */
private fun ByteArray.toInt8Array(): Int8Array {
    val result = Int8Array(this.size)
    for (index in this.indices) {
        result[index] = this[index]
    }
    return result
}