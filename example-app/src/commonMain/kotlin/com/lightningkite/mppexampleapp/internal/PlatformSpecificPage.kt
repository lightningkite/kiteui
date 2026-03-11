package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter

@Routable("/platform-specific")
object PlatformSpecificPage : Page {
    override fun ViewWriter.render(): Unit = run {
        platformSpecific()
    }
}

expect fun ViewWriter.platformSpecific(): Unit