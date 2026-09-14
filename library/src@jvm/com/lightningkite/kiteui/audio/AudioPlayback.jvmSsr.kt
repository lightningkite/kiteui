// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

/**
 * SSR stub for AudioPlayback.
 * Audio playback is not available during server-side rendering.
 */
public actual class AudioPlayback actual constructor(public actual val format: AudioFormat) {
    public actual val isPlaying: Reactive<Boolean> = Constant(false)
    public actual val bufferedDurationMs: Reactive<Long> = Constant(0L)
    public actual var volume: Float = 1f

    public actual fun enqueue(data: ByteArray) {
        // No-op for SSR
    }

    public actual fun start() {
        // No-op for SSR
    }

    public actual fun stop() {
        // No-op for SSR
    }

    public actual fun clearBuffer() {
        // No-op for SSR
    }

    public actual fun onBufferEmpty(action: () -> Unit) {
        // No-op for SSR
    }

    public actual fun release() {
        // No-op for SSR
    }
}
