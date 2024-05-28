package com.lightningkite.kiteui

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import platform.Foundation.*
import platform.Photos.*
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
                            setBody(body.content.data.toByteArray())
                        }

                        is RequestBodyFile -> {
                            when (val file = body.content.file) {
                                is GalleryAssetFile -> {
                                    contentType(ContentType.parse(file.mimeType()))
                                    val fileData = suspendCoroutine {
                                        PHImageManager.defaultManager().requestImageDataAndOrientationForAsset(file.asset, null) {
                                             imageData, _, _, _ ->
                                            it.resume(imageData?.toByteArray() ?: throw Exception("Data is null"))
                                        }
                                    }
                                    setBody(fileData)
                                }
                                is DocumentFile -> {
                                    val contentType = file.defaultType()
                                    contentType(ContentType.parse(contentType.preferredMIMEType ?: "application/octet-stream"))
                                    val fileData = suspendCoroutine {
                                        file.provider.loadDataRepresentationForContentType(contentType) { data, error ->
                                            if (error != null) throw Exception(error.description)
                                            val rawData = data?.toByteArray() ?: throw Exception("Data is null")
                                            it.resume(rawData)
                                        }
                                    }
                                    setBody(fileData)
                                }
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
            throw e
        }
    }

    actual suspend fun blob(): Blob {
        try {
            val result = withContext(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
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
                                    val data = Blob(x.data.toNSData())
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
            } catch (e: Exception) {
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
actual class FileReference(val file: File)

actual object StableFileReferenceSerializer : KSerializer<StableFileReference?> {
    private val photoKitAssetScheme = "photokitasset://"
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StableFileReference", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): StableFileReference? {
        val uri = decoder.decodeString()
        if (uri.isEmpty()) {
            return null
        } else if (uri.startsWith(photoKitAssetScheme)) {
            val asset = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(uri.substringAfter(photoKitAssetScheme)), null)
                .toList()
                .firstOrNull()
            return asset?.let { StableFileReference(FileReference(GalleryAssetFile(it))) }
        } else {
            return StableFileReference(FileReference(DocumentFile(NSURL(string = uri))))
        }
    }

    override fun serialize(encoder: Encoder, value: StableFileReference?) {
        when (val file = value?.wrapped?.file) {
            is GalleryAssetFile -> {
                encoder.encodeString("$photoKitAssetScheme${file.asset.localIdentifier}")
            }
            is DocumentFile -> {
                val absoluteUrl = file.url.absoluteString ?:
                    throw IllegalStateException("Unable to serialize StableFileReference for NSURL that does not resolve to an absolute URL")
                encoder.encodeString(absoluteUrl)
            }
            else -> {
                encoder.encodeString("")
            }
        }
    }
}

actual fun FileReference.mimeType(): String = file.mimeType()
actual fun FileReference.fileName(): String = file.fileName()
actual fun FileReference.stable(): StableFileReference? = StableFileReference(this)

sealed class File {
    abstract fun fileName(): String
    abstract fun mimeType(): String
}

class GalleryAssetFile(val asset: PHAsset) : File() {

    val resource = PHAssetResource.assetResourcesForAsset(asset)
        .filterIsInstance<PHAssetResource>()
        .first { it.type in setOf(PHAssetResourceTypeAudio, PHAssetResourceTypePhoto, PHAssetResourceTypeVideo) }

    override fun fileName(): String = resource.originalFilename

    override fun mimeType(): String = UTType.typeWithIdentifier(resource.uniformTypeIdentifier)?.preferredMIMEType
        ?: "application/octet-stream"
}

@OptIn(ExperimentalForeignApi::class)
fun PHFetchResult.toList(): List<PHAsset> {
    val assets = mutableListOf<PHAsset>()
    enumerateObjectsUsingBlock { asset, _, _ -> (asset as? PHAsset)?.let(assets::add) }
    return assets.toList()
}
class DocumentFile(val url: NSURL) : File() {

    val provider = NSItemProvider(contentsOfURL = url)

    override fun fileName(): String {
        val extension = provider.registeredContentTypes
            .filterIsInstance<UTType>()
            .firstNotNullOfOrNull { it.preferredFilenameExtension } ?: ""
        return "${provider.suggestedName ?: ""}.$extension"
    }

    override fun mimeType(): String = defaultType().preferredMIMEType ?: "application/octet-stream"

    fun defaultType() = provider.registeredContentTypes
        .filterIsInstance<UTType>()
        .firstOrNull() ?: UTTypeData
}

fun String.nsdata(): NSData? =
    NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)

fun NSData.string(): String? =
    NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.toULong()
    )
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
    usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
}