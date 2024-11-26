package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.mppexampleapp.docs.DocSearchScreen
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.*
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

//val defaultTheme = brandBasedExperimental("bsa", normalBack = Color.white)
val defaultTheme = Theme.flat("default", Angle(0.55f))// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Property<Theme>(defaultTheme)

fun ViewWriter.app(navigator: ScreenNavigator, dialog: ScreenNavigator) {
//    rootTheme = { appTheme() }
    appNav(navigator, dialog) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomeScreen() } },
                NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchScreen } },
            )
        }

        ::exists {
            navigator.currentScreen() !is UseFullScreen
        }

        actions = listOf(
            NavLink(
                title = { "Search" },
                icon = { Icon.search },
                destination = { { DocSearchScreen } }
            ),
//            NavExternal(
//                title = { "Open Source" },
//                icon = { Icon.download },
//                to = {
//                    val className = mainScreenNavigator.currentScreen()!!::class.toString().removePrefix("class ")
//                    "https://github.com/lightningkite/kiteui/main/${className}"
//                }
//            )
        )
    }
}

interface UseFullScreen
