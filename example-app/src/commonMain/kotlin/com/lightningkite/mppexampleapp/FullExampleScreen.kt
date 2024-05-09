package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.MaterialLikeTheme
import com.lightningkite.kiteui.navigation.Screen
import ViewWriter
import com.lightningkite.kiteui.views.debugNext
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.setTheme

@Routable("full-screen")
class FullExampleScreen: Screen, UseFullScreen {

    override fun ViewWriter.render() {
        debugNext
        col {
            h1 { content = "Full Screen!" }
            link {
                text { content = "Go back to root" }
                to = { RootScreen }
            }
        } in setTheme { MaterialLikeTheme.randomDark() }
    }
}
interface UseFullScreen