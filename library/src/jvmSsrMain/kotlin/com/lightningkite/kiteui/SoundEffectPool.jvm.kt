package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal
import kotlin.time.Duration

/**
 * The JVM target of KiteUI exists to render pages server-side, where there is no audio device and no
 * user to hear anything. Shared UI code that plays a sound is therefore not doing anything wrong on
 * this platform - it is simply doing something that has no meaning here.
 *
 * Everything below is consequently an inert no-op rather than a throw: failing fast would mean every
 * screen that plays a sound effect crashes the server render, which is not a real defect. The
 * returned handles still behave like well-formed audio handles - properties remember what you set
 * and read back - so shared code can drive them without special-casing the server.
 */
public actual class SoundEffectPool actual constructor(concurrency: Int) {
    public actual suspend fun preload(sound: AudioSource) {}

    /** Starts as `isPlaying = true` to mirror the real platforms, where `play` has already begun the sound. */
    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect = object : PlayingSoundEffect {
        override var volume: Float = 1f
        override var isPlaying: Boolean = true
        override fun stop() {
            isPlaying = false
        }
    }

    public actual fun unload(sound: AudioSource) {}
}

/**
 * See [SoundEffectPool] for why this is inert rather than a failure.
 *
 * [PlayableAudio.isPlaying] is a plain flag so that the polling loop in `backgroundAudio` - which
 * calls `play()` and then waits for `isPlaying` to become true - terminates instead of spinning.
 */
public actual suspend fun AudioSource.load(): PlayableAudio = object : PlayableAudio {
    override var volume: Float = 1f
    override var loop: Boolean = false
    override var isPlaying: Boolean = false
    override fun onComplete(action: () -> Unit) {}
    override fun stop() {
        isPlaying = false
    }

    override val currentTime: MutableReactive<Duration> = Signal(Duration.ZERO)
}
