package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.mppexampleapp.docs.DocSearchScreen
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.bindToPlatform
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.link
import com.lightningkite.kiteui.views.direct.stack
import com.lightningkite.kiteui.views.direct.swapView
import com.lightningkite.kiteui.views.direct.swapping
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.docs.ViewPagerElementScreen
import com.lightningkite.mppexampleapp.internal.CounterScreen
import com.lightningkite.mppexampleapp.internal.LeakCheckerScreen
import com.lightningkite.mppexampleapp.internal.RecyclerViewScreen
import com.lightningkite.mppexampleapp.internal.RootScreen
import com.lightningkite.mppexampleapp.internal.UltraBasicScreen
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

//val defaultTheme = brandBasedExperimental("bsa", normalBack = Color.white)
val defaultTheme = Theme.flat("default", Angle(0.55f))// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Property<Theme>(defaultTheme)

fun ViewWriter.app(navigator: ScreenNavigator, dialog: ScreenNavigator) {
//    rootTheme = { appTheme() }
    RViewHelper.leakDetection = true
    appBase(navigator, dialog) {
        swapView {
            swapping(
                current = { appNavFactory() },
                views = { it(this, {
                    appName = "KiteUI Sample App"
                    ::navItems {
                        listOf(
                            NavLink(title = { "Home" }, icon = { Icon.home }) { { HomeScreen() } },
                            NavLink(title = { "Internal" }, icon = { Icon.home }) { { RootScreen } },
                            NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchScreen } },
                            NavLink(title = { "Empty" }, icon = { Icon.close }) { { Screen.Empty } },
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
                }) }
            )
        }
    }
}

interface UseFullScreen
