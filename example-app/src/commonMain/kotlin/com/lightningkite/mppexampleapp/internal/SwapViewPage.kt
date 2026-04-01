package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("swapview")
object SwapViewPage : Page {
    override fun ElementWriter.CanAddTheme.render() {
        val clock = reactiveProcess {
            var tick = 0
            while (true) {
                emit(tick++)
                delay(2000)  // 2 second intervals for better visibility
            }
        }

        col {
            // Fade transition
            row {
                text("Fade:")
                swapView {
                    swapping(
                        transition = { ScreenTransition.Fade },
                        current = { clock() },
                        views = { tick -> h1("${(tick % 3) + 1}") }
                    )
                }
            }

            // Push transition (slide left)
            row {
                text("Push:")
                swapView {
                    swapping(
                        transition = { ScreenTransition.Push },
                        current = { clock() },
                        views = { tick -> h1("${(tick % 3) + 1}") }
                    )
                }
            }

            // Pop transition (slide right)
            row {
                text("Pop:")
                swapView {
                    swapping(
                        transition = { ScreenTransition.Pop },
                        current = { clock() },
                        views = { tick -> h1("${(tick % 3) + 1}") }
                    )
                }
            }

            // PullUp transition
            row {
                text("PullUp:")
                swapView {
                    swapping(
                        transition = { ScreenTransition.PullUp },
                        current = { clock() },
                        views = { tick -> h1("${(tick % 3) + 1}") }
                    )
                }
            }

            // PullDown transition
            row {
                text("PullDown:")
                swapView {
                    swapping(
                        transition = { ScreenTransition.PullDown },
                        current = { clock() },
                        views = { tick -> h1("${(tick % 3) + 1}") }
                    )
                }
            }

            // GrowFade transition
            row {
                text("GrowFade:")
                swapView {
                    swapping(
                        transition = { ScreenTransition.GrowFade },
                        current = { clock() },
                        views = { tick -> h1("${(tick % 3) + 1}") }
                    )
                }
            }

            // ShrinkFade transition
            row {
                text("ShrinkFade:")
                swapView {
                    swapping(
                        transition = { ScreenTransition.ShrinkFade },
                        current = { clock() },
                        views = { tick -> h1("${(tick % 3) + 1}") }
                    )
                }
            }
        }
    }
}