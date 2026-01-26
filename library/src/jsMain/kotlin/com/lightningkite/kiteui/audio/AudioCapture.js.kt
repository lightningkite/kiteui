// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.Float32Array
import org.khronos.webgl.get
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamConstraints
import kotlin.js.Promise
import kotlin.js.json
import kotlin.math.sqrt

/**
 * JavaScript/Web implementation of AudioCapture using Web Audio API.
 * Uses getUserMedia for microphone access and ScriptProcessorNode for audio data.
 */
actual class AudioCapture actual constructor(actual val format: AudioFormat) {
    private val _hasPermission = Signal(false)
    private val _isCapturing = Signal(false)
    private val _level = Signal(0f)

    actual val hasPermission: Reactive<Boolean> = _hasPermission
    actual val isCapturing: Reactive<Boolean> = _isCapturing
    actual val level: Reactive<Float> = _level

    private var audioContext: VoiceAudioContext? = null
    private var mediaStream: MediaStream? = null
    private var sourceNode: MediaStreamAudioSourceNode? = null
    private var processorNode: ScriptProcessorNode? = null
    private var onDataCallback: ((ByteArray) -> Unit)? = null

    actual fun onAudioData(action: (ByteArray) -> Unit) {
        onDataCallback = action
    }

    actual suspend fun start(): Boolean {
        try {
            // Request microphone access with audio constraints
            val audioConstraints = json(
                "sampleRate" to format.sampleRate,
                "channelCount" to format.channels,
                "echoCancellation" to true,
                "noiseSuppression" to true
            )
            val constraints = json("audio" to audioConstraints)

            val stream = window.navigator.mediaDevices.getUserMedia(constraints.unsafeCast<MediaStreamConstraints>()).await()
            mediaStream = stream
            _hasPermission.value = true

            // Create audio context with desired sample rate
            val contextOptions = json("sampleRate" to format.sampleRate)
            audioContext = VoiceAudioContext(contextOptions)

            val ctx = audioContext!!

            // Create source from media stream
            sourceNode = ctx.createMediaStreamSource(stream)

            // Use ScriptProcessorNode for audio processing
            // Buffer size of 4096 is a good balance between latency and performance
            val bufferSize = 4096
            processorNode = ctx.createScriptProcessor(bufferSize, format.channels, format.channels)

            processorNode!!.onaudioprocess = { event: AudioProcessingEvent ->
                val inputBuffer = event.inputBuffer
                val inputData: Float32Array = inputBuffer.getChannelData(0)

                // Calculate level
                var sumSquares = 0.0
                for (i in 0 until inputData.length) {
                    val sample = inputData[i].toDouble()
                    sumSquares += sample * sample
                }
                val rms = sqrt(sumSquares / inputData.length)
                _level.value = rms.toFloat().coerceIn(0f, 1f)

                // Convert Float32 to PCM16
                val pcm16 = floatArrayToPcm16(inputData)
                onDataCallback?.invoke(pcm16)
            }

            sourceNode!!.connect(processorNode!!)
            processorNode!!.connect(ctx.destination)

            _isCapturing.value = true
            return true
        } catch (e: Throwable) {
            console.error("AudioCapture.start failed:", e)
            _hasPermission.value = false
            return false
        }
    }

    actual fun stop() {
        try {
            processorNode?.disconnect()
            sourceNode?.disconnect()
            mediaStream?.getTracks()?.forEach { track ->
                track.asDynamic().stop()
            }
            _isCapturing.value = false
            _level.value = 0f
        } catch (e: Throwable) {
            console.error("AudioCapture.stop error:", e)
        }
    }

    actual fun release() {
        stop()
        try {
            audioContext?.close()
        } catch (e: Throwable) {
            // Ignore
        }
        audioContext = null
        mediaStream = null
        sourceNode = null
        processorNode = null
        onDataCallback = null
    }

    private fun floatArrayToPcm16(floatData: Float32Array): ByteArray {
        val result = ByteArray(floatData.length * 2)
        for (i in 0 until floatData.length) {
            val sample = (floatData[i].coerceIn(-1f, 1f) * 32767).toInt()
            result[i * 2] = (sample and 0xFF).toByte()
            result[i * 2 + 1] = (sample shr 8).toByte()
        }
        return result
    }
}

// External declarations for Web Audio API

external class VoiceAudioContext(options: dynamic = definedExternally) {
    val destination: AudioDestinationNode
    val sampleRate: Double
    val state: String
    fun createMediaStreamSource(stream: MediaStream): MediaStreamAudioSourceNode
    fun createScriptProcessor(bufferSize: Int, numberOfInputChannels: Int, numberOfOutputChannels: Int): ScriptProcessorNode
    fun createBufferSource(): AudioBufferSourceNode
    fun createBuffer(numberOfChannels: Int, length: Int, sampleRate: Int): AudioBuffer
    fun createGain(): GainNode
    fun resume(): Promise<Unit>
    fun close(): Promise<Unit>
}

external class AudioDestinationNode : AudioNode

open external class AudioNode {
    fun connect(destination: AudioNode): AudioNode
    fun disconnect()
}

external class MediaStreamAudioSourceNode : AudioNode

external class ScriptProcessorNode : AudioNode {
    var onaudioprocess: ((AudioProcessingEvent) -> Unit)?
}

external class AudioProcessingEvent {
    val inputBuffer: AudioBuffer
    val outputBuffer: AudioBuffer
}

external class AudioBuffer {
    val numberOfChannels: Int
    val length: Int
    val sampleRate: Double
    val duration: Double
    fun getChannelData(channel: Int): Float32Array
}

external class AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer?
    fun start(time: Double = definedExternally)
    fun stop(time: Double = definedExternally)
}

external class GainNode : AudioNode {
    val gain: AudioParam
}

external class AudioParam {
    var value: Float
}
