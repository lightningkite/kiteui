package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.reactive
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.important

@Routable("internal/scroll-into-view-test")
object ScrollIntoViewTest : Screen {

    enum class Location { Top, Bottom }
    val jumpTo = Property<Location?>(null)

    override fun ViewWriter.render() {
        scrolls - stack {
            sizeConstraints(height = 500.rem) - col {
                centered - important - button {
                    reactive {
                        if (jumpTo() == Location.Top) {
                            this@button.scrollIntoView(horizontal = null, vertical = Align.Start, animate = true)
                            jumpTo.value = null
                        }
                        println("Finished Check")
                    }
                    text("Scroll To Bottom")
                    onClick { jumpTo.value = Location.Bottom }
                }
                expanding - space()
                centered - important - button {
                    reactive {
                        if (jumpTo() == Location.Bottom) {
                            this@button.scrollIntoView(horizontal = null, vertical = Align.Start, animate = true)
                            jumpTo.value = null
                        }
                        println("Finished Check")
                    }
                    text("Scroll To Top")
                    onClick { jumpTo.value = Location.Top }
                }
            }
        }
    }
}