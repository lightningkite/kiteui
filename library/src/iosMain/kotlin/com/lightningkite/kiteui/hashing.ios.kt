@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.kiteui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.*

actual suspend fun ByteArray.sha1(): ByteArray {
    val out = UByteArray(CC_SHA1_DIGEST_LENGTH)
    this.usePinned { inputPinned ->
        out.usePinned { outputPinned ->
            CC_SHA1(inputPinned.addressOf(0), size.convert(), outputPinned.addressOf(0))
        }
    }
    return out.asByteArray()
}
actual suspend fun ByteArray.sha256(): ByteArray {
    val out = UByteArray(CC_SHA256_DIGEST_LENGTH)
    this.usePinned { inputPinned ->
        out.usePinned { outputPinned ->
            CC_SHA256(inputPinned.addressOf(0), size.convert(), outputPinned.addressOf(0))
        }
    }
    return out.asByteArray()
}
actual suspend fun ByteArray.sha512(): ByteArray {
    val out = UByteArray(CC_SHA512_DIGEST_LENGTH)
    this.usePinned { inputPinned ->
        out.usePinned { outputPinned ->
            CC_SHA512(inputPinned.addressOf(0), size.convert(), outputPinned.addressOf(0))
        }
    }
    return out.asByteArray()
}