package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("audio")
object AudioScreen : Screen {

    val backgroundSoundPlaying = PersistentProperty("backgroundNoisePlaying", false)

    override fun ViewWriter.render() {
        val soundEffectPool = SoundEffectPool()
        col {
            h1("Audio Testing")
            fun withPool(title: String, audioSource: AudioSource) {
                h2(title)
                row {
                    expanding - button { text("Pool"); onClick { soundEffectPool.play(audioSource) } }
                    expanding - button { text("Direct"); onClick { audioSource.load().play() } }
                }
            }
//            withPool("CantinaBand3.wav", "https://www2.cs.uic.edu/~i101/SoundFiles/CantinaBand3.wav")
//            withPool("StarWars3.wav", "https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav")
            withPool("Taunt", Resources.audioTaunt)

            toggleButton {
                checked bind backgroundSoundPlaying
                text("Background sound")
            }

            val backgroundAudioShared = shared {
                Resources.audioTaunt.load().apply { volume = 0.1f }
            }
            // Loop and cancel the background sound
            reactiveScope {
                if (backgroundSoundPlaying()) {
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
                if (backgroundSoundPlaying()) {
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
    }

}