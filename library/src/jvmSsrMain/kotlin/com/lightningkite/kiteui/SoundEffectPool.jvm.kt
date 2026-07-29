package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioSource

public actual class SoundEffectPool actual constructor(concurrency: Int) {
    public actual suspend fun preload(sound: AudioSource) {
    }

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        TODO("Not yet implemented")
    }

    public actual fun unload(sound: AudioSource) {
    }
}
public actual suspend fun AudioSource.load(): PlayableAudio = TODO()