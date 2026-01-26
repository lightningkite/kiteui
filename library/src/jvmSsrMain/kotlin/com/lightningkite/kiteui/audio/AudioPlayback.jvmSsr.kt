// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

/**
 * SSR stub for AudioPlayback.
 * Audio playback is not available during server-side rendering.
 */
actual class AudioPlayback actual constructor(actual val format: AudioFormat) {
    actual val isPlaying: Reactive<Boolean> = Constant(false)
    actual val bufferedDurationMs: Reactive<Long> = Constant(0L)
    actual var volume: Float = 1f

    actual fun enqueue(data: ByteArray) {
        // No-op for SSR
    }

    actual fun start() {
        // No-op for SSR
    }

    actual fun stop() {
        // No-op for SSR
    }

    actual fun clearBuffer() {
        // No-op for SSR
    }

    actual fun onBufferEmpty(action: () -> Unit) {
        // No-op for SSR
    }

    actual fun release() {
        // No-op for SSR
    }
}
