package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.load
import com.lightningkite.readable.Property
import com.lightningkite.readable.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.delay

@Routable("performance")
object PerformanceTestPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
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