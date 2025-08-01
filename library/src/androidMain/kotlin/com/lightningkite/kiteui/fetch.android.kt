@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.lightningkite.kiteui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.signal.AppScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


public val client: HttpClient
    get() {
        return AndroidAppContext.ktorClient
    }

private val fetchLog = ConsoleRoot.tag("fetch")

public actual suspend fun fetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
    onDownloadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
): RequestResponse {
    /**
     * There is currently a bug in android fetch where after a sleep or lock state it will
     * throw a UnknownHostException caused by a android.system.GaiException. To handle this
     * added attempt and delay to wait for android fetch system to be ready
     * https://github.com/square/okhttp/issues/8200
     * https://cs.android.com/android/_/android/platform/frameworks/base/+/0fa9120b8f72916951b2d070afd6c3dfd3c13f77
     **/

    // https://github.com/square/okhttp/issues/8200
    val maxRetries = 5
    var attempt = 0
    while (true) {
        try {
            attempt++
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
                        with(AndroidAppContext.applicationCtx.contentResolver.openInputStream(body.content.uri)) {
                            this?.readBytes()?.let { setBody(it) }
                        }
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
            return RequestResponse(response)
        } catch (e: Exception) {
            fetchLog.log("Attempt $attempt: <X $method $url ${e::class} ${e.message}")
            if (attempt >= maxRetries || e !is UnknownHostException) {
                throw ConnectionException("Network request failed", e)
            }
            fetchLog.log("Retrying after 2 s...")
            delay(2.seconds)
        }
    }
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
        } catch (e: Exception) {
            throw ConnectionException("Reading body failed", e)
        }
    }

    public actual suspend fun blob(): Blob {
        try {
            val result = wraps.body<ByteArray>()
                .let { Blob(it, wraps.contentType()?.toString() ?: "application/octet-stream") }
            return result
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
            } catch(e: Exception) {
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

public actual class FileReference(public val uri: Uri)


public actual fun Blob.mimeType() = type
public actual fun FileReference.mimeType() = when (uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> AndroidAppContext.applicationCtx.contentResolver.getType(uri)
    ContentResolver.SCHEME_FILE ->
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))

    ContentResolver.SCHEME_ANDROID_RESOURCE -> null
    else -> null
} ?: "*/*"

public actual fun FileReference.fileName(): String {
    return AndroidAppContext.applicationCtx.contentResolver
        .query(uri, null, null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
        ?: return "Unknown File Name"
}

public actual class Blob(public val data: ByteArray, public val type: String)

public val webSocketClient: HttpClient by lazy {
    HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000.milliseconds
        }
    }
}

public actual fun Blob.bytes(): Long = data.size.toLong()
public actual fun FileReference.bytes(): Long {
    return AndroidAppContext.applicationCtx.contentResolver
        .query(uri, null, null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(nameIndex)
        }
        ?: return -1L
}

//public actual suspend fun Blob.byteArray(): ByteArray = data
//public actual suspend fun FileReference.byteArray(): ByteArray = withContext(Dispatchers.Main) {
//    withContext(Dispatchers.IO) {
//        AndroidAppContext.applicationCtx.contentResolver.openInputStream(uri)!!.readBytes()
//    }
//}

public actual suspend fun Blob.text(): String = data.toString(Charsets.UTF_8)
public actual suspend fun FileReference.text(): String = withContext(Dispatchers.Main) {
    withContext(Dispatchers.IO) {
        AndroidAppContext.applicationCtx.contentResolver.openInputStream(uri)!!.reader(Charsets.UTF_8).readText()
    }
}

public actual fun String.toBlob(contentType: String): Blob {
    return Blob(toByteArray(Charsets.UTF_8), contentType)
}