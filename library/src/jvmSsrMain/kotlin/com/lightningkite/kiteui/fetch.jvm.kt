@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import java.io.File
import java.net.UnknownServiceException
import java.nio.file.Files
import javax.net.ssl.SSLException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


public val client: HttpClient by lazy { webSocketClient }

private val fetchLog = LogRoot.tag("fetch")

public actual suspend fun platformFetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)?,
    onDownloadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)?,
): RequestResponse {
    try {
        fetchLog.log("-> $method $url")
        val response = client.request(url) {
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
                    setBody(body.content.data)
                }

                is RequestBodyFile -> {
                    contentType(ContentType.parse(body.content.mimeType()))
                    setBody(body.content.file.readBytes())
                }

                is RequestBodyText -> {
                    contentType(ContentType.parse(body.type))
                    setBody(body.content)
                }

                null -> {}
            }
            onUploadProgress?.let {
                onUpload { a, b ->
                    it(a, b ?: -1)
                }
            }
            onDownloadProgress?.let {
                onDownload { a, b ->
                    it(a, b ?: -1)
                }
            }
        }
        fetchLog.log("<- $method $url ${response.status}")
        return RequestResponse(response)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        fetchLog.log("<X $method $url ${e::class} ${e.message}")
        throw classifyFetchFailure(e)
    }
}

/**
 * Sorts a failed request into one the network might yet satisfy and one the JVM has decided against.
 *
 * Ktor wraps engine failures at varying depths, so the whole cause chain is considered.
 */
private fun classifyFetchFailure(e: Exception): FetchException {
    val blocked = generateSequence<Throwable>(e) { it.cause }.take(10).firstOrNull {
        when (it) {
            // Raised when the client is configured to forbid the scheme, cleartext http being the usual case.
            is UnknownServiceException -> true
            // A rejected or pinned certificate. Grouped here because no amount of retrying changes the
            // verdict, even though the cause is often a server whose certificate needs attention.
            is SSLException -> true
            else -> false
        }
    }
    return if (blocked != null) RequestBlockedException(
        "The request was refused: ${blocked::class.simpleName}: ${blocked.message}", e
    ) else ConnectionException("Network request failed", e)
}

public actual fun httpHeaders(map: Map<String, String>): HttpHeaders =
    HttpHeaders(map.entries.associateTo(HashMap()) { it.key.lowercase() to listOf(it.value) })

public actual fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders =
    HttpHeaders(sequence.groupBy { it.first.lowercase() }.mapValues { it.value.map { it.second } }.toMutableMap())

public actual fun httpHeaders(headers: HttpHeaders): HttpHeaders = HttpHeaders(headers.map.toMutableMap())
public actual fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders =
    HttpHeaders(list.groupBy { it.first.lowercase() }.mapValues { it.value.map { it.second } }.toMutableMap())

public actual class HttpHeaders(public val map: MutableMap<String, List<String>>) {
    public actual fun append(name: String, value: String): Unit {
        map[name.lowercase()] = (map[name.lowercase()] ?: listOf()) + value
    }

    public actual fun delete(name: String): Unit {
        map.remove(name.lowercase())
    }

    public actual fun get(name: String): String? = map[name.lowercase()]?.joinToString(",")
    public actual fun has(name: String): Boolean = map.containsKey(name.lowercase())
    public actual fun set(name: String, value: String): Unit {
        map[name.lowercase()] = listOf(value)
    }
}

public actual class RequestResponse(public val wraps: HttpResponse) {
    public actual val status: Short get() = wraps.status.value.toShort()
    public actual val ok: Boolean get() = wraps.status.isSuccess()
    public actual suspend fun text(): String {
        try {
            val result = wraps.bodyAsText()
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ConnectionException("Reading body failed", e)
        }
    }

    public actual suspend fun blob(): Blob {
        try {
            val result = wraps.body<ByteArray>()
                .let { Blob(it, wraps.contentType()?.toString() ?: "application/octet-stream") }
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ConnectionException("Reading body failed", e)
        }
    }

    public actual val headers: HttpHeaders
        get() = HttpHeaders(
            wraps.headers.entries().associateTo(HashMap()) { it.key.lowercase() to it.value })
}

public actual fun platformWebSocket(url: String): WebSocket {
    return WebSocketWrapper(url)
}

public class WebSocketWrapper(public val url: String) : WebSocket {
    public val closeReason: Channel<CloseReason> = Channel<CloseReason>()
    public val sending: Channel<Frame> = Channel<Frame>(10)
    public var stayOn: Boolean = true
    public val onOpen: MutableList<() -> Unit> = ArrayList<() -> Unit>()

    init {
        onOpen.add { assertMainThread() }
    }

    public val onClose: MutableList<(Short) -> Unit> = ArrayList<(Short) -> Unit>()

    init {
        onClose.add { assertMainThread() }
    }

    public val onMessage: MutableList<(String) -> Unit> = ArrayList<(String) -> Unit>()

    init {
        onMessage.add { assertMainThread() }
    }

    public val onBinaryMessage: MutableList<(Blob) -> Unit> = ArrayList<(Blob) -> Unit>()

    init {
        onBinaryMessage.add { assertMainThread() }
    }

    init {
        @Suppress("OPT_IN_USAGE")
        AppScope.launch(Dispatchers.IO) {
            try {
                client.webSocket(url) {
                    // by Claude — Bug 1: flag to ensure onClose fires exactly once
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
                                    val data = Blob(x.data, "application/octet-stream")
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
                            break // by Claude — channel closed, exit loop
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
            } catch (e: Exception) {
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
        sending.trySend(Frame.Binary(false, data.data))
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

public actual class FileReference(public val file: File)

// by Claude - create FileReference from raw bytes for testing/mocking
public actual fun createFileReferenceFromBytes(bytes: ByteArray, mimeType: String, fileName: String): FileReference {
    // by Claude - use a subdirectory so the original fileName is preserved for fileName()
    val dir = File(System.getProperty("java.io.tmpdir"), "kiteui-mock-${System.nanoTime()}")
    dir.mkdirs()
    dir.deleteOnExit()
    val tempFile = File(dir, fileName)
    tempFile.deleteOnExit()
    tempFile.writeBytes(bytes)
    return FileReference(tempFile)
}

public actual fun Blob.mimeType(): String = type
public actual fun FileReference.mimeType(): String = Files.probeContentType(file.toPath()) ?: "application/octet-stream"

public actual fun FileReference.fileName(): String = file.toString().substringAfterLast('/')
public actual class Blob(public val data: ByteArray, public val type: String)

public val webSocketClient: HttpClient by lazy {
    HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
        install(WebSockets) {
            pingInterval = 20_000.milliseconds
        }
    }
}

public actual fun Blob.bytes(): Long = data.size.toLong()
public actual fun FileReference.bytes(): Long = file.length()
//actual suspend fun Blob.byteArray(): ByteArray = data
//actual suspend fun FileReference.byteArray(): ByteArray = withContext(Dispatchers.Main) {
//    withContext(Dispatchers.IO) {
//        AndroidAppContext.applicationCtx.contentResolver.openInputStream(uri)!!.readBytes()
//    }
//}

public actual suspend fun Blob.text(): String = data.toString(Charsets.UTF_8)
public actual suspend fun FileReference.text(): String = file.readText()

public actual fun String.toBlob(contentType: String): Blob = toByteArray(Charsets.UTF_8).toBlob(contentType)
public actual fun ByteArray.toBlob(contentType: String): Blob = Blob(this, contentType)

public actual suspend fun Blob.toByteArray(): ByteArray = data

