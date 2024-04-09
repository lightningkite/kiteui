package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.FallbackRoute
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.content
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.text

@FallbackRoute
class FourOhFour() : Screen {
    override fun ViewWriter.render() = col {
        h1 { content = "Not Found" }
        text { content = "Sorry, couldn't find what you were looking for." }
    }
}