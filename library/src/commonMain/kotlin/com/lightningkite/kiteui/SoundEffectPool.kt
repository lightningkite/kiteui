package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioSource

expect class SoundEffectPool(concurrency: Int = 4) {
    suspend fun preload(sound: AudioSource)
    suspend fun play(sound: AudioSource): PlayingSoundEffect
    fun unload(sound: AudioSource)
}

interface PlayingSoundEffect {
    var volume: Float
    var isPlaying: Boolean
    fun stop()
}

expect suspend fun AudioSource.load(): PlayableAudio

interface PlayableAudio {
    var volume: Float
    var isPlaying: Boolean
    fun onComplete(action: ()->Unit)
    fun stop()
    fun play() {
        isPlaying = true
    }
}
