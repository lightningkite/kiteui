package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.utils.SuspendCache
import com.lightningkite.kiteui.views.loadResource
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.ReactiveState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.Line
import javax.sound.sampled.LineEvent
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.log10
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

// Plays through javax.sound, which decodes WAV, AIFF and AU only.
// Like the web, the concurrency limit is ignored: every play gets its own Clip.
public actual class SoundEffectPool actual constructor(concurrency: Int) {
    private class Decoded(val format: AudioFormat, val data: ByteArray)

    private val loaded = SuspendCache<AudioSource, Decoded> { source ->
        val bytes = source.readBytes()
        withContext(Dispatchers.IO) {
            source.decode(bytes).use { Decoded(it.format, it.readAllBytes()) }
        }
    }

    public actual suspend fun preload(sound: AudioSource) {
        loaded.get(sound)
    }

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        val decoded = loaded.get(sound)
        val clip = AudioSystem.getClip()
        clip.open(decoded.format, decoded.data, 0, decoded.data.size)
        var playing = true
        // A clip holds an audio line until closed, so close it as soon as it stops, whether stopped or finished.
        clip.addLineListener {
            if (it.type == LineEvent.Type.STOP) onMainThread {
                playing = false
                clip.close()
            }
        }
        clip.start()
        return object : PlayingSoundEffect {
            override var volume: Float = 1f
                set(value) {
                    field = value
                    // A finished sound's clip is already closed and has no controls left.
                    if (clip.isOpen) clip.setVolume(value)
                }

            // As on the web, a stopped sound can't be restarted, so setting this to true is ignored.
            override var isPlaying: Boolean
                get() = playing
                set(value) {
                    if (!value) stop()
                }

            override fun stop() {
                playing = false
                clip.stop()
            }
        }
    }

    public actual fun unload(sound: AudioSource) {
        loaded.forget(sound)
    }
}

public actual suspend fun AudioSource.load(): PlayableAudio {
    val bytes = readBytes()
    val clip = AudioSystem.getClip()
    withContext(Dispatchers.IO) { decode(bytes).use { clip.open(it) } }
    val onCompletes = ArrayList<() -> Unit>()
    return object : PlayableAudio {
        private var playing = false

        init {
            // STOP comes both from pausing and from reaching the end; only the end has the position at the last frame.
            clip.addLineListener {
                if (it.type == LineEvent.Type.STOP) onMainThread {
                    if (clip.framePosition < clip.frameLength) return@onMainThread
                    playing = false
                    onCompletes.forEach { it() }
                }
            }
        }

        override var volume: Float = 1f
            set(value) {
                field = value
                clip.setVolume(value)
            }

        override var loop: Boolean = false
            set(value) {
                field = value
                if (playing) clip.loop(if (value) Clip.LOOP_CONTINUOUSLY else 0)
            }

        override var isPlaying: Boolean
            get() = playing
            set(value) {
                if (value == playing) return
                playing = value
                if (value) {
                    // Like the web's audio element, playing a finished sound starts it over.
                    if (clip.framePosition >= clip.frameLength) clip.framePosition = 0
                    clip.loop(if (loop) Clip.LOOP_CONTINUOUSLY else 0)
                } else {
                    clip.stop()
                }
            }

        override fun onComplete(action: () -> Unit) {
            onCompletes.add(action)
        }

        override fun stop() {
            isPlaying = false
            clip.framePosition = 0
        }

        override val currentTime: MutableReactive<Duration> = object : BaseListenable(), MutableReactive<Duration> {
            override suspend fun set(value: Duration) {
                clip.microsecondPosition = value.inWholeMicroseconds
            }

            override val state get() = ReactiveState(clip.microsecondPosition.microseconds)

            var remover: (() -> Unit)? = null

            override fun activate() {
                remover = AppState.animationFrame.addListener {
                    if (playing) invokeAllListeners()
                }
            }

            override fun deactivate() {
                remover?.invoke()
                remover = null
            }
        }
    }
}

private suspend fun AudioSource.readBytes(): ByteArray = when (this) {
    is AudioRemote -> {
        val response = fetch(url)
        if (!response.ok) throw IllegalStateException("Failed to load audio $url: HTTP ${response.status}")
        response.blob().toByteArray()
    }
    is AudioRaw -> data.data
    is AudioLocal -> withContext(Dispatchers.IO) { file.file.readBytes() }
    is AudioResource -> withContext(Dispatchers.IO) { loadResource(relativeUrl) }
}

private fun AudioSource.decode(bytes: ByteArray) = try {
    AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
} catch (e: UnsupportedAudioFileException) {
    throw UnsupportedAudioFileException("Can't play $this: the desktop backend plays WAV, AIFF and AU audio only")
}

// Volume is linear amplitude, as on the other platforms; the line's gain is in decibels.
internal fun Line.setVolume(volume: Float) {
    val gain = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
    gain.value = (20 * log10(volume)).coerceIn(gain.minimum, gain.maximum)
}
