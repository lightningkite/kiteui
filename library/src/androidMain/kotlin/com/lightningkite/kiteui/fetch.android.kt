@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.lightningkite.kiteui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.lightningkite.kiteui.views.AndroidAppContext
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File


val client: HttpClient
    get() {
        return AndroidAppContext.ktorClient
    }

actual suspend fun fetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
    onDownloadProgress: ((bytesComplete: Int, bytesExpectedOrNegativeOne: Int) -> Unit)?,
): RequestResponse {
    return withContext(Dispatchers.Main) {
        try {
            val response = withContext(Dispatchers.IO) {
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
                            withContext(Dispatchers.Main) {
                                it(a.toInt(), b.toInt())
                            }
                        }
                    }
                    onDownloadProgress?.let {
                        onDownload { a, b ->
                            withContext(Dispatchers.Main) {
                                it(a.toInt(), b.toInt())
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

actual inline fun httpHeaders(map: Map<String, String>): HttpHeaders =
    HttpHeaders(map.entries.associateTo(HashMap()) { it.key.lowercase() to listOf(it.value) })

actual inline fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders =
    HttpHeaders(sequence.groupBy { it.first.lowercase() }.mapValues { it.value.map { it.second } }.toMutableMap())
actual inline fun httpHeaders(headers: HttpHeaders): HttpHeaders = HttpHeaders(headers.map.toMutableMap())
actual inline fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders =
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
            val result = withContext(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    wraps.bodyAsText()
                }
            }
            return result
        } catch (e: Exception) {
            throw ConnectionException("Reading body failed", e)
        }
    }

    actual suspend fun blob(): Blob {
        try {
            val result = withContext(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    wraps.body<ByteArray>()
                        .let { Blob(it, wraps.contentType()?.toString() ?: "application/octet-stream") }
                }
            }
            return result
        } catch (e: Exception) {
            throw ConnectionException("Reading body failed", e)
        }
    }

    actual val headers: HttpHeaders get() = HttpHeaders(wraps.headers.entries().associateTo(HashMap()) { it.key.lowercase() to it.value })
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
        GlobalScope.launch(Dispatchers.IO) {
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

actual class FileReference(val uri: Uri)

@Serializable(with = StableFileReference.Companion.StableFileReferenceSerializer::class)
actual class StableFileReference private constructor(actual val wrapped: FileReference) {
    actual companion object {
        actual object StableFileReferenceSerializer : KSerializer<StableFileReference?> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StableFileReferenceAndroid", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): StableFileReference? {
                val uri = decoder.decodeString()
                return if (uri.isEmpty()) null
                else StableFileReference(FileReference(Uri.parse(uri)))
            }

            override fun serialize(encoder: Encoder, value: StableFileReference?) {
                encoder.encodeString(value?.wrapped?.uri?.toString() ?: "")
            }
        }

        actual fun FileReference.stableOrNull(): StableFileReference? = StableFileReference(this)
    }
}

actual fun FileReference.mimeType() = when(uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> AndroidAppContext.applicationCtx.contentResolver.getType(uri)
    ContentResolver.SCHEME_FILE ->
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
    ContentResolver.SCHEME_ANDROID_RESOURCE -> null
    else -> null
} ?: "*/*"

actual fun FileReference.fileName(): String {
    return AndroidAppContext.applicationCtx.contentResolver
        .query(uri, null, null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
        ?: return "Unknown File Name"
}

actual class Blob(val data: ByteArray, val type: String)

val webSocketClient: HttpClient by lazy {
    HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }
}
