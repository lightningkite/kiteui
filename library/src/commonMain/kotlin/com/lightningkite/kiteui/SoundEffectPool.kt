package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioResource
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.shared
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.reactiveScope

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

fun RView<*>.backgroundAudio(audio: AudioResource, playBackgroundAudio: suspend () -> Boolean) {
    val backgroundAudioShared = shared {
        audio.load().apply { volume = 0.1f }
    }
    // Loop and cancel the background sound
    reactiveScope {
        if (playBackgroundAudio()) {
            val backgroundAudio = backgroundAudioShared()
            suspendCoroutineCancellable<Unit> {
                backgroundAudio.onComplete {
                    backgroundAudio.play()
                }
                return@suspendCoroutineCancellable {
                    backgroundAudio.stop()
                }
            }
        }
    }
    // Trigger the background sound once every five seconds until it successfully starts
    reactiveScope {
        if (playBackgroundAudio()) {
            val backgroundAudio = backgroundAudioShared()
            while (true) {
                if (!backgroundAudio.isPlaying) {
                    backgroundAudio.play()
                    delay(5000)
                } else {
                    break
                }
            }
        }
    }
}
