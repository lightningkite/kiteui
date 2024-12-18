

package com.lightningkite.kiteui

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cache.storage.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import platform.Foundation.*
import platform.UniformTypeIdentifiers.*
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val client = HttpClient {
    install(WebSockets)
    install(HttpCache) {
//        publicStorage(object: CacheStorage {
//            override suspend fun find(url: Url, varyKeys: Map<String, String>): CachedResponseData? {
//                TODO("Not yet implemented")
//            }
//
//            override suspend fun findAll(url: Url): Set<CachedResponseData> {
//                TODO("Not yet implemented")
//            }
//
//            override suspend fun store(url: Url, data: CachedResponseData) {
//                TODO("Not yet implemented")
//            }
//        })
    }
}

actual suspend fun fetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
    onDownloadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
): RequestResponse {
    return run {
        try {
            val response = run {
                client.request(url) {
                    this.method = when (method) {
                        HttpMethod.GET -> io.ktor.http.HttpMethod.Get
                        HttpMethod.POST -> io.ktor.http.HttpMethod.Post
                        HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
                        HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
                        HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
                        HttpMethod.HEAD -> io.ktor.http.HttpMethod.Head
                    }
                    headers { headers.map.forEach { it.value.forEach { v -> append(it.key, v) } } }
                    when (body) {
                        is RequestBodyBlob -> {
                            contentType(ContentType.parse(body.content.type))
                            setBody(body.content.data.toByteArray())
                        }

                        is RequestBodyFile -> {
                            val mime = body.content.suggestedType
                                ?: (body.content.provider.registeredContentTypes.firstOrNull() as? UTType ?: UTTypeData)
                            // Type is dyn.age8u (null)
                            contentType(ContentType.parse(mime.preferredMIMEType ?: throw Exception("No mime type found from file")))
                            val fileData = suspendCoroutine {
                                body.content.provider.loadDataRepresentationForContentType(mime) { data, error ->
                                    if (error != null) throw Exception(error.description)
                                    val rawData = data?.toByteArray() ?: throw Exception("Data is null")
                                    it.resume(rawData)
                                }
                            }
                            setBody(fileData)
                        }

                        is RequestBodyText -> {
                            contentType(ContentType.parse(body.type))
                            setBody(body.content)
                        }

                        null -> {}
                    }
                    onUploadProgress?.let {
                        onUpload { a, b ->
                            run {
                                it(a.toInt(), b?.toInt() ?: -1)
                            }
                        }
                    }
                    onDownloadProgress?.let {
                        onDownload { a, b ->
                            run {
                                it(a.toInt(), b?.toInt() ?: -1)
                            }
                        }
                    }
                }
            }

            RequestResponse(response)
        } catch (e: Exception) {
            throw ConnectionException("Network request failed", e)
        }
    }
}

actual fun httpHeaders(map: Map<String, String>): HttpHeaders =
    HttpHeaders(map.entries.associateTo(HashMap()) { it.key.lowercase() to listOf(it.value) })

actual fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders =
    HttpHeaders(sequence.groupBy { it.first.lowercase() }.mapValues { it.value.map { it.second } }.toMutableMap())

actual fun httpHeaders(headers: HttpHeaders): HttpHeaders = HttpHeaders(headers.map.toMutableMap())
actual fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders =
    HttpHeaders(list.groupBy { it.first.lowercase() }.mapValues { it.value.map { it.second } }.toMutableMap())

actual class HttpHeaders(val map: MutableMap<String, List<String>>) {
    actual fun append(name: String, value: String): Unit {
        map[name.lowercase()] = (map[name.lowercase()] ?: listOf()) + value
    }

    actual fun delete(name: String): Unit {
        map.remove(name.lowercase())
    }

    actual fun get(name: String): String? = map[name.lowercase()]?.joinToString(",")
    actual fun has(name: String): Boolean = map.containsKey(name.lowercase())
    actual fun set(name: String, value: String): Unit {
        map[name.lowercase()] = listOf(value)
    }
}

actual class RequestResponse(val wraps: HttpResponse) {
    actual val status: Short get() = wraps.status.value.toShort()
    actual val ok: Boolean get() = wraps.status.isSuccess()
    actual suspend fun text(): String {
        try {
            val result = run {
                run {
                    wraps.bodyAsText()
                }
            }
            return result
        } catch (e: Exception) {
            throw e
        }
    }

    actual suspend fun blob(): Blob {
        try {
            val result = run {
                run {
                    wraps.body<ByteArray>()
                        .let { Blob(it.toNSData(), wraps.contentType()?.toString() ?: "application/octet-stream") }
                }
            }
            return result
        } catch (e: Exception) {
            throw e
        }
    }

    actual val headers: HttpHeaders
        get() = HttpHeaders(
            wraps.headers.entries().associateTo(HashMap()) { it.key.lowercase() to it.value })
}

