package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.icon
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.core.*

data class UserInfo(
    val name: String,
    val profileImage: ImageVector? = null,
    val defaultIcon: Icon,
)

interface AppNav {
    var appName: String
    var appIcon: Icon
    var appLogo: ImageSource
    var navItems: List<NavElement>
    var actions: List<NavElement>
    var exists: Boolean

    class ByProperty : AppNav {
        val appNameProperty = Signal("My App")
        override var appName: String by appNameProperty
        val appIconProperty = Signal<Icon>(Icon.home)
        override var appIcon: Icon by appIconProperty
        val appLogoProperty = Signal<ImageSource>(Icon.home.toImageSource(Color.white))
        override var appLogo: ImageSource by appLogoProperty
        val navItemsProperty = Signal(listOf<NavElement>())
        override var navItems: List<NavElement> by navItemsProperty
        val actionsProperty = Signal<List<NavElement>>(listOf())
        override var actions: List<NavElement> by actionsProperty
        val existsProperty = Signal(true)
        override var exists: Boolean by existsProperty
    }
}


val ElementContext.appNavFactory by contextAddon<Signal<ViewWriter.(AppNav.() -> Unit) -> Unit>>(
    Signal(
        ViewWriter::appNavBottomTabs
    )
)

fun ElementWriter.appNav(main: PageNavigator, dialog: PageNavigator? = null, setup: AppNav.() -> Unit) {
    return appBase(main, dialog) {
        swapView {
            debugName = "swap-appNavFactory"
            swapping(
                current = { context.appNavFactory() },
                views = { it(this, setup) }
            )
        }
    }
}

fun ViewWriter.appNavHamburger(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    val showMenu = Signal(false)
    themed(OuterSemantic).col {
        debugName = "outer-nav"
        bar.row {
            applySafeInsets(bottom = false)
            debugName = "top bar"
            showOnPrint = false
            setup(appNav)
            toggleButton {
                checked bind showMenu
                icon(Icon.menu, "Open navigation menu")
            }
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { pageNavigator.canGoBack() }
                onClick { pageNavigator.goBack() }
            }
            centered.expanding.themed(HeaderSemantic).text {
                ::content.invoke { context.pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            navGroupActions(appNav.actionsProperty)
            ::shown { appNav.existsProperty() }
        }
        expanding.frame {
            applySafeInsets(top = false)
            debugName = "menu and navigator container"
            navigatorView(pageNavigator)
            atStart.shownWhen(false) { showMenu() && appNav.existsProperty() }.nav.scrolling.navGroupColumn(appNav.navItemsProperty, { showMenu set false }) {
                gap = 0.px
            }
        }
    }
}


fun ViewWriter.appNavTop(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    // Nav 2 top, horizontal
    themed(OuterSemantic).col {
        bar.row {
            applySafeInsets(bottom = false)
            showOnPrint = false
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { pageNavigator.canGoBack() }
                onClick { pageNavigator.goBack() }
            }
            centered.themed(HeaderSemantic).text {
                ::content { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            space()
            centered.expanding.navGroupTop(appNav.navItemsProperty)
            space()
            centered.navGroupActions(appNav.actionsProperty)
            ::shown { appNav.existsProperty() }
        }
        beforeSetup { applySafeInsets(top = false) }.expanding.navigatorView(pageNavigator)
    }
}

fun ElementWriter.CanAddTheme.appNavBottomTabs(setup: AppNav.() -> Unit): Unit {
    val appNav = AppNav.ByProperty()
    themed(OuterSemantic).col {
        debugName = "outer nav"
// Nav 3 top and bottom (top)
        if (Platform.probablyAppleUser) {
            compact.bar.frame {
                applySafeInsets(bottom = false)
                debugName = "apple app bar"
                showOnPrint = false
                setup(appNav)
                atStart.onNext(InteractiveSemantic).button {
                    row {
                        gap = 0.px
                        centered.icon(Icon.chevronLeft, "Go Back")
                        centered.text {
                            ::content {
                                pageNavigator.stack().let { it.getOrNull(it.size - 2) }?.title?.let { it().let { if (it.length > 15) it.take(15) + "\u2026" else it } } ?: ""
                            }
                        }
                    }
                    ::visible { pageNavigator.canGoBack() }
                    onClick { pageNavigator.goBack() }
                }
                centered.expanding.themed(HeaderSemantic).text {
                    ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                    wraps = false
                    ellipsis = true
                }
                atEnd.navGroupActions(appNav.actionsProperty)
                ::shown { appNav.existsProperty() }
            }
        } else {
            bar.row {
                applySafeInsets(bottom = false)
                debugName = "normal app bar"
                showOnPrint = false
                setup(appNav)
                if (Platform.current != Platform.Web) button {
                    icon(Icon.arrowBack, "Go Back")
                    ::visible { pageNavigator.canGoBack() }
                    onClick { pageNavigator.goBack() }
                }
                centered.expanding.themed(HeaderSemantic).text {
                    ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                    wraps = false
                    ellipsis = true
                }
                navGroupActions(appNav.actionsProperty)
                ::shown { appNav.existsProperty() }
            }
        }
        val tabsHandleBottom = remember { appNav.existsProperty() && !AppState.softInputOpen() }
        beforeSetup {
            // Apply bottom safe insets when keyboard is open, since the tab bar
            // (which normally handles the bottom inset) is hidden during keyboard input.
            applySafeInsets { edges ->
                Edges(
                    left = edges.left,
                    top = if (appNav.existsProperty()) 0.px else edges.top,
                    right = edges.right,
                    bottom = if (!tabsHandleBottom()) edges.bottom else 0.px,
                )
            }
        }.expanding.navigatorView(pageNavigator)
        //Nav 3 - top and bottom (bottom/tabs)
        nav.navGroupTabs(appNav.navItemsProperty) {
            applySafeInsets(top = false)
            debugName = "navGroupTabs"
            showOnPrint = false
            ::shown { tabsHandleBottom() }
        }
    }
}

fun ElementWriter.CanAddTheme.appNavTopAndLeft(setup: AppNav.() -> Unit): Unit {
    val appNav = AppNav.ByProperty()
    themed(OuterSemantic).col {
// Nav 4 left and top - add dropdown for user info
        bar.row {
            applySafeInsets(bottom = false)
            showOnPrint = false
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { pageNavigator.canGoBack() }
                onClick { pageNavigator.goBack() }
            }
            centered.themed(HeaderSemantic).text {
                ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            expanding.space {}
            navGroupActions(appNav.actionsProperty)

            ::shown { appNav.existsProperty() }
        }
        expanding.themed(OuterSemantic).row {
            beforeSetup { applySafeInsets(right = false) }.nav.scrolling.navGroupColumn(appNav.navItemsProperty) {
                ::shown { appNav.navItemsProperty().size > 1 && appNav.existsProperty() }
                showOnPrint = false
            }
            beforeSetup { applySafeInsets(top = false) }.expanding.navigatorView(pageNavigator)
        }
    }
}
