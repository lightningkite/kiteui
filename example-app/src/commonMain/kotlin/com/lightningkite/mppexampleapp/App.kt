package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.swapView
import com.lightningkite.kiteui.views.direct.swapping
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.docs.DocSearchPage
import com.lightningkite.mppexampleapp.internal.RootPage

//val defaultTheme = brandBasedExperimental("bsa", normalBack = Color.white)
val defaultTheme = Theme.flat("default", Angle(0.55f)).customize("default2", transitionDuration = 0.5.seconds)// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Property<Theme>(defaultTheme)

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator) {
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
                            NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
                            NavLink(title = { "Internal" }, icon = { Icon.home }) { { RootPage } },
                            NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
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
//            NavExternal(
//                title = { "Open Source" },
//                icon = { Icon.download },
//                to = {
//                    val className = mainPageNavigator.currentPage()!!::class.toString().removePrefix("class ")
//                    "https://github.com/lightningkite/kiteui/main/${className}"
//                }
//            )
                    )
                }) }
            )
        }
    }
}

interface UseFullPage
