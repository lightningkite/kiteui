package com.lightningkite.kiteui.audio

import com.lightningkite.kiteui.onMainThread
import com.lightningkite.kiteui.setVolume
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import java.util.concurrent.LinkedBlockingQueue
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import javax.sound.sampled.AudioFormat as LineFormat

public actual class AudioPlayback actual constructor(public actual val format: AudioFormat) {
    private val _isPlaying = Signal(false)
    private val _bufferedDurationMs = Signal(0L)
    public actual val isPlaying: Reactive<Boolean> = _isPlaying
    public actual val bufferedDurationMs: Reactive<Long> = _bufferedDurationMs

    private val lineFormat = LineFormat(format.sampleRate.toFloat(), format.bitsPerSample, format.channels, format.signed, false)
    private val queue = LinkedBlockingQueue<ByteArray>()
    private var onBufferEmptyCallback: (() -> Unit)? = null

    // Both exist only while started.
    private var line: SourceDataLine? = null
    private var writer: Thread? = null

    public actual var volume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            line?.setVolume(field)
        }

    public actual fun enqueue(data: ByteArray) {
        if (data.isEmpty()) return
        queue.put(data)
        updateBufferedDuration()
    }

    public actual fun start() {
        if (line != null) return
        val line = AudioSystem.getSourceDataLine(lineFormat)
        line.open(lineFormat)
        line.setVolume(volume)
        line.start()
        this.line = line
        _isPlaying.value = true
        writer = thread(name = "KiteUI AudioPlayback") {
            try {
                while (true) {
                    val chunk = queue.take()
                    // Like the web, "empty" means everything queued has been handed to the audio system.
                    onMainThread {
                        updateBufferedDuration()
                        if (queue.isEmpty() && _isPlaying.value) onBufferEmptyCallback?.invoke()
                    }
                    line.write(chunk, 0, chunk.size)
                }
            } catch (e: InterruptedException) {
                // Stopped.
            }
        }
    }

    public actual fun stop() {
        // Closing the line releases a writer blocked in write(); the interrupt ends one waiting for data.
        writer?.interrupt()
        line?.close()
        writer?.join()
        writer = null
        line = null
        _isPlaying.value = false
        clearBuffer()
    }

    public actual fun clearBuffer() {
        queue.clear()
        updateBufferedDuration()
    }

    public actual fun onBufferEmpty(action: () -> Unit) {
        onBufferEmptyCallback = action
    }

    public actual fun release() {
        stop()
        onBufferEmptyCallback = null
    }

    private fun updateBufferedDuration() {
        _bufferedDurationMs.value = format.bytesToMs(queue.sumOf { it.size })
    }
}
