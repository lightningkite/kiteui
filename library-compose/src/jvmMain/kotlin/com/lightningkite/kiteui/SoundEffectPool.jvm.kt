package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.utils.SuspendCache
import com.lightningkite.kiteui.views.loadResource
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.ReactiveState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

// Plays through javax.sound, which decodes WAV, AIFF and AU only.
// Opening a clip takes a noticeable moment and holds an audio line until closed, so, like Android's SoundPool,
// at most `concurrency` clips are open, and an idle one is reused when its sound plays again.
public actual class SoundEffectPool actual constructor(private val concurrency: Int) {
    private val loaded = SuspendCache<AudioSource, Pcm> { it.decodePcm() }

    private inner class PooledClip(val sound: AudioSource, val clip: Clip) {
        var current: Effect? = null
        val idle get() = current?.going != true
    }

    // Least recently started first.
    private val clips = ArrayList<PooledClip>()

    private inner class Effect(val pooled: PooledClip) : PlayingSoundEffect {
        var going = true

        override var volume: Float = 1f
            set(value) {
                field = value
                if (pooled.current === this) pooled.clip.setVolume(value)
            }

        // As on the web, a stopped sound can't be restarted, so setting this to true is ignored.
        override var isPlaying: Boolean
            get() = going
            set(value) {
                if (!value) stop()
            }

        override fun stop() {
            if (!going) return
            going = false
            pooled.clip.stop()
        }
    }

    public actual suspend fun preload(sound: AudioSource) {
        loaded.get(sound)
    }

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        val pcm = loaded.get(sound)
        val pooled = clips.find { it.sound == sound && it.idle } ?: run {
            val clip = withContext(Dispatchers.IO) { pcm.openClip() }
            while (clips.size >= concurrency) close(clips.firstOrNull { it.idle } ?: clips.first())
            PooledClip(sound, clip).also { pooled ->
                // STOP also arrives, late, from stopping a sound, possibly after its clip has started over for another play.
                // Only the end of the sound has the position at the last frame.
                clip.addLineListener {
                    if (it.type == LineEvent.Type.STOP) onMainThread {
                        if (clip.isOpen && clip.framePosition >= clip.frameLength) pooled.current?.going = false
                    }
                }
            }
        }
        clips.remove(pooled)
        clips.add(pooled)
        val effect = Effect(pooled)
        pooled.current = effect
        pooled.clip.framePosition = 0
        pooled.clip.setVolume(effect.volume)
        pooled.clip.start()
        return effect
    }

    private fun close(pooled: PooledClip) {
        pooled.current?.going = false
        pooled.current = null
        clips.remove(pooled)
        AppScope.launch(Dispatchers.IO) { pooled.clip.close() }
    }

    public actual fun unload(sound: AudioSource) {
        clips.filter { it.sound == sound }.forEach { close(it) }
        loaded.forget(sound)
    }
}

public actual suspend fun AudioSource.load(): PlayableAudio = LoadedAudio(decodePcm())

private class LoadedAudio(private val pcm: Pcm) : PlayableAudio {
    private var playing = false

    // The position, in frames, while no clip is open.
    private var position = 0

    // A clip holds an audio line, so one is open only while playing.
    private var clip: Clip? = null

    // Counts clip requests, so that a clip that finishes opening after a pause is closed instead of played.
    private var requests = 0

    private val onCompletes = ArrayList<() -> Unit>()

    private fun openClip() {
        val request = ++requests
        AppScope.launch(Dispatchers.Main) {
            val clip = withContext(Dispatchers.IO) { pcm.openClip() }
            if (request != requests) {
                withContext(Dispatchers.IO) { clip.close() }
                return@launch
            }
            // A clip that's no longer current was paused or stopped; one that is current stopped by reaching the end.
            clip.addLineListener {
                if (it.type == LineEvent.Type.STOP) onMainThread {
                    if (this@LoadedAudio.clip !== clip) return@onMainThread
                    closeClip()
                    position = pcm.frames
                    playing = false
                    onCompletes.forEach { it() }
                }
            }
            clip.framePosition = position
            clip.setVolume(volume)
            clip.loop(if (loop) Clip.LOOP_CONTINUOUSLY else 0)
            this@LoadedAudio.clip = clip
        }
    }

    private fun closeClip() {
        requests++
        val clip = clip ?: return
        position = currentFrame()
        this.clip = null
        AppScope.launch(Dispatchers.IO) { clip.close() }
    }

    // A clip's frame position counts every frame played since it was positioned, so it passes the end when looping.
    private fun currentFrame(): Int = clip?.let { (it.longFramePosition % pcm.frames).toInt() } ?: position

    override var volume: Float = 1f
        set(value) {
            field = value
            clip?.setVolume(value)
        }

    override var loop: Boolean = false
        set(value) {
            field = value
            clip?.loop(if (value) Clip.LOOP_CONTINUOUSLY else 0)
        }

    override var isPlaying: Boolean
        get() = playing
        set(value) {
            if (value == playing) return
            playing = value
            if (value) {
                // Like the web's audio element, playing a finished sound starts it over.
                if (position >= pcm.frames) position = 0
                openClip()
            } else {
                closeClip()
            }
        }

    override fun onComplete(action: () -> Unit) {
        onCompletes.add(action)
    }

    override fun stop() {
        isPlaying = false
        position = 0
        time.changed()
    }

    private val time = object : BaseListenable(), MutableReactive<Duration> {
        fun changed() = invokeAllListeners()

        override suspend fun set(value: Duration) {
            val frame = (value.toDouble(DurationUnit.SECONDS) * pcm.format.frameRate).toInt().coerceIn(0, pcm.frames)
            clip?.framePosition = frame
            position = frame
            changed()
        }

        override val state get() = ReactiveState((currentFrame() / pcm.format.frameRate.toDouble()).seconds)

        private var remover: (() -> Unit)? = null

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

    override val currentTime: MutableReactive<Duration> get() = time
}

private class Pcm(val format: AudioFormat, val data: ByteArray) {
    val frames get() = data.size / format.frameSize
    fun openClip(): Clip = AudioSystem.getClip().also { it.open(format, data, 0, data.size) }
}

// Lines have a volume control only for PCM of at most 16 bits and two channels (see the JDK's DirectAudioDevice),
// so every sound is decoded to 16-bit PCM.
private suspend fun AudioSource.decodePcm(): Pcm {
    val bytes = readBytes()
    return withContext(Dispatchers.IO) {
        decode(bytes).use { encoded ->
            val channels = encoded.format.channels
            if (channels > 2) throw UnsupportedAudioFileException("Can't play $this: the desktop backend plays mono and stereo audio only")
            val format = AudioFormat(encoded.format.sampleRate, 16, channels, true, false)
            AudioSystem.getAudioInputStream(format, encoded).use { Pcm(format, it.readAllBytes()) }
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
