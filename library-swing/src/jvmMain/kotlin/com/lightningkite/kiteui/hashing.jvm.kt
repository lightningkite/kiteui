package com.lightningkite.kiteui

import java.security.MessageDigest

actual suspend fun ByteArray.sha1(): ByteArray = MessageDigest.getInstance("SHA-1").let {
    it.reset()
    it.update(this)
    it.digest()
}
actual suspend fun ByteArray.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").let {
    it.reset()
    it.update(this)
    it.digest()
}
actual suspend fun ByteArray.sha512(): ByteArray = MessageDigest.getInstance("SHA-512").let {
    it.reset()
    it.update(this)
    it.digest()
}