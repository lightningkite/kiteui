package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.UIAudioPool
import com.lightningkite.kiteui.UIAudioSegment
import com.lightningkite.kiteui.models.AudioRemote
import com.lightningkite.kiteui.navigation.KiteUiScreen
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.awaitNotNull
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.launch
import com.lightningkite.kiteui.views.minus
import com.lightningkite.kiteui.views.reactiveScope

@Routable("audio")
class AudioScreen : KiteUiScreen {
    override fun ViewWriter.render() {
        val audioPool = UIAudioPool()
        col {
            fun audioLoadAndPlay(url: String, label: String) = row {
                val sound = Property<UIAudioSegment?>(null)
                weight(1.0f) - button {
                    text("Load $label")
                    onClick {
                        launch {
                            sound.set(audioPool.load(AudioRemote(url)))
                        }
                    }
                }
                weight(1.0f) - button {
                    reactiveScope {
                        enabled = sound.await() != null
                    }
                    text("Play")
                    onClick {
                        launch {
                            sound.awaitNotNull().let { audioPool.play(it) }
                        }
                    }
                }
            }
            audioLoadAndPlay("https://www2.cs.uic.edu/~i101/SoundFiles/CantinaBand3.wav", "Cantina Band")
            audioLoadAndPlay("https://www2.cs.uic.edu/~i101/SoundFiles/StarWars3.wav", "Star Wars")
            audioLoadAndPlay("https://www2.cs.uic.edu/~i101/SoundFiles/taunt.wav", "Taunt")
        }
    }

}