package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioResource
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

//import com.lightningkite.kiteui.views.reactiveScope

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

fun CalculationContext.backgroundAudio(audio: AudioResource, backgroundVolume: Float, playBackgroundAudio: suspend () -> Boolean) {
    val backgroundAudioShared = CoroutineScope(coroutineContext).async {
        audio.load().apply {
            volume = backgroundVolume
            onComplete { play() }
        }
    }
    reactiveSuspending {
        val backgroundAudio = backgroundAudioShared.await()
        if (playBackgroundAudio()) {
            backgroundAudio.play()
            while (!backgroundAudio.isPlaying) {
                delay(5000)
                backgroundAudio.play()
            }
        } else {
            backgroundAudio.stop()
        }
    }
}
