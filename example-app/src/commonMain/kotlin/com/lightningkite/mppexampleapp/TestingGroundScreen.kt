package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("testing")
object TestingGroundScreen: Screen {
    override fun ViewWriter.render() {
        scrolls - col {
            h1("Experiments tests")
            centered - sizeConstraints(maxWidth = 10.rem) - image { source = Resources.imagesSolera }
        }
    }
}