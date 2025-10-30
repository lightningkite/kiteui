package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.docs.DocSearchPage
import com.lightningkite.mppexampleapp.internal.RootPage
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val defaultTheme = Theme.shadCnLike("shadcnlike")
val appTheme = Signal<Theme>(defaultTheme)

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator): ViewModifiable {
    RViewHelper.leakDetection = true
//    return frame {
//        this.forcedSafeInsets = Edges(100.dp)
//        col {
//            text("A")
//            text("B")
//            text("C")
//        }
//    }
    return appNav(navigator, dialog) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
                NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
                NavLink(title = { "Test Pages" }, icon = { Icon.home }) { { RootPage } },
            )
        }

        ::exists {
            navigator.currentPage() !is UseFullPage
        }

        actions = listOf(
            NavLink(
                title = { "Search" },
                icon = { Icon.search },
                destination = { { DocSearchPage } }
            ),
        )
    }
}

interface UseFullPage
