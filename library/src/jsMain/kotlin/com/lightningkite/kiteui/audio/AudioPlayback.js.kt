// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.await
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import kotlin.js.json

/**
 * JavaScript/Web implementation of AudioPlayback using Web Audio API.
 * Buffers incoming PCM16 audio and plays it through AudioBufferSourceNodes.
 */
public actual class AudioPlayback actual constructor(public actual val format: AudioFormat) {
    private val _isPlaying = Signal(false)
    private val _bufferedDurationMs = Signal(0L)

    public actual val isPlaying: Reactive<Boolean> = _isPlaying
    public actual val bufferedDurationMs: Reactive<Long> = _bufferedDurationMs

    public actual var volume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            gainNode?.gain?.value = field
        }

    private var audioContext: VoiceAudioContext? = null
    private var gainNode: GainNode? = null
    private var onBufferEmptyCallback: (() -> Unit)? = null

    // Buffer queue for audio data
    private val audioQueue = ArrayDeque<ByteArray>()

    // Every buffer scheduled since the last stop() - a single drain of audioQueue can
    // schedule several nodes at once, so stop() must stop all of them, not just the latest.
    private val scheduledSourceNodes = mutableListOf<AudioBufferSourceNode>()
    private var nextStartTime: Double = 0.0
    private var isScheduling = false

    init {
        initAudioContext()
    }

    private fun initAudioContext() {
        try {
            val contextOptions = json("sampleRate" to format.sampleRate)
            audioContext = VoiceAudioContext(contextOptions)
            gainNode = audioContext!!.createGain()
            gainNode!!.gain.value = volume
            gainNode!!.connect(audioContext!!.destination)
        } catch (e: Throwable) {
            console.error("AudioPlayback: Failed to create AudioContext:", e)
        }
    }

    public actual fun enqueue(data: ByteArray) {
        if (data.isEmpty()) return
        audioQueue.addLast(data)
        updateBufferedDuration()

        if (_isPlaying.value && !isScheduling) {
            scheduleNextBuffer()
        }
    }

    public actual fun start() {
        if (audioContext == null) {
            initAudioContext()
        }

        // Resume context if suspended (browser autoplay policy)
        val ctx = audioContext
        if (ctx != null && ctx.state == "suspended") {
            ctx.resume()
        }

        _isPlaying.value = true
        nextStartTime = audioContext?.asDynamic()?.currentTime as? Double ?: 0.0
        scheduleNextBuffer()
    }

    public actual fun stop() {
        _isPlaying.value = false
        scheduledSourceNodes.forEach { it.stop() }
        scheduledSourceNodes.clear()
        clearBuffer()
    }

    public actual fun clearBuffer() {
        audioQueue.clear()
        updateBufferedDuration()
    }

    public actual fun onBufferEmpty(action: () -> Unit) {
        onBufferEmptyCallback = action
    }

    public actual fun release() {
        stop()
        try {
            audioContext?.close()
        } catch (e: Throwable) {
            // Ignore
        }
        audioContext = null
        gainNode = null
        onBufferEmptyCallback = null
    }

    private fun scheduleNextBuffer() {
        if (!_isPlaying.value || audioContext == null) return

        isScheduling = true
        val ctx = audioContext!!

        while (audioQueue.isNotEmpty()) {
            val pcm16Data = audioQueue.removeFirst()
            updateBufferedDuration()

            // Convert PCM16 to Float32
            val floatData = pcm16ToFloat32(pcm16Data)

            // Create AudioBuffer
            val buffer = ctx.createBuffer(format.channels, floatData.size, format.sampleRate)
            val channelData: Float32Array = buffer.getChannelData(0)
            for (i in floatData.indices) {
                channelData[i] = floatData[i]
            }

            // Create and configure source node
            val sourceNode = ctx.createBufferSource()
            sourceNode.buffer = buffer
            sourceNode.connect(gainNode!!)

            // Calculate start time
            val currentTime: Double = ctx.asDynamic().currentTime as Double
            if (nextStartTime < currentTime) {
                nextStartTime = currentTime
            }

            sourceNode.start(nextStartTime)
            nextStartTime += buffer.duration

            scheduledSourceNodes.add(sourceNode)
        }

        isScheduling = false

        // Check if buffer is empty
        if (audioQueue.isEmpty() && _isPlaying.value) {
            onBufferEmptyCallback?.invoke()
        }
    }

    private fun updateBufferedDuration() {
        val totalBytes = audioQueue.sumOf { it.size }
        _bufferedDurationMs.value = format.bytesToMs(totalBytes)
    }

    private fun pcm16ToFloat32(pcm16: ByteArray): FloatArray {
        val samples = pcm16.size / 2
        val result = FloatArray(samples)
        for (i in 0 until samples) {
            val low = pcm16[i * 2].toInt() and 0xFF
            val high = pcm16[i * 2 + 1].toInt()
            val sample = (high shl 8) or low
            result[i] = sample / 32768f
        }
        return result
    }
}
