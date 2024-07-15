package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.mppexampleapp.docs.DocSearchScreen
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.*
import kotlin.time.Duration.Companion.seconds

val appTheme = Property<Theme>(Theme.flat("default", Angle(0.55f)).copy(
    body = FontAndStyle(Resources.fontsOpensans),
    title = FontAndStyle(Resources.fontsOpensans, bold = true),
    transitionDuration = 0.25.seconds,
    bodyTransitions = ScreenTransitions.HorizontalSlide,
    dialogTransitions = ScreenTransitions.FadeResize,
))

fun ViewWriter.app(navigator: ScreenNavigator, dialog: ScreenNavigator) {
//    rootTheme = { appTheme() }
    appNav(navigator, dialog) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { RootScreen } },
                NavLink({ "Themes" }, { Icon.sync }) { { ThemesScreen } },
                NavLink({ "Navigation" }, { Icon.chevronRight }) { { NavigationTestScreen } },
                NavLink(title = { "Docs" }, icon = { Icon.list }) { { DocSearchScreen } },
            )
        }

        ::exists {
            navigator.currentScreen.await() !is UseFullScreen
        }

        actions = listOf(
            NavLink(
                title = { "Search" },
                icon = { Icon.search },
                destination = { { DocSearchScreen } }
            )
        )
    }
}
