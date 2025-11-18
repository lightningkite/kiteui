package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class SoundEffectPool actual constructor(concurrency: Int) {
    actual suspend fun preload(sound: AudioSource) {
    }

    actual suspend fun play(sound: AudioSource, volume: Float, loop: Boolean): PlayingSoundEffect {
        return object: PlayingSoundEffect {
            override var isPlaying: Boolean = true
            override var volume: Float = volume
            override fun stop() { isPlaying = false }
        }
    }

    actual fun unload(sound: AudioSource) {
    }
}
actual suspend fun AudioSource.load(): PlayableAudio = object: PlayableAudio {
    override var isPlaying: Boolean = false
    override var loop: Boolean = false
    override fun onComplete(action: () -> Unit) {}
    override fun stop() {}
    override fun play() {}
    override val currentTime: MutableReactive<Duration> = Signal(0.seconds)
    override var volume: Float = 1f
}