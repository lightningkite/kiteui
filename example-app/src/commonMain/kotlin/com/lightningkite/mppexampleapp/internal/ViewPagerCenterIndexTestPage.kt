package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.atTopEnd
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.h3
import com.lightningkite.kiteui.views.direct.horizontalRecyclerView
import com.lightningkite.kiteui.views.direct.unpadded
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.RecyclerViewPagingPlacer
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

object ViewPagerCenterIndexTestPage : Page {
    override fun ElementWriter.CanAddTheme.render() {
        frame {
            val slides = Constant((0..10).toList())
            val recycler = expanding.unpadded.horizontalRecyclerView {
                placer = RecyclerViewPagingPlacer()
                snapToElements = Align.Center
                scrollSnapStop = true

                children(slides, { it }) { slide ->
                    frame {
                        centered.h1 {
                            ::content { "${slide()}" }
                        }
                    }
                }
            }

            atTopEnd.h3 {
                ::content { "Center Index: ${recycler.centerIndex()}" }
            }
        }
    }
}