package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioSource

actual class SoundEffectPool actual constructor(concurrency: Int) {
    actual suspend fun preload(sound: AudioSource) {
    }

    actual suspend fun play(sound: AudioSource): PlayingSoundEffect = object: PlayingSoundEffect {
        override var isPlaying: Boolean
            get() = false
            set(value) {}
        override var volume: Float
            get() = 0f
            set(value) {}
        override fun stop() {

        }
    }

    actual fun unload(sound: AudioSource) {
    }
}
actual suspend fun AudioSource.load(): PlayableAudio = object: PlayableAudio {
    override var isPlaying: Boolean
        get() = false
        set(value) {}
    override var volume: Float
        get() = 0f
        set(value) {}
    override fun stop() {

    }

    override fun onComplete(action: () -> Unit) {

    }
}