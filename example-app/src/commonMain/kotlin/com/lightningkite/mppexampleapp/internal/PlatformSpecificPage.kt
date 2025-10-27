package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter

@Routable("/platform-specific")
object PlatformSpecificPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        platformSpecific()
    }
}

expect fun ViewWriter.platformSpecific(): ViewModifiable