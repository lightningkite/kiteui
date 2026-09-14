// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Untested
import com.lightningkite.reactive.core.Reactive

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
@ExperimentalKiteUi
@Untested
public expect class AudioPlayback(format: AudioFormat = AudioFormat()) {
    /** The audio format being played */
    public val format: AudioFormat

    /** Whether audio is currently playing */
    public val isPlaying: Reactive<Boolean>

    /** Approximate buffered duration in milliseconds */
    public val bufferedDurationMs: Reactive<Long>

    /** Volume from 0.0 to 1.0 */
    public var volume: Float

    /**
     * Enqueue audio data for playback.
     * Audio is buffered and played in order.
     * @param data PCM16 audio bytes to play
     */
    public fun enqueue(data: ByteArray)

    /** Start playback of buffered audio */
    public fun start()

    /** Stop playback and clear buffer */
    public fun stop()

    /** Clear buffer without stopping playback state */
    public fun clearBuffer()

    /**
     * Register callback for when buffer becomes empty.
     * Useful for detecting end of stream or requesting more data.
     * @param action Callback invoked when buffer is empty
     */
    public fun onBufferEmpty(action: () -> Unit)

    /** Release all resources. Instance should not be used after calling this. */
    public fun release()
}
