package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.row

@Routable("test")
class TestScreen: Screen {
    override fun ViewWriter.render() {
        col {
            row {
            }
        }
    }
}