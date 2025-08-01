package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioResource
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

//import com.lightningkite.kiteui.views.reactiveScope

public expect class SoundEffectPool(concurrency: Int = 4) {
    public suspend fun preload(sound: AudioSource)
    public suspend fun play(sound: AudioSource): PlayingSoundEffect
    public fun unload(sound: AudioSource)
}

public interface PlayingSoundEffect {
    public var volume: Float
    public var isPlaying: Boolean
    public fun stop()
}

public expect suspend fun AudioSource.load(): PlayableAudio

public interface PlayableAudio {
    public var volume: Float
    public var loop: Boolean
    public var isPlaying: Boolean
    public fun onComplete(action: ()->Unit)
    public fun stop()
    public fun play() {
        isPlaying = true
    }
}

public fun CalculationContext.backgroundAudio(audio: AudioResource, backgroundVolume: Float, playBackgroundAudio: suspend () -> Boolean) {
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
}
