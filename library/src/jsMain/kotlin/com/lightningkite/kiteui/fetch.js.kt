package com.lightningkite.kiteui

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.CloseEvent
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.xhr.BLOB
import org.w3c.xhr.ProgressEvent
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
actual suspend fun fetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
    onDownloadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
): RequestResponse {
    return suspendCoroutineCancellable { cont ->
        val request = XMLHttpRequest()
        onUploadProgress?.let { p ->
            request.upload.addEventListener("progress", { event ->
                event as ProgressEvent
                p(event.loaded.toInt(), event.total.toInt().let { if(it == 0) -1 else it })
            })
        }
        onDownloadProgress?.let { p ->
            request.addEventListener("progress", { event ->
                event as ProgressEvent
                p(event.loaded.toInt(), event.total.toInt().let { if(it == 0) -1 else it })
            })
        }
        request.responseType = XMLHttpRequestResponseType.BLOB
        request.open(method.name, url)
        headers.forEach { key, value -> request.setRequestHeader(key, value) }
        request.onloadend = { ev ->
            if(request.status >= 100)
                cont.resume(RequestResponse(request))
            else
                cont.resumeWithException(ConnectionException("Connection failed"))
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
            else -> throw NotImplementedError()
        }
        return@suspendCoroutineCancellable {
            request.abort()
        }
    }
}

actual inline fun httpHeaders(map: Map<String, String>): HttpHeaders = HttpHeaders().apply {
    for (entry in map) {
        append(entry.key, entry.value)
    }
}

actual inline fun httpHeaders(headers: HttpHeaders): HttpHeaders = HttpHeaders(init = headers)
actual inline fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders = HttpHeaders().apply {
    for (entry in list) {
        append(entry.first, entry.second)
    }
}
actual inline fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders = HttpHeaders().apply {
    for (entry in sequence) {
        append(entry.first, entry.second)
    }
}
actual typealias HttpHeaders = Headers
fun HttpHeaders.forEach(action: (String, String) -> Unit) {
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
actual class RequestResponse(val wraps: XMLHttpRequest) {
    actual val status: Short get() = wraps.status
    actual val ok: Boolean get() = wraps.status / 100 == 2
    actual suspend fun text(): String {
        if(wraps.readyState == XMLHttpRequest.DONE)
            return ((wraps.response as Blob).asDynamic().text() as Promise<String>).await()
        else
            return suspendCoroutineCancellable { cont ->
                val handler: (Event)->Unit = { ev ->
                    launchGlobal {
                        cont.resume(
                            ((wraps.response as Blob).asDynamic().text() as Promise<String>).await()
                        )
                    }
                }
                wraps.addEventListener("loadend", handler)
                return@suspendCoroutineCancellable {
                    wraps.removeEventListener("loadend", handler)
                }
            }
    }
    actual suspend fun blob(): Blob {
        if(wraps.readyState == XMLHttpRequest.DONE)
            return wraps.response as Blob
        else
            return suspendCoroutineCancellable { cont ->
                val handler: (Event)->Unit = { ev ->
                    cont.resume(wraps.response as Blob)
                }
                wraps.addEventListener("loadend", handler)
                return@suspendCoroutineCancellable {
                    wraps.removeEventListener("loadend", handler)
                }
            }
    }
    actual val headers: HttpHeaders by lazy {
        httpHeaders(wraps.getAllResponseHeaders().splitToSequence("\r\n").filter { it.contains(':') }.flatMap {
            val s = it.split(":")
            s[1].trim().splitToSequence(';').map { s[0].trim() to it }
        })
    }
}

actual typealias Blob = org.w3c.files.Blob
actual typealias FileReference = File


actual fun Blob.mimeType(): String {
    return this.type
}
actual fun FileReference.mimeType(): String {
    return this.type
}

actual fun FileReference.fileName(): String {
    return this.name
}

actual fun websocket(url: String): WebSocket {
    return WebSocketWrapper(org.w3c.dom.WebSocket(url))
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
class WebSocketWrapper(val native: org.w3c.dom.WebSocket) : WebSocket {
    override fun close(code: Short, reason: String) = native.close(code, reason)
    override fun send(data: String) = native.send(data)
    override fun send(data: Blob) = native.send(data)
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
}

actual fun Blob.bytes(): Long = size.toLong()
actual fun FileReference.bytes(): Long = size.toLong()

fun jsTextBlob(blob: Blob) = js("blob.text()") as Promise<String>
actual suspend fun Blob.text(): String = jsTextBlob(this).await()
actual suspend fun FileReference.text(): String = jsTextBlob(this).await()
actual fun String.toBlob(contentType: String): Blob = Blob(arrayOf(this), BlobPropertyBag(type = contentType))