package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.PopoverSemantic
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.SemanticOverrides
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.ThemeDerivation.Companion.invoke
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.models.override
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.l2.rawPopover
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("testing")
object TestingGroundPage: Page {
    val progressRatio: Signal<Float> = Signal(0f)
    override fun ViewWriter.render(): Unit = run {
//        val ratioShared = sharedSuspending {
//            kotlinx.coroutines.delay(1000)
//            ratio.set(ratio.value + 0.1f)
//            println("ratio: $ratio.value")
//            ratio()
//        }



        scrolling.col {
            h1("Experiments test")
            centered.sizeConstraints(maxWidth = 10.rem).image { source = Resources.imagesSnowyBackground }

            launch {
                while (true) {
                    delay(2000L)
                    progressRatio.value += 0.01f
                    if (progressRatio.value > 1f) {
                        progressRatio.value = 0f
                    }
                }
            }

            progressBar {
                ::ratio {
                    progressRatio.invoke()
                }
            }
//            frame {
                sizeConstraints(width = 6.rem, height = 6.rem).circularProgress {
                    ::ratio {
                        progressRatio.invoke()
                    }
                }
//            }

            val input = Signal("")

            col {
                val anchorTarget = this
                fieldTheme.textArea {
                    content.bind(input)

                }
                reactive {
                    if (input() == "test") {
                        var willRemove: RView? = null
                        openPopover(PopoverPreferredDirection.belowCenter, anchorTarget) {
                            text("Anchored to Target!")
                        }
                    } else closeThisPopover()
                }
                text("Is this behind it or what")
            }

        }
    }
}