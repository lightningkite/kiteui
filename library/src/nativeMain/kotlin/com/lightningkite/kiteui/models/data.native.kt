package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.Blob

actual class ImageRaw actual constructor(val data: Blob) : ImageSource() {
    actual fun release() = Unit
}
actual class VideoRaw actual constructor(val data: Blob) : VideoSource() {
    actual fun release() = Unit
}
actual class AudioRaw actual constructor(val data: Blob) : AudioSource() {
    actual fun release() = Unit
}