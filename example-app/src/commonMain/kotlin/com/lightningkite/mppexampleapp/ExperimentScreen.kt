package com.lightningkite.mppexampleapp

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
        col {
            row {
                ::vertical { WindowInfo().width < 100.rem }
                expanding - card - stack { text("A") }
                expanding - card - stack { text("B") }
                expanding - card - stack { text("C") }
            }
        }
    }
}