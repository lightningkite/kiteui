package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.isDevelopment
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

val defaultTheme = Theme.flat2("flat2default", 0.6.turns).customize(
    newId = "asdf",
    transitionDuration = 0.2.seconds,
    bodyTransitions = ScreenTransitions.HorizontalSlide
)
//val defaultTheme = Theme.shadCnLike("shadcnlike", background = Color.white)
val appTheme = Signal<Theme>(defaultTheme)

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator): Unit {
    RViewHelper.leakDetection = true
    configureTelemetry(navigator)
    val rootView = produceOne {
        appNav(navigator, dialog) {
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

    if (Platform.isDevelopment) {
        AiDriver.connect(
            appName = "example",
            rootView = { rootView },
            navigator = { navigator },
        )
    }
}

interface UseFullPage
