package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.AudioLocal
import com.lightningkite.kiteui.models.AudioRemote
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.awaitNotNull
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("audio")
object AudioScreen : Screen {
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
        }
    }

}