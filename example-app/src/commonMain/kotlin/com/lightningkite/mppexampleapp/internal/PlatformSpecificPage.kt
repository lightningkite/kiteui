package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter

@Routable("/platform-specific")
public object PlatformSpecificPage : Page {
    public override fun ViewWriter.render(): ViewModifiable = run {
        platformSpecific()
    }
}

public expect fun ViewWriter.platformSpecific(): ViewModifiable