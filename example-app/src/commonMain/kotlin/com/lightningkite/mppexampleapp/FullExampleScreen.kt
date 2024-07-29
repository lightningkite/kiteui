package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.MaterialLikeTheme
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*

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
interface UseFullScreen