@file:OptIn(EvolvingAppearance::class)

package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.EvolvingAppearance
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.debugMode
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.installDebugHandlers
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.docs.DocSearchPage
import com.lightningkite.mppexampleapp.docs.MaterialIconLibraryPage
import com.lightningkite.mppexampleapp.docs.ShorthandBuilderPage
import com.lightningkite.mppexampleapp.internal.RootPage
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.core.*
import kotlin.time.Duration.Companion.seconds

val defaultTheme = Theme.flat2("flat2-default", 0.6.turns)
//val defaultTheme = Theme.shadCnLike("shadcnlike", background = Color.fromHexString("#FAFAFA"))
val appTheme = Signal<Theme>(defaultTheme)

/** Which of the built-in nav variants the example app is currently wearing. */
enum class NavVariant(val display: String) {
    Sidebar("Sidebar"),
    TopBar("Top bar"),
    Tabs("Tabs"),
    Drawer("Drawer"),
}

val navVariant = Signal(NavVariant.TopBar)

class ToastException(override val message: String) : Exception()

@OptIn(ExperimentalKiteUi::class)
fun ViewWriter.app(navigator: PageNavigator) {
    debugMode = true
//    configureTelemetry(navigator)

    context.exceptionHandlers.installDebugHandlers()

    context.exceptionHandlers += ExceptionHandler<ToastException>(1f) {
        toast(it.message)

        return@ExceptionHandler {}
    }

    context.exceptionHandlers += ExceptionHandler(0f) {
        val t = (it.cause as? ToastException) ?: return@ExceptionHandler null

        toast("Error caused by ${t.message}")

        return@ExceptionHandler {}
    }

    Element.Debugger.leakDetect = true

    val appLogo = ImageVector.kiteUiLogo()
    val appName = "KiteUI Example"
    val showNav: ReactiveContext.() -> Boolean = { navigator.currentPage() !is UseFullPage }
    val menuItems: ReactiveContext.() -> List<Nav> = {
        listOf(
            Nav.Link(title = "Home", icon = Icon.home) { HomePage() },
            Nav.Link(title = "Docs", fullTitle = "Documentation", icon = Icon.list) { DocSearchPage },
            Nav.Group(
                title = "Tools", fullTitle = "Tools", icon = Icon.list, count = 2, children = listOf(
                    Nav.Link(title = "Icons", fullTitle = "Material Icons Browser", icon = Icon.search) { MaterialIconLibraryPage },
                    Nav.Link(title = "Builder", fullTitle = "Shorthand Builder", icon = Icon.list) { ShorthandBuilderPage },
                )
            ),
            Nav.Link(title = "Tests", fullTitle = "Test Pages", icon = Icon.home, count = 12) { RootPage },
            Nav.External(title = "Source", icon = Icon.download, to = "https://github.com/lightningkite/kiteui"),
            Nav.Link(
                title = "Settings", icon = Icon.settings,
                to = { SettingsPage() }
            )
        )
    }
    val footerItems: ReactiveContext.() -> List<Nav> = {
        listOf(
            Nav.Custom(
                title = "Nav style",
                wide = { centered.text { ::content { "Style: ${navVariant().display}" } } },
                narrow = { centered.icon(Icon.sort, "Nav style") },
            ),
            Nav.Link(title = "Settings", icon = Icon.settings) { SettingsPage() },
        )
    }
    val actionItems: ReactiveContext.() -> List<Nav> = {
        listOf(
            Nav.Link(title = "Search", icon = Icon.search, count = 0, to = { DocSearchPage }),
            // The escape hatch: content the other four kinds cannot express. Note that it reads
            // navVariant() directly, so it updates on its own rather than waiting for the nav's
            // item list to be rebuilt.
            Nav.Custom(
                title = "Nav style",
                wide = { centered.text { ::content { navVariant().display } } },
                narrow = { centered.icon(Icon.sort, "Nav style") },
            ),
        )
    }

    val rootView = produceExactlyOneElement {
        appBase(navigator) {
            swapView {
                debugName = "swap-nav-variant"
                swapping(
                    current = { navVariant() },
                    views = { variant ->
                        when (variant) {
                            NavVariant.Sidebar -> navSidebar(appLogo, appName, showNav = showNav, menuItems = menuItems, footerItems = footerItems, actionItems = actionItems)
                            NavVariant.TopBar -> navTopBar(appLogo, appName, showNav = showNav, menuItems = menuItems, actionItems = actionItems)
                            NavVariant.Tabs -> navTabs(appLogo, appName, showNav = showNav, menuItems = menuItems, actionItems = actionItems)
                            NavVariant.Drawer -> navDrawer(appLogo, appName, showNav = showNav, menuItems = menuItems, footerItems = footerItems, actionItems = actionItems)
                        }
                    }
                )
            }
        }
    }

//    if (Platform.isDevelopment) {
//        AiDriver.connect(
//            appName = "example",
//            rootView = { rootView },
//            navigator = { navigator },
//        )
//    }
}

interface UseFullPage
