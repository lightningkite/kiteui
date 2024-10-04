package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.load
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.delay

@Routable("performance")
object PerformanceTestScreen : Screen {
    override fun ViewWriter.render() {
        col {
            h1 { content = "Performance Test" }
            text("This screen is hammering the UI by adding and removing thousands of views and updating content.")
            val items = Property((0..5000).toList())
            val property = Property(0)
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
            scrolls - col  {
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