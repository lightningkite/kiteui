package com.lightningkite.kiteui

import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

private suspend fun ByteArray.digest(type: String): ByteArray {
    val p: Promise<ArrayBuffer> = window.asDynamic().crypto.subtle.digest(type, this)
    return suspendCoroutine { c -> p.then({c.resume(Int8Array(it).unsafeCast<ByteArray>())}, {c.resumeWithException(it)}) }
}
actual suspend fun ByteArray.sha1(): ByteArray = digest("SHA-1")
actual suspend fun ByteArray.sha256(): ByteArray = digest("SHA-256")
actual suspend fun ByteArray.sha512(): ByteArray = digest("SHA-512")