// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

/**
 * Audio capture is not supported by the Compose desktop backend yet.
 * Always reports no permission and never captures.
 */
public actual class AudioCapture actual constructor(public actual val format: AudioFormat) {
    public actual val hasPermission: Reactive<Boolean> = Constant(false)
    public actual val isCapturing: Reactive<Boolean> = Constant(false)
    public actual val level: Reactive<Float> = Constant(0f)

    public actual fun onAudioData(action: (ByteArray) -> Unit) {
        // No-op
    }

    public actual suspend fun start(): Boolean = false

    public actual fun stop() {
        // No-op
    }

    public actual fun release() {
        // No-op
    }
}
