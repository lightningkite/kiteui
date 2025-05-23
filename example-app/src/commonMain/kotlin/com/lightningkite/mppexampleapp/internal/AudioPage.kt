package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources

@Routable("audio")
object AudioPage : Page {

    val backgroundSoundPlaying = PersistentProperty("backgroundNoisePlaying", false)

    override fun ViewWriter.render(): ViewModifiable = run {
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

            backgroundAudio(Resources.audioTaunt, 0.1f) { backgroundSoundPlaying() }
        }
    }

}