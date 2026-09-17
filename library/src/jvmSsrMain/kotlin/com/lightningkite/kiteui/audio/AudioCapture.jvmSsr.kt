// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

/**
 * SSR stub for AudioCapture.
 * Audio capture is not available during server-side rendering.
 */
public actual class AudioCapture actual constructor(public actual val format: AudioFormat) {
    public actual val hasPermission: Reactive<Boolean> = Constant(false)
    public actual val isCapturing: Reactive<Boolean> = Constant(false)
    public actual val level: Reactive<Float> = Constant(0f)

    public actual fun onAudioData(action: (ByteArray) -> Unit) {
        // No-op for SSR
    }

    public actual suspend fun start(): Boolean = false

    public actual fun stop() {
        // No-op for SSR
    }

    public actual fun release() {
        // No-op for SSR
    }
}
