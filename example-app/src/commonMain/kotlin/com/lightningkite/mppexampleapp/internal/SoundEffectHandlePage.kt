package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.PlayingSoundEffect
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.SoundEffectPool
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.core.*

/**
 * Manual check for the handle [SoundEffectPool.play] hands back.
 *
 * `volume` and `isPlaying` used to throw on read on web, so nothing about a playing sound was
 * observable. Only the ear can confirm the gain node is really in the signal path, hence this page:
 * the readout proves the values come back, and listening proves they mean something.
 *
 * The readout is a snapshot, not live - press Refresh to re-read the handle. That is deliberate:
 * `isPlaying` flipping to false on its own is exactly what you are checking for after the sound
 * finishes.
 */
@Routable("sound-effect-handle")
object SoundEffectHandlePage : Page {

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val pool = SoundEffectPool()
        val handle = Signal<PlayingSoundEffect?>(null)
        val refresh = Signal(0)

        fun reread() {
            refresh.value = refresh.value + 1
        }

        scrolling.col {
            h1("Sound effect handle")

            card.col {
                h2("Readout")
                text {
                    ::content {
                        refresh()
                        handle()?.let { "volume = ${it.volume}, isPlaying = ${it.isPlaying}" }
                            ?: "nothing played yet"
                    }
                }
                button {
                    text("Refresh")
                    onClick { reread() }
                }
            }

            card.col {
                h2("Play")
                button {
                    text("Play taunt")
                    onClick {
                        handle.value = pool.play(Resources.audioTaunt)
                        reread()
                    }
                }
                text("Expect the readout to show volume = 1.0 and isPlaying = true straight away.")
                text("Wait for the sound to finish, press Refresh, and isPlaying should have become false on its own.")
            }

            card.col {
                h2("Volume")
                row {
                    // All exactly representable in float32, so the readout shows round numbers
                    // rather than the likes of 0.10000000149011612.
                    for (level in listOf(1f, 0.5f, 0.25f, 0f)) {
                        expanding.button {
                            text("${(level * 100).toInt()}%")
                            onClick {
                                handle.value?.volume = level
                                reread()
                            }
                        }
                    }
                }
                text("Play the taunt, then drop the volume while it is still going - it must get quieter, not just report a smaller number.")
            }

            card.col {
                h2("Stop")
                row {
                    expanding.button {
                        text("stop()")
                        onClick {
                            handle.value?.stop()
                            reread()
                        }
                    }
                    expanding.button {
                        text("isPlaying = false")
                        onClick {
                            handle.value?.isPlaying = false
                            reread()
                        }
                    }
                }
                text("Either one must silence the sound and leave isPlaying reading false. Pressing them again must not throw.")
            }
        }
    }
}
