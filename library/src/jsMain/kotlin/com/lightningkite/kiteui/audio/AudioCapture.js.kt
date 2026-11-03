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
public actual class AudioCapture actual constructor(public actual val format: AudioFormat) {
    private val _hasPermission = Signal(false)
    private val _isCapturing = Signal(false)
    private val _level = Signal(0f)

    public actual val hasPermission: Reactive<Boolean> = _hasPermission
    public actual val isCapturing: Reactive<Boolean> = _isCapturing
    public actual val level: Reactive<Float> = _level

    private var audioContext: VoiceAudioContext? = null
    private var mediaStream: MediaStream? = null
    private var sourceNode: MediaStreamAudioSourceNode? = null
    private var processorNode: ScriptProcessorNode? = null
    private var onDataCallback: ((ByteArray) -> Unit)? = null

    public actual fun onAudioData(action: (ByteArray) -> Unit) {
        onDataCallback = action
    }

    public actual suspend fun start(): Boolean {
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

    public actual fun stop() {
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

    public actual fun release() {
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

public external class VoiceAudioContext(options: dynamic = definedExternally) {
    public val destination: AudioDestinationNode
    public val sampleRate: Double
    public val state: String
    public fun createMediaStreamSource(stream: MediaStream): MediaStreamAudioSourceNode
    public fun createScriptProcessor(bufferSize: Int, numberOfInputChannels: Int, numberOfOutputChannels: Int): ScriptProcessorNode
    public fun createBufferSource(): AudioBufferSourceNode
    public fun createBuffer(numberOfChannels: Int, length: Int, sampleRate: Int): AudioBuffer
    public fun createGain(): GainNode
    public fun resume(): Promise<Unit>
    public fun close(): Promise<Unit>
}

public external class AudioDestinationNode : AudioNode

public open external class AudioNode {
    public fun connect(destination: AudioNode): AudioNode
    public fun disconnect()
}

public external class MediaStreamAudioSourceNode : AudioNode

public external class ScriptProcessorNode : AudioNode {
    public var onaudioprocess: ((AudioProcessingEvent) -> Unit)?
}

public external class AudioProcessingEvent {
    public val inputBuffer: AudioBuffer
    public val outputBuffer: AudioBuffer
}

public external class AudioBuffer {
    public val numberOfChannels: Int
    public val length: Int
    public val sampleRate: Double
    public val duration: Double
    public fun getChannelData(channel: Int): Float32Array
}

public external class AudioBufferSourceNode : AudioNode {
    public var buffer: AudioBuffer?
    public fun start(time: Double = definedExternally)
    public fun stop(time: Double = definedExternally)
}

public external class GainNode : AudioNode {
    public val gain: AudioParam
}

public external class AudioParam {
    public var value: Float
}
