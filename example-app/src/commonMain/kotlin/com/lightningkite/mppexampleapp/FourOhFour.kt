package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.FallbackRoute
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.text

@FallbackRoute
@Routable("/notfound")
class FourOhFour() : Page {
    override fun ElementWriter.CanAddTheme.render() {
        col {
            h1 { content = "Not Found" }
            text { content = "Sorry, couldn't find what you were looking for." }
        }
    }
}