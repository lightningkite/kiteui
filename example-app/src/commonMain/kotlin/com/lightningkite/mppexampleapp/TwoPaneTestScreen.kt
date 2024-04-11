package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("twopane")
object TwoPaneTestScreen : Screen {
    override fun ViewWriter.render() {
        this.twoPane(
            setup = {},
            leftPaneSize = 25.rem,
            rightPaneMinSize = 25.rem,
            left = {
                hover - col {
                    repeat(100) {
                        text("Left Pane")
                    }
                }
            },
            right = {
                col {
                    repeat(100) {
                        text("Right Pane")
                    }
                }
            },
        )
    }
}