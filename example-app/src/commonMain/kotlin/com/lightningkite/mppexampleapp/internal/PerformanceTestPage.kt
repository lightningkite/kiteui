package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.load
import com.lightningkite.kiteui.models.*
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

@Routable("performance")
object PerformanceTestPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        col {
            h1 { content = "Performance Test" }
            text("This screen is hammering the UI by adding and removing thousands of views and updating content.")
            val items = Signal((0..5000).toList())
            val property = Signal(0)
            load {
                var i = 0
                while(true) {
                    delay(400L)
                    items.value = (i..(5000 + i)).toList()
                    i++
                }
            }
            load {
                while(true) {
                    delay(50L)
                    property.value++
                }
            }
            scrolling.col  {
                forEach(items) {
                    row {
                        icon { source = Icon.add }
                        text { ::content { property().toString() } }
                    }
                }
            }
        }
    }
}