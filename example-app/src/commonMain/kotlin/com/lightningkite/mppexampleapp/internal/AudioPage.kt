package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.SoundEffectPool
import com.lightningkite.kiteui.backgroundAudio
import com.lightningkite.kiteui.load
import com.lightningkite.kiteui.models.AudioRemote
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding

@Routable("audio")
object AudioPage : Page {

    val backgroundSoundPlaying = PersistentProperty("backgroundNoisePlaying", false)

    override fun ViewWriter.render(): Unit = run {
        val soundEffectPool = SoundEffectPool()
        col {
            h1("Audio Testing")
            fun withPool(title: String, audioSource: AudioSource) {
                h2(title)
                row {
                    expanding.button { text("Pool"); onClick { soundEffectPool.play(audioSource) } }
                    expanding.button { text("Direct"); onClick { audioSource.load().play() } }
                }
            }
            withPool("Single Tone", AudioRemote("https://www.audiotars.com/sample_files/sample.mp3"))
//            withPool("Taunt", Resources.audioTaunt)

            toggleButton {
                checked bind backgroundSoundPlaying
                text("Background sound")
            }

//            backgroundAudio(Resources.audioTaunt, 0.1f) { backgroundSoundPlaying() }


//            media {
//                ::info {
//                    MediaView.Info(
//                        sources = listOf(
//                            AudioRemote("https://www.audiotars.com/sample_files/sample.mp3")
//                        ),
////                        scaleType = TODO(),
////                        description = TODO()
//                    )
//                }
//            }
        }
    }

}