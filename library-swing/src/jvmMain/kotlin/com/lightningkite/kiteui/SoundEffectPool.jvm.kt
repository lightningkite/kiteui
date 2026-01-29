package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioSource

actual class SoundEffectPool actual constructor(concurrency: Int) {
    actual suspend fun preload(sound: AudioSource) {
    }

    actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        TODO("Not yet implemented")
    }

    actual fun unload(sound: AudioSource) {
    }
}
actual suspend fun AudioSource.load(): PlayableAudio = TODO()