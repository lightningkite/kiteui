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
    public fun onComplete(action: () -> Unit)
    public fun stop()
    public fun play() {
        isPlaying = true
    }
    public val currentTime: MutableReactive<Duration>
}

public fun CoroutineScope.backgroundAudio(
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
