package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.signal.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.signal.invoke
import com.lightningkite.signal.*

public data class UserInfo(
    public val name: String,
    public val profileImage: ImageVector? = null,
    public val defaultIcon: Icon,
)

public interface AppNav {
    public var appName: String
    public var appIcon: Icon
    public var appLogo: ImageSource
    public var navItems: List<NavElement>
    public var actions: List<NavElement>
    public var exists: Boolean

    public class ByProperty : AppNav {
        public val appNameProperty: Property<String> = Property("My App")
        public override var appName: String by appNameProperty
        public val appIconProperty: Property<Icon> = Property<Icon>(Icon.home)
        public override var appIcon: Icon by appIconProperty
        public val appLogoProperty: Property<ImageSource> = Property<ImageSource>(Icon.home.toImageSource(Color.white))
        public override var appLogo: ImageSource by appLogoProperty
        public val navItemsProperty: Property<List<NavElement>> = Property(listOf<NavElement>())
        public override var navItems: List<NavElement> by navItemsProperty
        public val actionsProperty: Property<List<NavElement>> = Property<List<NavElement>>(listOf())
        public override var actions: List<NavElement> by actionsProperty
        public val existsProperty: Property<Boolean> = Property(true)
        public override var exists: Boolean by existsProperty
    }
}


public val ViewWriter.appNavFactory by rContextAddon<Property<ViewWriter.(AppNav.() -> Unit) -> ViewModifiable>>(
    Property(
        ViewWriter::appNavBottomTabs
    )
)

public fun ViewWriter.appNav(main: PageNavigator, dialog: PageNavigator? = null, setup: AppNav.() -> Unit): ViewModifiable {
    return appBase(main, dialog) {
        swapView {
            debugName = "swapView for appNavFactory"
            swapping(
                current = { appNavFactory() },
                views = { it(this, setup) }
            )
        }
    }
}

public fun ViewWriter.appNavHamburger(setup: AppNav.() -> Unit): ViewModifiable {
    val appNav = AppNav.ByProperty()
    val showMenu = Property(false)
    return OuterSemantic.onNext - col {
        debugName = "outer nav"
        bar - row {
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
            HeaderSemantic.onNext - centered - expanding - text {
                ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            navGroupActions(appNav.actionsProperty)
            ::shown { appNav.existsProperty() }
        }
        expanding - frame {
            debugName = "menu and navigator container"
            navigatorView(pageNavigator)
            atStart - shownWhen(false) { showMenu() && appNav.existsProperty() } - nav - scrolling - navGroupColumn(appNav.navItemsProperty, { showMenu set false }) {
                gap = 0.px
            }
        }
    }
}


public fun ViewWriter.appNavTop(setup: AppNav.() -> Unit): ViewModifiable {
    val appNav = AppNav.ByProperty()
    // Nav 2 top, horizontal
    return OuterSemantic.onNext - col {
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
            ::shown { appNav.existsProperty() }
        }
        expanding - navigatorView(pageNavigator)
    }
}

public fun ViewWriter.appNavBottomTabs(setup: AppNav.() -> Unit): ViewModifiable {
    val appNav = AppNav.ByProperty()
    return OuterSemantic.onNext - col {
        debugName = "outer nav"
// Nav 3 top and bottom (top)
        if (Platform.probablyAppleUser) {
            compact - bar - frame {
                debugName = "apple app bar"
                showOnPrint = false
                setup(appNav)
                atStart - InteractiveSemantic.onNext - button {
                    row {
                        gap = 0.px
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
                ::shown { appNav.existsProperty() }
            }
        } else {
            bar - row {
                debugName = "normal app bar"
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
                ::shown { appNav.existsProperty() }
            }
        }
        expanding - navigatorView(pageNavigator)
        //Nav 3 - top and bottom (bottom/tabs)
        navGroupTabs(appNav.navItemsProperty) {
            debugName = "navGroupTabs"
            showOnPrint = false
            ::shown { appNav.existsProperty() && !AppState.softInputOpen() }
        }
    }
}

public fun ViewWriter.appNavTopAndLeft(setup: AppNav.() -> Unit): ViewModifiable {
    val appNav = AppNav.ByProperty()
    return OuterSemantic.onNext - col {
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

            ::shown { appNav.existsProperty() }
        }
        expanding - OuterSemantic.onNext - row {
            scrolling - navGroupColumn(appNav.navItemsProperty) {
                ::shown { appNav.navItemsProperty().size > 1 && appNav.existsProperty() }
                showOnPrint = false
            }
            expanding - navigatorView(pageNavigator)
        }
    }
}
