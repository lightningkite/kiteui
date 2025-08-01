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
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


public val client: HttpClient by lazy { webSocketClient }

private val fetchLog = LogRoot.tag("fetch")

public actual suspend fun fetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
    onDownloadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
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
                    it(a.toInt(), b?.toInt() ?: -1)
                }
            }
            onDownloadProgress?.let {
                onDownload { a, b ->
                    it(a.toInt(), b?.toInt() ?: -1)
                }
            }
        }
        fetchLog.log("<- $method $url ${response.status}")
        return RequestResponse(response)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        fetchLog.log("<X $method $url ${e::class} ${e.message}")
        throw ConnectionException("Network request failed", e)
    }
}

public actual fun httpHeaders(map: Map<String, String>): HttpHeaders =
    HttpHeaders(map.entries.associateTo(HashMap()) { it.key.lowercase() to listOf(it.value) })

public actual fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders =
    HttpHeaders(sequence.groupBy { it.first.lowercase() }.mapValues { it.value.map { it.second } }.toMutableMap())

public actual fun httpHeaders(headers: HttpHeaders): HttpHeaders = HttpHeaders(headers.map.toMutableMap())
public actual fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders =
    HttpHeaders(list.groupBy { it.first.lowercase() }.mapValues { it.value.map { it.second } }.toMutableMap())

@InternalKiteUi
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

public actual fun websocket(url: String): WebSocket {
    return WebSocketWrapper(url)
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
public class WebSocketWrapper(public val url: String) : WebSocket {
    public val closeReason: Channel<CloseReason> = Channel<CloseReason>()
    public val sending: Channel<Frame> = Channel<Frame>(10)
    public var stayOn: Boolean = true
    public val onOpen: ArrayList<() -> Unit> = ArrayList<() -> Unit>()

    init {
        onOpen.add { assertMainThread() }
    }

    public val onClose: ArrayList<(Short) -> Unit> = ArrayList<(Short) -> Unit>()

    init {
        onClose.add { assertMainThread() }
    }

    public val onMessage: ArrayList<(String) -> Unit> = ArrayList<(String) -> Unit>()

    init {
        onMessage.add { assertMainThread() }
    }

    public val onBinaryMessage: ArrayList<(Blob) -> Unit> = ArrayList<(Blob) -> Unit>()

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
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onClose.forEach { it(reason?.code ?: 0) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onClose.forEach { it(0) }
                }
            }
        }
    }

    public override fun close(code: Short, reason: String) {
        stayOn = false
        closeReason.trySend(CloseReason(code, reason))
    }

    public override fun send(data: String) {
        sending.trySend(Frame.Text(data))
    }

    public override fun send(data: Blob) {
        sending.trySend(Frame.Binary(false, data.data))
    }

    public override fun onOpen(action: () -> Unit) {
        onOpen.add(action)
    }

    public override fun onMessage(action: (String) -> Unit) {
        onMessage.add(action)
    }

    public override fun onBinaryMessage(action: (Blob) -> Unit) {
        onBinaryMessage.add(action)
    }

    public override fun onClose(action: (Short) -> Unit) {
        onClose.add(action)
    }
}

@InternalKiteUi
public actual class FileReference(public val file: File)


public actual fun Blob.mimeType() = type
public actual fun FileReference.mimeType() = Files.probeContentType(file.toPath()) ?: "application/octet-stream"

public actual fun FileReference.fileName(): String = file.toString().substringAfterLast('/')
@InternalKiteUi
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
//public actual suspend fun Blob.byteArray(): ByteArray = data
//public actual suspend fun FileReference.byteArray(): ByteArray = withContext(Dispatchers.Main) {
//    withContext(Dispatchers.IO) {
//        AndroidAppContext.applicationCtx.contentResolver.openInputStream(uri)!!.readBytes()
//    }
//}

public actual suspend fun Blob.text(): String = data.toString(Charsets.UTF_8)
public actual suspend fun FileReference.text(): String = file.readText()

public actual fun String.toBlob(contentType: String): Blob {
    return Blob(toByteArray(Charsets.UTF_8), contentType)
}

actual suspend fun Blob.toByteArray(): ByteArray = data
