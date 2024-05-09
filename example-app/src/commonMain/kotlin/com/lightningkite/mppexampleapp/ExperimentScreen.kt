package com.lightningkite.mppexampleapp

import ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("experiment")
object ExperimentScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        scrolls - stack {
            centered - card - col {
                spacing = 2.rem
                card - button { text("A") }
                card - button { text("B") }
                card - button { text("C") }
                card - col {
                    spacing = 1.rem
                    card - button { text("A") }
                    card - button { text("B") }
                    card - button { text("C") }
                }
                card - button { text("A") }
                card - button { text("B") }
                card - button { text("C") }
            }
        }
    }
}