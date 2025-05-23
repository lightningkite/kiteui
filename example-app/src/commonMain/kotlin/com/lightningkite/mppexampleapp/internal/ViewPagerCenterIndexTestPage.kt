package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.atTopEnd
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.h3
import com.lightningkite.kiteui.views.direct.horizontalRecyclerView
import com.lightningkite.kiteui.views.direct.unpadded
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.RecyclerViewPagingPlacer
import com.lightningkite.signal.Constant
import com.lightningkite.kiteui.views.l2.children

object ViewPagerCenterIndexTestPage : Page {
    override fun ViewWriter.render() = frame {
        val slides = Constant((0..10).toList())
        unpadded - expanding
        val recycler = horizontalRecyclerView {
            placer = RecyclerViewPagingPlacer()
            snapToElements = Align.Center
            scrollSnapStop = true

            children(slides, { it }) { slide ->
                frame {
                    centered - h1 {
                        ::content { "${slide()}" }
                    }
                }
            }
        }

        atTopEnd - h3 {
            ::content { "Center Index: ${recycler.centerIndex()}" }
        }
    }
}