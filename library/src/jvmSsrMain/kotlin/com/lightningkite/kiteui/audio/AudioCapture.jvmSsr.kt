// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

/**
 * SSR stub for AudioCapture.
 * Audio capture is not available during server-side rendering.
 */
actual class AudioCapture actual constructor(actual val format: AudioFormat) {
    actual val hasPermission: Reactive<Boolean> = Constant(false)
    actual val isCapturing: Reactive<Boolean> = Constant(false)
    actual val level: Reactive<Float> = Constant(0f)

    actual fun onAudioData(action: (ByteArray) -> Unit) {
        // No-op for SSR
    }

    actual suspend fun start(): Boolean = false

    actual fun stop() {
        // No-op for SSR
    }

    actual fun release() {
        // No-op for SSR
    }
}
