// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Reactive

/**
 * Captures audio from the device microphone.
 *
 * Provides real-time audio capture with support for permission handling and level monitoring.
 * Audio data is delivered as PCM16 byte arrays at the configured sample rate.
 *
 * Usage:
 * ```kotlin
 * val capture = AudioCapture()
 * capture.onAudioData { pcmBytes ->
 *     // Process or send audio data
 * }
 * if (capture.start()) {
 *     // Recording active...
 * }
 * // Later:
 * capture.stop()
 * capture.release()
 * ```
 *
 * @param format The audio format to capture (default: 24kHz mono PCM16)
 */
expect class AudioCapture(format: AudioFormat = AudioFormat()) {
    /** The audio format being captured */
    val format: AudioFormat

    /** Whether microphone permission has been granted */
    val hasPermission: Reactive<Boolean>

    /** Whether capture is currently active */
    val isCapturing: Reactive<Boolean>

    /** Current audio level (0.0 to 1.0) for visualization, updated in real-time */
    val level: Reactive<Float>

    /**
     * Register callback for audio data.
     * Called with PCM16 byte arrays as audio is captured.
     * @param action Callback receiving PCM16 audio bytes
     */
    fun onAudioData(action: (ByteArray) -> Unit)

    /**
     * Start capturing audio from the microphone.
     * Requests permission if not already granted.
     * @return true if capture started successfully, false if permission denied or error occurred
     */
    suspend fun start(): Boolean

    /** Stop capturing audio */
    fun stop()

    /** Release all resources. Instance should not be used after calling this. */
    fun release()
}
