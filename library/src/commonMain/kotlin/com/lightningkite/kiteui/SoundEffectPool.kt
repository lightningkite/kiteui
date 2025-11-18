package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioResource
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

expect class SoundEffectPool(concurrency: Int = 4) {
    suspend fun preload(sound: AudioSource)
    suspend fun play(sound: AudioSource, volume: Float = 1f, loop: Boolean = false): PlayingSoundEffect
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
    var loop: Boolean
    var isPlaying: Boolean
    fun onComplete(action: () -> Unit)
    fun stop()
    fun play() {
        isPlaying = true
    }
    val currentTime: MutableReactive<Duration>
}

fun CalculationContext.backgroundAudio(
    audio: AudioResource,
    backgroundVolume: Float,
    playBackgroundAudio: suspend () -> Boolean
) {
    val backgroundAudioShared = CoroutineScope(coroutineContext).async {
        audio.load().apply {
            volume = backgroundVolume
            loop = true
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
    onRemove {
        AppScope.launch {
            try {
                backgroundAudioShared.await().stop()
            } catch (t: Throwable) {
                /*squish*/
            }
        }
    }
}
