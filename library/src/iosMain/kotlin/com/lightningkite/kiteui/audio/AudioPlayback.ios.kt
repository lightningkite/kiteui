// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.CoreAudioTypes.kAudioFormatLinearPCM
import platform.CoreAudioTypes.kAudioFormatFlagsNativeEndian
import platform.CoreAudioTypes.kAudioFormatFlagIsPacked
import platform.CoreAudioTypes.kAudioFormatFlagIsSignedInteger

/**
 * iOS implementation of AudioPlayback using AVAudioEngine.
 * Uses AVAudioPlayerNode to play streaming PCM16 audio.
 */
@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayback actual constructor(actual val format: AudioFormat) {
    private val _isPlaying = Signal(false)
    private val _bufferedDurationMs = Signal(0L)

    actual val isPlaying: Reactive<Boolean> = _isPlaying
    actual val bufferedDurationMs: Reactive<Long> = _bufferedDurationMs

    actual var volume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            playerNode?.volume = field
        }

    private var audioEngine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var onBufferEmptyCallback: (() -> Unit)? = null

    private val audioQueue = ArrayDeque<ByteArray>()
    private var totalBufferedBytes = 0L
    private var isScheduling = false

    // Audio format for playback
    private val pcmFormat: AVAudioFormat by lazy {
        AVAudioFormat(
            commonFormat = AVAudioPCMFormatInt16,
            sampleRate = format.sampleRate.toDouble(),
            channels = format.channels.toUInt(),
            interleaved = true
        )!!
    }

    init {
        initAudioEngine()
    }

    private fun initAudioEngine() {
        try {
            // Configure audio session
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)

            audioEngine = AVAudioEngine()
            playerNode = AVAudioPlayerNode()

            val engine = audioEngine!!
            val player = playerNode!!

            engine.attachNode(player)
            engine.connect(player, engine.mainMixerNode, pcmFormat)

            player.volume = volume

            engine.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun enqueue(data: ByteArray) {
        if (data.isEmpty()) return
        audioQueue.addLast(data)
        totalBufferedBytes += data.size
        updateBufferedDuration()

        if (_isPlaying.value && !isScheduling) {
            scheduleBuffers()
        }
    }

    actual fun start() {
        if (audioEngine == null) {
            initAudioEngine()
        }

        val engine = audioEngine ?: return

        if (!engine.isRunning()) {
            engine.startAndReturnError(null)
        }

        playerNode?.play()
        _isPlaying.value = true

        scheduleBuffers()
    }

    actual fun stop() {
        playerNode?.stop()
        _isPlaying.value = false
        clearBuffer()
    }

    actual fun clearBuffer() {
        audioQueue.clear()
        totalBufferedBytes = 0
        updateBufferedDuration()
    }

    actual fun onBufferEmpty(action: () -> Unit) {
        onBufferEmptyCallback = action
    }

    actual fun release() {
        stop()
        audioEngine?.stop()
        audioEngine = null
        playerNode = null
        onBufferEmptyCallback = null
    }

    private fun scheduleBuffers() {
        if (!_isPlaying.value || playerNode == null) return

        isScheduling = true
        val player = playerNode!!

        while (audioQueue.isNotEmpty()) {
            val pcm16Data = audioQueue.removeFirst()
            totalBufferedBytes -= pcm16Data.size
            updateBufferedDuration()

            // Create AVAudioPCMBuffer from PCM16 data
            val buffer = createBuffer(pcm16Data)
            if (buffer != null) {
                player.scheduleBuffer(buffer, completionHandler = null)
            }
        }

        isScheduling = false

        // Check if buffer is empty
        if (audioQueue.isEmpty() && _isPlaying.value) {
            onBufferEmptyCallback?.invoke()
        }
    }

    private fun createBuffer(pcm16Data: ByteArray): AVAudioPCMBuffer? {
        val samples = pcm16Data.size / 2
        if (samples == 0) return null

        val buffer = AVAudioPCMBuffer(pcmFormat, samples.toUInt())
        buffer.frameLength = samples.toUInt()

        // Copy PCM16 data to buffer
        val int16ChannelData = buffer.int16ChannelData ?: return null
        val channelData = int16ChannelData[0] ?: return null

        for (i in 0 until samples) {
            val low = pcm16Data[i * 2].toInt() and 0xFF
            val high = pcm16Data[i * 2 + 1].toInt()
            val sample = ((high shl 8) or low).toShort()
            channelData[i] = sample
        }

        return buffer
    }

    private fun updateBufferedDuration() {
        _bufferedDurationMs.value = format.bytesToMs(totalBufferedBytes.toInt())
    }
}
