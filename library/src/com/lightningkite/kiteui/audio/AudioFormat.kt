// by Claude
package com.lightningkite.kiteui.audio

/**
 * Audio format specification for capture and playback.
 * Defaults match common voice requirements (24kHz mono PCM16).
 *
 * @param sampleRate Sample rate in Hz (default 24000 for voice)
 * @param channels Number of audio channels (1 = mono, 2 = stereo)
 * @param bitsPerSample Bits per sample (typically 16 for PCM16)
 * @param signed Whether samples are signed (true for PCM16)
 */
public data class AudioFormat(
    val sampleRate: Int = 24000,
    val channels: Int = 1,
    val bitsPerSample: Int = 16,
    val signed: Boolean = true
) {
    /** Bytes per sample (bitsPerSample / 8 * channels) */
    val bytesPerSample: Int get() = (bitsPerSample / 8) * channels

    /** Bytes per second of audio */
    val bytesPerSecond: Int get() = sampleRate * bytesPerSample

    /** Duration in milliseconds for a given number of bytes */
    public fun bytesToMs(bytes: Int): Long = (bytes * 1000L) / bytesPerSecond

    /** Number of bytes for a given duration in milliseconds */
    public fun msToBytes(ms: Long): Int = ((ms * bytesPerSecond) / 1000).toInt()

    public companion object {
        /** Standard format for voice applications: 24kHz mono PCM16 */
        public val VOICE: AudioFormat = AudioFormat(sampleRate = 24000, channels = 1, bitsPerSample = 16)

        /** CD quality audio: 44.1kHz stereo PCM16 */
        public val CD_QUALITY: AudioFormat = AudioFormat(sampleRate = 44100, channels = 2, bitsPerSample = 16)

        /** Standard telephony format: 8kHz mono PCM16 */
        public val TELEPHONY: AudioFormat = AudioFormat(sampleRate = 8000, channels = 1, bitsPerSample = 16)
    }
}