actual fun websocket(url: String): WebSocket {
    return WebSocketWrapper(url)
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
class WebSocketWrapper(val url: String) : WebSocket {
    val closeReason = Channel<CloseReason>()
    val sending = Channel<Frame>(10)
    var stayOn = true
    val onOpen = ArrayList<() -> Unit>()

    init {
        onOpen.add { assertMainThread() }
    }

    val onClose = ArrayList<(Short) -> Unit>()

    init {
        onClose.add { assertMainThread() }
    }

    val onMessage = ArrayList<(String) -> Unit>()

    init {
        onMessage.add { assertMainThread() }
    }

    val onBinaryMessage = ArrayList<(Blob) -> Unit>()

    init {
        onBinaryMessage.add { assertMainThread() }
    }

    init {
        @Suppress("OPT_IN_USAGE")
        AppScope.launch(Dispatchers.IO) {
            try {
                client.webSocket(url) {
                    withContext(Dispatchers.Main) {
                        onOpen.forEach { it() }
                    }
                    launch {
                        try {
                            while (stayOn) {
                                send(sending.receive())
                            }
                        } catch (e: ClosedReceiveChannelException) {
                        }
                    }
                    launch {
                        try {
                            this@WebSocketWrapper.closeReason.receive().let { reason ->
                                close(reason)
                                withContext(Dispatchers.Main) {
                                    onClose.forEach { it(reason.code) }
                                }
                            }
                        } catch (e: ClosedReceiveChannelException) {
                        }
                    }
                    var reason: CloseReason? = null
                    while (stayOn) {
                        try {
                            when (val x = incoming.receive()) {
                                is Frame.Binary -> {
                                    val data = Blob(x.data.toNSData(), "application/octet-stream")
                                    withContext(Dispatchers.Main) {
                                        onBinaryMessage.forEach { it(data) }
                                    }
                                }

                                is Frame.Text -> {
                                    val text = x.readText()
                                    withContext(Dispatchers.Main) {
                                        onMessage.forEach { it(text) }
                                    }
                                }

                                is Frame.Close -> {
                                    reason = x.readReason()
                                    break
                                }

                                else -> {}
                            }
                        } catch (e: ClosedReceiveChannelException) {
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onClose.forEach { it(reason?.code ?: 0) }
                    }
                }
            } catch(e: Exception) {
                withContext(Dispatchers.Main) {
                    onClose.forEach { it(0) }
                }
            }
        }
    }

    override fun close(code: Short, reason: String) {
        stayOn = false
        closeReason.trySend(CloseReason(code, reason))
    }

    override fun send(data: String) {
        sending.trySend(Frame.Text(data))
    }

    override fun send(data: Blob) {
        sending.trySend(Frame.Binary(false, data.data.toByteArray()))
    }

    override fun onOpen(action: () -> Unit) {
        onOpen.add(action)
    }

    override fun onMessage(action: (String) -> Unit) {
        onMessage.add(action)
    }

    override fun onBinaryMessage(action: (Blob) -> Unit) {
        onBinaryMessage.add(action)
    }

    override fun onClose(action: (Short) -> Unit) {
        onClose.add(action)
    }
}

actual class Blob(val data: NSData, val type: String = "application/octet-stream")
actual class FileReference(val provider: NSItemProvider, val suggestedType: UTType? = null)


actual fun Blob.mimeType(): String = type
actual fun FileReference.mimeType(): String = suggestedType?.preferredMIMEType ?: "application/octet-stream"

actual fun FileReference.fileName(): String {
    val extension = suggestedType?.preferredFilenameExtension ?: ""
    return "${provider.suggestedName ?: ""}.$extension"
}

fun String.nsdata(): NSData? =
    NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)

fun NSData.string(): String? =
    NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()


fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.toULong()
    )
}


fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
    usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
}

actual fun Blob.bytes(): Long = this.data.length.toLong()
actual fun FileReference.bytes(): Long = -1L

//actual suspend fun Blob.byteArray(): ByteArray = data.toByteArray()
//actual suspend fun FileReference.byteArray(): ByteArray {
//    val mime = suggestedType
//        ?: (provider.registeredContentTypes.firstOrNull() as? UTType ?: UTTypeData)
//    // Type is dyn.age8u (null)
//    return suspendCoroutine {
//        provider.loadDataRepresentationForContentType(mime) { data, error ->
//            if (error != null) throw Exception(error.description)
//            val rawData = data?.toByteArray() ?: throw Exception("Data is null")
//            it.resume(rawData)
//        }
//    }
//}

actual suspend fun Blob.text(): String = data.string()!!
actual suspend fun FileReference.text(): String {
    val mime = suggestedType
        ?: (provider.registeredContentTypes.firstOrNull() as? UTType ?: UTTypeData)
    // Type is dyn.age8u (null)
    return suspendCoroutine {
        provider.loadDataRepresentationForContentType(mime) { data, error ->
            if (error != null) throw Exception(error.description)
            val rawData = data?.string() ?: throw Exception("Data is null")
            it.resume(rawData)
        }
    }
}

actual fun String.toBlob(contentType: String): Blob = Blob(this.nsdata()!!, contentType)