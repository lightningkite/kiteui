// by Claude
package com.lightningkite.kiteui.audio

import android.media.AudioAttributes
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioTrack
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Android implementation of AudioPlayback using AudioTrack.
 * Buffers incoming PCM16 audio and plays it in streaming mode.
 */
public actual class AudioPlayback actual constructor(public actual val format: AudioFormat) {
    private val _isPlaying = Signal(false)
    private val _bufferedDurationMs = Signal(0L)

    public actual val isPlaying: Reactive<Boolean> = _isPlaying
    public actual val bufferedDurationMs: Reactive<Long> = _bufferedDurationMs

    public actual var volume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            audioTrack?.setVolume(field)
        }

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null
    private var onBufferEmptyCallback: (() -> Unit)? = null

    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()
    @Volatile private var isRunning = false
    @Volatile private var totalBufferedBytes = 0L

    private val channelConfig = if (format.channels == 1) {
        AndroidAudioFormat.CHANNEL_OUT_MONO
    } else {
        AndroidAudioFormat.CHANNEL_OUT_STEREO
    }

    private val audioFormat = AndroidAudioFormat.ENCODING_PCM_16BIT

    private val bufferSize: Int by lazy {
        val minBuffer = AudioTrack.getMinBufferSize(format.sampleRate, channelConfig, audioFormat)
        // Use a larger buffer for smoother playback
        maxOf(minBuffer, format.sampleRate / 5 * format.bytesPerSample)
    }

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormatBuilder = AndroidAudioFormat.Builder()
                .setSampleRate(format.sampleRate)
                .setEncoding(audioFormat)
                .setChannelMask(channelConfig)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(audioFormatBuilder)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(volume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public actual fun enqueue(data: ByteArray) {
        if (data.isEmpty()) return
        audioQueue.offer(data)
        totalBufferedBytes += data.size
        updateBufferedDuration()
    }

    public actual fun start() {
        if (audioTrack == null) {
            initAudioTrack()
        }

        if (isRunning) return

        isRunning = true
        _isPlaying.value = true

        audioTrack?.play()

        playbackThread = Thread {
            while (isRunning) {
                val data = audioQueue.poll()
                if (data != null) {
                    totalBufferedBytes -= data.size
                    updateBufferedDuration()

                    audioTrack?.write(data, 0, data.size)
                } else {
                    // Buffer empty
                    if (_isPlaying.value) {
                        onBufferEmptyCallback?.invoke()
                    }
                    // Small sleep to avoid busy-waiting
                    Thread.sleep(10)
                }
            }
        }.apply {
            name = "AudioPlayback-Thread"
            start()
        }
    }

    public actual fun stop() {
        isRunning = false
        playbackThread?.join(1000)
        playbackThread = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            // Ignore
        }

        _isPlaying.value = false
        clearBuffer()
    }

    public actual fun clearBuffer() {
        audioQueue.clear()
        totalBufferedBytes = 0
        updateBufferedDuration()
    }

    public actual fun onBufferEmpty(action: () -> Unit) {
        onBufferEmptyCallback = action
    }

    public actual fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
        onBufferEmptyCallback = null
    }

    private fun updateBufferedDuration() {
        _bufferedDurationMs.value = format.bytesToMs(totalBufferedBytes.toInt())
    }
}
