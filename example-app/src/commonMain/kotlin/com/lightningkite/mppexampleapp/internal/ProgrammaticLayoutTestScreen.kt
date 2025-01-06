package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactive
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.important
import kotlin.math.absoluteValue

@Routable("programmatic-layout-test")
object ProgrammaticLayoutTestScreen : Screen {
    override fun ViewWriter.render() {
        col {
            h1 { content = "Programmatic Layout Test" }
            val pl: ProgrammaticLayout
            expanding - programmatic {
                pl = this
                card - stack { text("Left") }
                card - stack { text("Top Right") }
                card - stack { text("Bottom Right") }
                important - button { text("Obnoxious Bouncing") }
                reactive {
                    val s = externalSizeLimit()
                    setChildBounds(children[0], Rect(0.0, 0.0, s.width / 2, s.height))
                    setChildBounds(children[1], Rect(s.width / 2, 0.0, s.width, s.height / 2))
                    setChildBounds(children[2], Rect(s.width / 2, s.height / 2, s.width, s.height))
                }
                var obx: Double = 0.0
                var obvx: Double = 1.0
                var oby: Double = 0.0
                var obvy: Double = 1.0
                reactive{
                    rerunOn(AppState.animationFrame)
                    val s = externalSizeLimit()
                    val measured = measureChild(children[3], s)

                    obx += obvx
                    oby += obvy

                    if(obx + measured.width > s.width) obvx = -(obvx.absoluteValue)
                    if(oby + measured.height > s.height) obvy = -(obvy.absoluteValue)
                    if(obx < 0) obvx = (obvx.absoluteValue)
                    if(oby < 0) obvy = (obvy.absoluteValue)
                    obx = obx.coerceIn(0.0, (s.width - measured.width).coerceAtLeast(0.0))
                    oby = oby.coerceIn(0.0, (s.height - measured.height).coerceAtLeast(0.0))

                    setChildBounds(children[3], Rect(
                        obx,
                        oby,
                        obx + measured.width,
                        oby + measured.height
                    ))
                }
            }
            sizeConstraints(height = 3.rem) - text {
                ::content { pl.externalSizeLimit().toString() }
            }
        }
    }
}

