package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.important
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.math.absoluteValue

@Routable("programmatic-layout-test")
object ProgrammaticLayoutTestPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            h1 { content = "Programmatic Layout Test" }
            val pl: ProgrammaticLayout
            expanding.programmatic {
                pl = this
                val child: Frame
                val move = Signal(true)
                card.frame { child = this; text("Left") }
                card.frame { text("Top Right") }
                card.frame { text("Bottom Right") }
                important.button {
                    col {
                        text("Obnoxious Bouncing")
                        shownWhen { move() }.col {
                            text("We're currently jamming down!")
                            text("We're currently jamming down!")
                            text("We're currently jamming down!")
                        }
                    }
                    onClick { move.value = !move.value }
                }
                delegate = object : ProgrammaticLayoutDelegate {
                    var obx: Double = 0.0
                    var obvx: Double = 10.0
                    var oby: Double = 0.0
                    var obvy: Double = 10.0

                    override fun measure(
                        layout: ProgrammaticLayout,
                        inProgress: ProgrammingLayoutInProgress,
                        within: Size
                    ): Size = within

                    var lastSize: Size = Size(0.0, 0.0)

                    override fun layout(
                        layout: ProgrammaticLayout,
                        inProgress: ProgrammingLayoutInProgress,
                        within: Size
                    ) = with(layout) {
                        with(inProgress) {
                            val s = within
                            if(lastSize != s) {
                                place(children[0], 0.0, 0.0, s.width / 2, s.height)
                                place(children[1], s.width / 2, 0.0, s.width, s.height / 2)
                                place(children[2], s.width / 2, s.height / 2, s.width, s.height)
                                lastSize = s
                            }

                            val measured = measure(children[3], Size(s.width.coerceAtMost(200.0), s.height.coerceAtMost(200.0)))

                            obx += obvx
                            oby += obvy

                            if (obx + measured.width > s.width) obvx = -(obvx.absoluteValue)
                            if (oby + measured.height > s.height) obvy = -(obvy.absoluteValue)
                            if (obx < 0) obvx = (obvx.absoluteValue)
                            if (oby < 0) obvy = (obvy.absoluteValue)
                            obx = obx.coerceIn(0.0, (s.width - measured.width).coerceAtLeast(0.0))
                            oby = oby.coerceIn(0.0, (s.height - measured.height).coerceAtLeast(0.0))

                            place(
                                children[3],
                                obx,
                                oby,
                                obx + measured.width,
                                oby + measured.height
                            )
                        }
                    }
                }
//                launch {
//                    while(true) {
//                        delay(100)
//                        if(move.value) {
//                            invalidateLayout()
//                        }
//                    }
//                }
            }
        }
    }
}

