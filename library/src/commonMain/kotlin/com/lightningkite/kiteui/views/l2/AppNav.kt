package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

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
        val appNameProperty = Property("My App")
        override var appName: String by appNameProperty
        val appIconProperty = Property<Icon>(Icon.home)
        override var appIcon: Icon by appIconProperty
        val appLogoProperty = Property<ImageSource>(Icon.home.toImageSource(Color.white))
        override var appLogo: ImageSource by appLogoProperty
        val navItemsProperty = Property(listOf<NavElement>())
        override var navItems: List<NavElement> by navItemsProperty
        val actionsProperty = Property<List<NavElement>>(listOf())
        override var actions: List<NavElement> by actionsProperty
        val existsProperty = Property(true)
        override var exists: Boolean by existsProperty
    }
}


val ViewWriter.appNavFactory by rContextAddon<Property<ViewWriter.(AppNav.() -> Unit) -> Unit>>(
    Property(
        ViewWriter::appNavBottomTabs
    )
)

fun ViewWriter.appNav(main: PageNavigator, dialog: PageNavigator? = null, setup: AppNav.() -> Unit) {
    appBase(main, dialog) {
        swapView {
            swapping(
                current = { appNavFactory() },
                views = { it(this, setup) }
            )
        }
    }
}

fun ViewWriter.appNavHamburger(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    val showMenu = Property(false)
    padded - navSpacing - col {
        bar - row {
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
            HeaderSemantic.onNext - centered - expanding - text {
                ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            navGroupActions(appNav.actionsProperty)
            ::exists { appNav.existsProperty() }
        }
        expanding - navSpacing - stack {
            navigatorView(pageNavigator)
            atStart - navSpacing - onlyWhen(false) { showMenu() && appNav.existsProperty() } - nav - scrolls - navGroupColumn(appNav.navItemsProperty, { showMenu set false }) {
                spacing = 0.px
            }
        }
    }
}


fun ViewWriter.appNavTop(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    // Nav 2 top, horizontal
    padded - navSpacing  - col {
        bar - row {
            showOnPrint = false
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { pageNavigator.canGoBack() }
                onClick { pageNavigator.goBack() }
            }
            HeaderSemantic.onNext - centered - text {
                ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            space()
            expanding - centered - navGroupTop(appNav.navItemsProperty)
            space()
            centered - navGroupActions(appNav.actionsProperty)
            ::exists { appNav.existsProperty() }
        }
        expanding - navigatorView(pageNavigator)
    }
}

fun ViewWriter.appNavBottomTabs(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    padded - navSpacing  - col {
// Nav 3 top and bottom (top)
        if (Platform.probablyAppleUser) {
            compact - bar - stack {
                showOnPrint = false
                setup(appNav)
                atStart - InteractiveSemantic.onNext - button {
                    row {
                        spacing = 0.px
                        centered - icon(Icon.chevronLeft, "Go Back")
                        centered - text {
                            ::content {
                                pageNavigator.stack().let { it.getOrNull(it.size - 2) }?.title?.let { it().let{ if(it.length > 15) it.take(15) + "\u2026" else it } } ?: ""
                            }
                        }
                    }
                    ::visible { pageNavigator.canGoBack() }
                    onClick { pageNavigator.goBack() }
                }
                centered - HeaderSemantic.onNext - centered - expanding - text {
                    ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                    wraps = false
                    ellipsis = true
                }
                atEnd - navGroupActions(appNav.actionsProperty)
                ::exists { appNav.existsProperty() }
            }
        } else {
            bar - row {
                showOnPrint = false
                setup(appNav)
                if (Platform.current != Platform.Web) button {
                    icon(Icon.arrowBack, "Go Back")
                    ::visible { pageNavigator.canGoBack() }
                    onClick { pageNavigator.goBack() }
                }
                HeaderSemantic.onNext - centered - expanding - text {
                    ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                    wraps = false
                    ellipsis = true
                }
                navGroupActions(appNav.actionsProperty)
                ::exists { appNav.existsProperty() }
            }
        }
        expanding - navigatorView(pageNavigator)
        //Nav 3 - top and bottom (bottom/tabs)
        navGroupTabs(appNav.navItemsProperty) {
            showOnPrint = false
            ::exists { appNav.existsProperty() && !AppState.softInputOpen() }
        }
    }
}

fun ViewWriter.appNavTopAndLeft(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    padded - navSpacing  - col {
// Nav 4 left and top - add dropdown for user info
        bar - row {
            showOnPrint = false
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { pageNavigator.canGoBack() }
                onClick { pageNavigator.goBack() }
            }
            HeaderSemantic.onNext - centered - text {
                ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            expanding - space {}
            navGroupActions(appNav.actionsProperty)

            ::exists { appNav.existsProperty() }
        }
        navSpacing - expanding - row {
            navSpacing  - nav - scrolls - navGroupColumn(appNav.navItemsProperty) {
                ::exists { appNav.navItemsProperty().size > 1 && appNav.existsProperty() }
                showOnPrint = false
            }
            expanding - navigatorView(pageNavigator)
        }
    }
}
