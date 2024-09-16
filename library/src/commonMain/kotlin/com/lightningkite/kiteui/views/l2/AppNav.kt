package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.viewDebugTarget
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

fun ViewWriter.appNav(main: ScreenNavigator, dialog: ScreenNavigator? = null, setup: AppNav.() -> Unit) {
    appBase(main, dialog) {
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
                ::visible { screenNavigator.canGoBack.await() }
                onClick { screenNavigator.goBack() }
            }
            h2 {
                ::content.invoke { screenNavigator.currentScreen.await()?.title?.await() ?: "" }
                wraps = false
                ellipsis = true
            } in gravity(
                Align.Center,
                Align.Center
            ) in weight(1f)
            navGroupActions(appNav.actionsProperty)
            ::exists { appNav.existsProperty.await() }
        }
        expanding - navSpacing - stack {
            navigatorView(screenNavigator)
            atStart - navSpacing - onlyWhen(false) { showMenu.await() && appNav.existsProperty.await() } - nav - scrolls - navGroupColumn(appNav.navItemsProperty, { showMenu set false }) {
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
                ::visible { screenNavigator.canGoBack.await() }
                onClick { screenNavigator.goBack() }
            }
            h2 {
                ::content.invoke { screenNavigator.currentScreen.await()?.title?.await() ?: "" }
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
        expanding - navigatorView(screenNavigator)
    }
}

fun ViewWriter.appNavBottomTabs(setup: AppNav.() -> Unit) {
    val appNav = AppNav.ByProperty()
    padded - navSpacing  - col {
// Nav 3 top and bottom (top)
        bar - row {
            showOnPrint = false
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { screenNavigator.canGoBack.await() }
                onClick { screenNavigator.goBack() }
            }
            h2 {
                ::content.invoke { screenNavigator.currentScreen.await()?.title?.await() ?: "" }
                wraps = false
                ellipsis = true
            } in gravity(
                Align.Center,
                Align.Center
            ) in weight(1f)
            navGroupActions(appNav.actionsProperty)
            ::exists { appNav.existsProperty.await() }
        }
        expanding - navigatorView(screenNavigator)
        //Nav 3 - top and bottom (bottom/tabs)
        navGroupTabs(appNav.navItemsProperty) {
            showOnPrint = false
            ::exists { appNav.existsProperty.await() && !SoftInputOpen.await() }
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
                ::visible { screenNavigator.canGoBack.await() }
                onClick { screenNavigator.goBack() }
            }
            h2 {
                ::content.invoke { screenNavigator.currentScreen.await()?.title?.await() ?: "" }
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
        navSpacing  - row {
            navSpacing  - nav - scrolls - navGroupColumn(appNav.navItemsProperty) {
                showOnPrint = false
                ::exists { appNav.navItemsProperty.await().size > 1 && appNav.existsProperty.await() }
            }
            expanding - navigatorView(screenNavigator)
        } in weight(1f)
    }
}
