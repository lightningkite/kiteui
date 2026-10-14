package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioSource

public actual class SoundEffectPool actual constructor(concurrency: Int) {
    init { throw NotImplementedError("Compose backend: SoundEffectPool is not implemented yet") }

    public actual suspend fun preload(sound: AudioSource): Unit = TODO("Compose backend: SoundEffectPool")
    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect = TODO("Compose backend: SoundEffectPool")
    public actual fun unload(sound: AudioSource): Unit = TODO("Compose backend: SoundEffectPool")
}

public actual suspend fun AudioSource.load(): PlayableAudio = TODO("Compose backend: AudioSource.load")
