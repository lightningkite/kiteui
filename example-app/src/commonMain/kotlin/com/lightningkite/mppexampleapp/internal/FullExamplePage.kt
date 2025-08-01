package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.UseFullPage

@Routable("full-screen")
public class FullExampleScreen: Page, UseFullPage {

    public override fun ViewWriter.render(): ViewModifiable = run {
        col {
            h1 { content = "Full Screen!" }
            link {
                text { content = "Go back to root" }
                to = { RootPage }
            }
        }
    }
}