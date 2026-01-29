// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Untested
import com.lightningkite.reactive.core.*

/**
 * Plays streaming audio in real-time.
 *
 * Audio chunks are enqueued and played sequentially, enabling real-time streaming playback.
 * Designed for voice/audio streaming applications where data arrives incrementally.
 *
 * Usage:
 * ```kotlin
 * val playback = AudioPlayback()
 * // Enqueue audio chunks as they arrive
 * playback.enqueue(chunk1)
 * playback.enqueue(chunk2)
 * playback.start()
 * // Later:
 * playback.stop()
 * playback.release()
 * ```
 *
 * @param format The audio format to play (default: 24kHz mono PCM16)
 */
actual class AudioPlayback actual constructor(actual val format: AudioFormat) {
    /** The audio format being played */

    /** Whether audio is currently playing */
    actual val isPlaying: Reactive<Boolean> = Signal(false)

    /** Approximate buffered duration in milliseconds */
    actual val bufferedDurationMs: Reactive<Long> = Signal(0L)

    /** Volume from 0.0 to 1.0 */
    actual var volume: Float = 0f

    /**
     * Enqueue audio data for playback.
     * Audio is buffered and played in order.
     * @param data PCM16 audio bytes to play
     */
    actual fun enqueue(data: ByteArray) {}

    /** Start playback of buffered audio */
    actual fun start() {}

    /** Stop playback and clear buffer */
    actual fun stop() {}

    /** Clear buffer without stopping playback state */
    actual fun clearBuffer() {}

    /**
     * Register callback for when buffer becomes empty.
     * Useful for detecting end of stream or requesting more data.
     * @param action Callback invoked when buffer is empty
     */
    actual fun onBufferEmpty(action: () -> Unit) {}

    /** Release all resources. Instance should not be used after calling this. */
    actual fun release() {}
}

actual class AudioCapture actual constructor(actual val format: AudioFormat) {

    /** Whether microphone permission has been granted */
    actual val hasPermission: Reactive<Boolean> = Signal(false)

    /** Whether capture is currently active */
    actual val isCapturing: Reactive<Boolean> = Signal(false)

    /** Current audio level (0.0 to 1.0) for visualization, updated in real-time */
    actual val level: Reactive<Float> = Signal(0.0f)

    /**
     * Register callback for audio data.
     * Called with PCM16 byte arrays as audio is captured.
     * @param action Callback receiving PCM16 audio bytes
     */
    actual fun onAudioData(action: (ByteArray) -> Unit) {}

    /**
     * Start capturing audio from the microphone.
     * Requests permission if not already granted.
     * @return true if capture started successfully, false if permission denied or error occurred
     */
    actual suspend fun start(): Boolean = false

    /** Stop capturing audio */
    actual fun stop() {}

    /** Release all resources. Instance should not be used after calling this. */
    actual fun release() {}
}
