package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Reactive

public actual class AudioPlayback actual constructor(public actual val format: AudioFormat) {
    init { throw NotImplementedError("Compose backend: AudioPlayback is not implemented yet") }

    public actual val isPlaying: Reactive<Boolean> get() = TODO("Compose backend: AudioPlayback")
    public actual val bufferedDurationMs: Reactive<Long> get() = TODO("Compose backend: AudioPlayback")
    public actual var volume: Float
        get() = TODO("Compose backend: AudioPlayback")
        set(value) = TODO("Compose backend: AudioPlayback")

    public actual fun enqueue(data: ByteArray): Unit = TODO("Compose backend: AudioPlayback")
    public actual fun start(): Unit = TODO("Compose backend: AudioPlayback")
    public actual fun stop(): Unit = TODO("Compose backend: AudioPlayback")
    public actual fun clearBuffer(): Unit = TODO("Compose backend: AudioPlayback")
    public actual fun onBufferEmpty(action: () -> Unit): Unit = TODO("Compose backend: AudioPlayback")
    public actual fun release(): Unit = TODO("Compose backend: AudioPlayback")
}
