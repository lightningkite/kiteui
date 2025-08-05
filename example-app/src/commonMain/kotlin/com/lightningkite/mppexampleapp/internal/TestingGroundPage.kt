package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("testing")
public object TestingGroundPage: Page {
    public val progressRatio: Signal<Float> = Signal(0f)
    public override fun ViewWriter.render(): ViewModifiable = run {
//        val ratioShared = sharedSuspending {
//            kotlinx.coroutines.delay(1000)
//            ratio.set(ratio.value + 0.1f)
//            println("ratio: $ratio.value")
//            ratio()
//        }



        scrolling - col {
            h1("Experiments test")
            centered - sizeConstraints(maxWidth = 10.rem) - image { source = Resources.imagesSnowyBackground }

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
                sizeConstraints(width = 6.rem, height = 6.rem) -
                        circularProgress {
                    ::ratio {
                        progressRatio.invoke()
                    }
                }
//            }
        }
    }
}