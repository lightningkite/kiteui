package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*

@Routable("/platform-specific")
object PlatformSpecificPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        platformSpecific()
    }
}

expect fun ViewWriter.platformSpecific(): Unit