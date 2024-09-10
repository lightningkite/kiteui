package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.mppexampleapp.docs.DocSearchScreen
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.l2.*

//val defaultTheme = brandBasedExperimental("bsa", normalBack = Color.white)
val defaultTheme = Theme.flat("default", Angle(0.55f))// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Property<Theme>(defaultTheme)

fun ViewWriter.app(navigator: ScreenNavigator, dialog: ScreenNavigator) {
//    rootTheme = { appTheme() }
    setupExampleAppAnalytics()
    appNav(navigator, dialog) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { RootScreen } },
                NavLink({ "Themes" }, { Icon.sync }) { { ThemesScreen } },
                NavLink({ "Navigation" }, { Icon.chevronRight }) { { NavigationTestScreen } },
                NavLink(title = { "Docs" }, icon = { Icon.list }) { { DocSearchScreen } },
                NavGroup("Grouped Test", Icon.settings, listOf(
                    NavLink(title = { "Home Home Home" }, icon = { Icon.home }) { { RootScreen } },
                    NavLink({ "Themes Themes Themes" }, { Icon.sync }) { { ThemesScreen } },
                    NavLink({ "Navigation Navigation Navigation" }, { Icon.chevronRight }) { { NavigationTestScreen } },
                    NavLink(title = { "Docs Docs Docs" }, icon = { Icon.list }) { { DocSearchScreen } },
                ))
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
