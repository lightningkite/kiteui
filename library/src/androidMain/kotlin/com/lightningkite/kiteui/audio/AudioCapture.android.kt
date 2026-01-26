// by Claude
package com.lightningkite.kiteui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Android implementation of AudioCapture using AudioRecord.
 * Uses VOICE_COMMUNICATION audio source for echo cancellation.
 */
actual class AudioCapture actual constructor(actual val format: AudioFormat) {
    private val _hasPermission = Signal(false)
    private val _isCapturing = Signal(false)
    private val _level = Signal(0f)

    actual val hasPermission: Reactive<Boolean> = _hasPermission
    actual val isCapturing: Reactive<Boolean> = _isCapturing
    actual val level: Reactive<Float> = _level

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private var onDataCallback: ((ByteArray) -> Unit)? = null
    @Volatile private var isRunning = false

    private val channelConfig = if (format.channels == 1) {
        AndroidAudioFormat.CHANNEL_IN_MONO
    } else {
        AndroidAudioFormat.CHANNEL_IN_STEREO
    }

    private val audioFormat = AndroidAudioFormat.ENCODING_PCM_16BIT

    private val bufferSize: Int by lazy {
        val minBuffer = AudioRecord.getMinBufferSize(format.sampleRate, channelConfig, audioFormat)
        // Use a buffer that's at least 4096 samples (good balance of latency/performance)
        maxOf(minBuffer, format.sampleRate / 10 * format.bytesPerSample)
    }

    actual fun onAudioData(action: (ByteArray) -> Unit) {
        onDataCallback = action
    }

    actual suspend fun start(): Boolean {
        // Check/request permission
        val context = AndroidAppContext.applicationCtx

        val hasRecordPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasRecordPermission) {
            // Request permission
            val result = AndroidAppContext.requestPermissions(
                Manifest.permission.RECORD_AUDIO
            )
            if (!result.accepted) {
                _hasPermission.value = false
                return false
            }
        }

        _hasPermission.value = true

        return withContext(Dispatchers.IO) {
            try {
                // Create AudioRecord with echo cancellation
                @Suppress("MissingPermission")
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    format.sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.release()
                    audioRecord = null
                    return@withContext false
                }

                audioRecord?.startRecording()
                isRunning = true
                _isCapturing.value = true

                // Start capture thread
                captureThread = Thread {
                    val buffer = ByteArray(bufferSize)
                    while (isRunning) {
                        val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (bytesRead > 0) {
                            val data = buffer.copyOf(bytesRead)

                            // Calculate level
                            _level.value = calculateLevel(data)

                            // Invoke callback
                            onDataCallback?.invoke(data)
                        }
                    }
                }.apply {
                    name = "AudioCapture-Thread"
                    start()
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                audioRecord?.release()
                audioRecord = null
                false
            }
        }
    }

    actual fun stop() {
        isRunning = false
        captureThread?.join(1000)
        captureThread = null

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            // Ignore
        }

        _isCapturing.value = false
        _level.value = 0f
    }

    actual fun release() {
        stop()
        audioRecord?.release()
        audioRecord = null
        onDataCallback = null
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
