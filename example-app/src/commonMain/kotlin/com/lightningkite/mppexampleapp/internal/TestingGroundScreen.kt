package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import kotlinx.coroutines.delay
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.Align
import com.lightningkite.mppexampleapp.Resources
import kotlinx.coroutines.launch

@Routable("testing")
object TestingGroundScreen: Screen {
    val progressRatio: Property<Float> = Property(0f)
    override fun ViewWriter.render() {
//        val ratioShared = sharedSuspending {
//            kotlinx.coroutines.delay(1000)
//            ratio.set(ratio.value + 0.1f)
//            println("ratio: $ratio.value")
//            ratio()
//        }



        scrolls - col {
            h1("Experiments test")
            centered - sizeConstraints(maxWidth = 10.rem) - image { source = Resources.imagesSolera }

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
//            stack {
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