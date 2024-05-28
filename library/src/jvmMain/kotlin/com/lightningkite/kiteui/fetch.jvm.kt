package com.lightningkite.kiteui

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.nio.file.Files

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
actual suspend fun fetch(
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    onUploadProgress: ((Int, Int) -> Unit)?,
    onDownloadProgress: ((Int, Int) -> Unit)?,
): RequestResponse = TODO()
actual inline fun httpHeaders(map: Map<String, String>): HttpHeaders = TODO()
actual inline fun httpHeaders(headers: HttpHeaders): HttpHeaders =  TODO()
actual inline fun httpHeaders(list: List<Pair<String, String>>): HttpHeaders = TODO()
actual inline fun httpHeaders(sequence: Sequence<Pair<String, String>>): HttpHeaders = TODO()
actual class HttpHeaders {
    actual fun append(name: String, value: String): Unit = TODO()
    actual fun delete(name: String): Unit = TODO()
    actual fun get(name: String): String?= TODO()
    actual fun has(name: String): Boolean = TODO()

    actual fun set(name: String, value: String): Unit = TODO()
}
actual class RequestResponse {
    actual val status: Short get() = TODO()
    actual val ok: Boolean get() = TODO()
    actual suspend fun text(): String = TODO()
    actual suspend fun blob(): Blob = TODO()
    actual val headers: HttpHeaders = TODO()
}

actual class Blob(val file: File, val type: String) {

}
actual typealias FileReference = File

@Serializable(with = StableFileReference.Companion.StableFileReferenceSerializer::class)
actual class StableFileReference private constructor(actual val wrapped: FileReference) {
    actual companion object {
        actual object StableFileReferenceSerializer : KSerializer<StableFileReference?> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StableFileReferenceJVM", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): StableFileReference? {
                val uri = decoder.decodeString()
                return if (uri.isEmpty()) null
                else StableFileReference(File(uri))
            }

            override fun serialize(encoder: Encoder, value: StableFileReference?) {
                encoder.encodeString(value?.wrapped?.path ?: "")
            }
        }

        actual fun FileReference.stableOrNull(): StableFileReference? = StableFileReference(this)
    }
}

actual fun FileReference.mimeType(): String {
    return Files.probeContentType(this.toPath())
}
actual fun FileReference.fileName(): String {
    return this.name
}

actual fun websocket(url: String): WebSocket = TODO()