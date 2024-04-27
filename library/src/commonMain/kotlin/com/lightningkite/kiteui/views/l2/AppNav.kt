package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.js.JsName

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


val ViewWriter.appNavFactory by viewWriterAddon<Property<ViewWriter.(AppNav.() -> Unit) -> Unit>>(
    Property(
        ViewWriter::appNavBottomTabs
    )
)

fun ViewWriter.appNav(routes: Routes, setup: AppNav.() -> Unit) {
    appBase(routes) {
        swapView {
            swapping(
                current = { appNavFactory.await() },
                views = { it(this, setup) }
            )
        }
    }
}

fun ViewWriter.appNavHamburger(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    val showMenu = Property(false)
    padded - navSpacing { appNav.existsProperty.await() } - col {
        bar - row {
            setup(appNav)
            toggleButton {
                checked bind showMenu
                icon(Icon.menu, "Open navigation menu")
            }
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { navigator.canGoBack.await() }
                onClick { navigator.goBack() }
            }
            h2 {
                ::content.invoke { navigator.currentScreen.await()?.title?.await() ?: "" }
                wraps = false
                ellipsis = true
            } in gravity(
                Align.Center,
                Align.Center
            ) in weight(1f)
            navGroupActions(appNav.actionsProperty)
            ::exists { appNav.existsProperty.await() }
        }
        expanding - navSpacing { appNav.existsProperty.await() } - stack {
            navigatorView(navigator)
            atStart - nav - onlyWhen(false) { showMenu.await() && appNav.existsProperty.await() }
            scrolls - navGroupColumn(appNav.navItemsProperty, { showMenu set false }) {
                spacing = 0.px
            }
        }
    }
}


fun ViewWriter.appNavTop(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    // Nav 2 top, horizontal
    padded - navSpacing { appNav.existsProperty.await() } - col {
        bar - row {
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { navigator.canGoBack.await() }
                onClick { navigator.goBack() }
            }
            h2 {
                ::content.invoke { navigator.currentScreen.await()?.title?.await() ?: "" }
                wraps = false
                ellipsis = true
            } in gravity(
                Align.Center,
                Align.Center
            )
            space()
            expanding - centered - navGroupTop(appNav.navItemsProperty)
            space()
            centered - navGroupActions(appNav.actionsProperty)
            ::exists { appNav.existsProperty.await() }
        }
        expanding - navigatorView(navigator)
    }
}

fun ViewWriter.appNavBottomTabs(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    padded - navSpacing { appNav.existsProperty.await() } - col {
// Nav 3 top and bottom (top)
        bar - row {
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { navigator.canGoBack.await() }
                onClick { navigator.goBack() }
            }
            h2 {
                ::content.invoke { navigator.currentScreen.await()?.title?.await() ?: "" }
                wraps = false
                ellipsis = true
            } in gravity(
                Align.Center,
                Align.Center
            ) in weight(1f)
            navGroupActions(appNav.actionsProperty)
            ::exists { appNav.existsProperty.await() }
        }
        expanding - navigatorView(navigator)
        //Nav 3 - top and bottom (bottom/tabs)
        navGroupTabs(appNav.navItemsProperty) {
            ::exists { appNav.existsProperty.await() && !SoftInputOpen.await() }
        }
    }
}

fun ViewWriter.appNavTopAndLeft(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    padded - navSpacing { appNav.existsProperty.await() } - col {
// Nav 4 left and top - add dropdown for user info
        bar - row {
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { navigator.canGoBack.await() }
                onClick { navigator.goBack() }
            }
            h2 {
                ::content.invoke { navigator.currentScreen.await()?.title?.await() ?: "" }
                wraps = false
                ellipsis = true
            } in gravity(
                Align.Center,
                Align.Center
            )
            space {} in weight(1f)
            navGroupActions(appNav.actionsProperty)

            ::exists { appNav.existsProperty.await() }
        }
        navSpacing { appNav.existsProperty.await() } - row {
            navSpacing { appNav.existsProperty.await() } - nav - scrolls - navGroupColumn(appNav.navItemsProperty) {
                ::exists { appNav.navItemsProperty.await().size > 1 && appNav.existsProperty.await() }
            }
            expanding - navigatorView(navigator)
        } in weight(1f)
    }
}

@ViewModifierDsl3
fun ViewWriter.navSpacing(showNav: suspend () -> Boolean): ViewWrapper {
    beforeNextElementSetup {
        val theme = currentTheme
        calculationContext.reactiveScope {
            spacing = if (showNav()) theme().navSpacing else 0.px
        }
    }
    return ViewWrapper
}