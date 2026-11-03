// by Claude
package com.lightningkite.kiteui.audio

import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.sqrt

/**
 * iOS implementation of AudioCapture using AVAudioEngine.
 * Uses input node with tap to capture PCM audio data.
 */
@OptIn(ExperimentalForeignApi::class)
public actual class AudioCapture actual constructor(public actual val format: AudioFormat) {
    private val _hasPermission = Signal(false)
    private val _isCapturing = Signal(false)
    private val _level = Signal(0f)

    public actual val hasPermission: Reactive<Boolean> = _hasPermission
    public actual val isCapturing: Reactive<Boolean> = _isCapturing
    public actual val level: Reactive<Float> = _level

    private var audioEngine: AVAudioEngine? = null
    private var onDataCallback: ((ByteArray) -> Unit)? = null

    public actual fun onAudioData(action: (ByteArray) -> Unit) {
        onDataCallback = action
    }

    public actual suspend fun start(): Boolean {
        // Request microphone permission
        val permissionGranted = suspendCoroutine<Boolean> { cont ->
            AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                cont.resume(granted)
            }
        }

        if (!permissionGranted) {
            _hasPermission.value = false
            return false
        }
        _hasPermission.value = true

        try {
            // Configure audio session for recording
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, null)
            session.setActive(true, null)

            // Create audio engine
            audioEngine = AVAudioEngine()
            val engine = audioEngine!!

            val inputNode = engine.inputNode
            val inputFormat = inputNode.outputFormatForBus(0u)

            // Calculate buffer size for approximately 100ms of audio
            val bufferSize = (format.sampleRate.toDouble() * 0.1).toUInt()

            // Install tap on input node to receive audio data
            inputNode.installTapOnBus(
                bus = 0u,
                bufferSize = bufferSize,
                format = inputFormat
            ) { buffer, _ ->
                buffer?.let { audioBuffer ->
                    // Convert AVAudioPCMBuffer to PCM16 ByteArray
                    val pcmData = convertBufferToPcm16(audioBuffer, inputFormat.sampleRate.toInt())
                    if (pcmData.isNotEmpty()) {
                        // Calculate level
                        _level.value = calculateLevel(pcmData)

                        // Invoke callback
                        onDataCallback?.invoke(pcmData)
                    }
                }
            }

            // Start the engine
            engine.prepare()
            engine.startAndReturnError(null)

            _isCapturing.value = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _isCapturing.value = false
            return false
        }
    }

    public actual fun stop() {
        audioEngine?.inputNode?.removeTapOnBus(0u)
        audioEngine?.stop()
        _isCapturing.value = false
        _level.value = 0f
    }

    public actual fun release() {
        stop()
        audioEngine = null
        onDataCallback = null
    }

    private fun convertBufferToPcm16(buffer: AVAudioPCMBuffer, sourceSampleRate: Int): ByteArray {
        val floatChannelData = buffer.floatChannelData ?: return ByteArray(0)
        val frameLength = buffer.frameLength.toInt()
        if (frameLength == 0) return ByteArray(0)

        // Get the first channel's data
        val channelData = floatChannelData[0] ?: return ByteArray(0)

        // If source sample rate matches our format, convert directly
        // Otherwise we need to resample
        if (sourceSampleRate == format.sampleRate) {
            return floatDataToPcm16(channelData, frameLength)
        } else {
            // Simple resampling: take every Nth sample
            val ratio = sourceSampleRate.toDouble() / format.sampleRate
            val outputSamples = (frameLength / ratio).toInt()
            val result = ByteArray(outputSamples * 2)

            for (i in 0 until outputSamples) {
                val srcIndex = (i * ratio).toInt().coerceIn(0, frameLength - 1)
                val sample = (channelData[srcIndex].coerceIn(-1f, 1f) * 32767).toInt()
                result[i * 2] = (sample and 0xFF).toByte()
                result[i * 2 + 1] = (sample shr 8).toByte()
            }
            return result
        }
    }

    private fun floatDataToPcm16(data: CPointer<FloatVar>, length: Int): ByteArray {
        val result = ByteArray(length * 2)
        for (i in 0 until length) {
            val sample = (data[i].coerceIn(-1f, 1f) * 32767).toInt()
            result[i * 2] = (sample and 0xFF).toByte()
            result[i * 2 + 1] = (sample shr 8).toByte()
        }
        return result
    }

    private fun calculateLevel(data: ByteArray): Float {
        if (data.size < 2) return 0f
        val samples = data.size / 2
        var sumSquares = 0.0
        for (i in 0 until samples) {
            val low = data[i * 2].toInt() and 0xFF
            val high = data[i * 2 + 1].toInt()
            val sample = (high shl 8) or low
            val normalized = sample / 32768.0
            sumSquares += normalized * normalized
        }
        val rms = sqrt(sumSquares / samples)
        return rms.toFloat().coerceIn(0f, 1f)
    }
}
