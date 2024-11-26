package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.MaterialLikeTheme
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.UseFullScreen

@Routable("full-screen")
class FullExampleScreen: Screen, UseFullScreen {

    override fun ViewWriter.render() {
        col {
            h1 { content = "Full Screen!" }
            link {
                text { content = "Go back to root" }
                to = { RootScreen }
            }
        }
    }
}