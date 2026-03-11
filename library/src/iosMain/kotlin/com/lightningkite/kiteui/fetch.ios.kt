

package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import platform.Foundation.*
import platform.UniformTypeIdentifiers.*
import platform.posix.memcpy
import kotlin.coroutines.resumeWithException

// by Claude
private val fetchLog = LogRoot.tag("fetch")

val client = HttpClient {
    install(WebSockets)
    install(UserAgent) {
        agent = Platform.userAgent
    }
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
    onUploadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)?,
    onDownloadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)?,
): RequestResponse {
    return run {
        try {
            fetchLog.log("-> $method $url")
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
                            // 1. Resolve the UTType just like before
                            val type = body.content.suggestedType
                                ?: (body.content.provider.registeredContentTypes.firstOrNull() as? UTType)
                                ?: UTTypeData

                            // 2. FIX: Fallback to octet-stream instead of throwing an Exception
                            val mimeString = type.preferredMIMEType ?: "application/octet-stream"
                            contentType(ContentType.parse(mimeString))

                            val fileData = suspendCoroutine { continuation ->
                                body.content.provider.loadDataRepresentationForContentType(type) { data, error ->
                                    if (error != null) {
                                        // If the specific type fails, sometimes falling back to UTTypeData works
                                        if (type != UTTypeData) {
                                            body.content.provider.loadDataRepresentationForContentType(UTTypeData) { data2, error2 ->
                                                if(error2 != null) {
                                                    continuation.resumeWithException(Exception(error2.description))
                                                } else {
                                                    val rawData = data2?.toByteArray() ?: throw Exception("Data is null")
                                                    continuation.resume(rawData)
                                                }
                                            }
                                        } else {
                                            continuation.resumeWithException(Exception(error.description))
                                        }
                                    } else {
                                        val rawData = data?.toByteArray() ?: throw Exception("Data is null")
                                        continuation.resume(rawData)
                                    }
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
                                it(a, b ?: -1)
                            }
                        }
                    }
                    onDownloadProgress?.let {
                        onDownload { a, b ->
                            run {
                                it(a, b ?: -1)
                            }
                        }
                    }
                }
            }

            fetchLog.log("<- $method $url ${response.status}")
            RequestResponse(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fetchLog.log("<X $method $url ${e::class.simpleName}: ${e.message}")
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
            return wraps.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ConnectionException("Reading body failed", e)
        }
    }

    actual suspend fun blob(): Blob {
        try {
            return wraps.body<ByteArray>()
                .let { Blob(it.toNSData(), wraps.contentType()?.toString() ?: "application/octet-stream") }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ConnectionException("Reading body failed", e)
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
                    var onCloseFired = false
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
                                    if (!onCloseFired) {
                                        onCloseFired = true
                                        onClose.forEach { it(reason.code) }
                                    }
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
                            break
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (!onCloseFired) {
                            onCloseFired = true
                            onClose.forEach { it(reason?.code ?: 0) }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch(e: Exception) {
                fetchLog.log("WebSocket connection failed: ${e::class.simpleName}: ${e.message}")
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

@OptIn(ExperimentalForeignApi::class)
actual fun createFileReferenceFromBytes(bytes: ByteArray, mimeType: String, fileName: String): FileReference {
    val nsData = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
    }
    val utType = UTType.typeWithMIMEType(mimeType) ?: UTTypeData
    val provider = NSItemProvider(item = nsData, typeIdentifier = utType.identifier)
    provider.suggestedName = fileName
    return FileReference(provider, utType)
}


actual fun Blob.mimeType(): String = type
actual fun FileReference.mimeType(): String {
    // 1. Try the explicit suggested type
    // 2. Fallback to the first registered type in the provider
    // 3. Fallback to generic Data type
    val type = suggestedType
        ?: (provider.registeredContentTypes.firstOrNull() as? UTType)
        ?: UTTypeData

    return type.preferredMIMEType ?: "application/octet-stream"
}
actual fun FileReference.fileName(): String {
    val type = suggestedType
        ?: (provider.registeredContentTypes.firstOrNull() as? UTType)
        ?: UTTypeData

    val extension = type.preferredFilenameExtension ?: ""

    // 1. Try the system suggested name
    // 2. If null, create a unique one using a timestamp
    val name = provider.suggestedName ?: run {
        val timestamp = NSDate().timeIntervalSince1970.toLong()
        "file_$timestamp"
    }

    return if (extension.isNotBlank()) {
        "$name.$extension"
    } else {
        name
    }
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
actual suspend fun Blob.toByteArray(): ByteArray = this.data.toByteArray()
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