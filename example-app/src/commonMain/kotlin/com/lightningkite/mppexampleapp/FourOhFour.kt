package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.FallbackRoute
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.text

@FallbackRoute
public class FourOhFour() : Page {
    public override fun ViewWriter.render() = col {
        h1 { content = "Not Found" }
        text { content = "Sorry, couldn't find what you were looking for." }
    }
}