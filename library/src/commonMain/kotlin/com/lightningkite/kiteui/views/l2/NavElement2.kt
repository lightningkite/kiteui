package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.HeaderSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.MediaQuery
import com.lightningkite.kiteui.models.OuterSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.atStart
import com.lightningkite.kiteui.views.bar
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.dynamicThemed
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.nav
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember

public sealed interface NavElement2 {
    public val title: String
    public val icon: Icon

    public data class Group(
        override val title: String,
        override val icon: Icon,
        val children: List<NavElement2>,
    ) : NavElement2

    public data class Link(
        override val title: String,
        val fullTitle: String = title,
        override val icon: Icon,
        val to: () -> Page,
    ) : NavElement2

    public data class Action(
        override val title: String,
        override val icon: Icon,
        val onSelect: suspend () -> Unit,
    ) : NavElement2
}


public fun ViewWriter.navWebStyle(
    appLogo: ImageSource,
    appName: String,
    breakpoint: Dimension = 60.rem,
    showNav: ReactiveContext.() -> Boolean = { true },
    menuItems: ReactiveContext.() -> List<NavElement2> = { listOf() },
    actionItems: ReactiveContext.() -> List<NavElement2> = { listOf() },
) {
    val showMenu = Signal(false)
    themed(OuterSemantic).col {
        debugName = "outer-nav"
        shownWhen(default = false, condition = showNav).bar.row {
            applySafeInsets(bottom = false)
            debugName = "top bar"
            showOnPrint = false

            if (Platform.current != Platform.Web) centered.button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { context.pageNavigator.canGoBack() }
                onClick { context.pageNavigator.goBack() }
            }
            shownForQuery(MediaQuery.MaxWidth(breakpoint)).toggleButton {
                checked bind showMenu
                icon(Icon.menu, "Open navigation menu")
            }
            centered.sizeConstraints(height = 3.rem, width = 4.rem).link {
                rawImage(appLogo, appName)
                to = { context.mainPageNavigator.routes.parse(UrlLikePath(listOf(), mapOf()))!! }
            }

            centered.expanding.themed(HeaderSemantic).text {
                ::content.invoke { context.pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            rowOfExpensive(remember(action = actionItems)) {
                it.renderWithGroupsAsMenus(
                    PopoverPreferredDirection.leftBottom,
                    select = {},
                    content = { it.displayIcon() })
            }
        }
        expanding.themed(OuterSemantic).frame {
            val m = remember(action = menuItems)
            applySafeInsets(top = false)
            debugName = "menu and navigator container"
            themed(OuterSemantic).row {
                shownForQuery(MediaQuery.MinWidth(breakpoint)).frame {
                    shownWhen(false) { showNav() }.sizeConstraints(width = 16.rem).nav.themed(ListSemantic).scrolling.colOfExpensive(m) {
                        it.renderWithGroupsAsMenus(
                            PopoverPreferredDirection.leftBottom,
                            select = {},
                            content = { it.displayH() })
                    }
                }
                expanding.navigatorView(context.pageNavigator)
            }
            atStart.shownWhen(false, transition = ScreenTransition.FromLeft) { showMenu() && showNav() }
                .sizeConstraints(width = 16.rem).nav.themed(ListSemantic).scrolling.colOfExpensive(m) {
                    it.renderWithGroupsAsMenus(
                        PopoverPreferredDirection.leftBottom,
                        select = { showMenu.value = false },
                        content = { it.displayH() })
                }
            reactive {
                if (AppState.windowInfo().width > breakpoint) showMenu.value = false
            }
        }
        frame {
            applySafeInsets(top = false)
        }
    }
}


context(writer: ElementWriter)
private fun NavElement2.displayH() {
    writer.row {
        with(centered) {
            displayIcon()
            text(title)
        }
    }
}

context(writer: ElementWriter)
private fun NavElement2.displayV() {
    writer.col {
        with(centered) {
            displayIcon()
            text(title)
        }
    }
}

context(writer: ElementWriter)
private fun NavElement2.displayIcon() {
    writer.icon {
        source = this@displayIcon.icon.copy(width = 1.5.rem, height = 1.5.rem)
        description = ""
    }
}

context(writer: ElementWriter)
private fun NavElement2.displayText() {
    writer.text(title)
}

context(writer: ElementWriter.CanAddTheme)
private fun NavElement2.renderWithGroupsAsMenus(
    direction: PopoverPreferredDirection,
    select: suspend () -> Unit = {},
    content: ElementWriter.() -> Unit
) {
    render(select, { element ->
        menuButton {
            content()
            preferredDirection = direction
            opensMenu {
                themed(ListSemantic).col {
                    for (child in element.children) {
                        child.renderWithGroupsAsMenus(direction, { context.closePopovers() }) {
                            child.displayH()
                        }
                    }
                }
            }
        }
    }, content)
}

context(writer: ElementWriter.CanAddTheme)
private fun NavElement2.renderWithGroupsAsIndented(
    select: suspend () -> Unit = {},
    content: ElementWriter.() -> Unit
) {
    render(select, { element ->
        col {
            padded.content()
            row {
                space()
                expanding.themed(ListSemantic).col {
                    for (child in element.children) {
                        child.renderWithGroupsAsIndented(select) {
                            child.displayH()
                        }
                    }
                }
            }
        }
    }, content)
}

context(writer: ElementWriter.CanAddTheme)
private fun NavElement2.render(
    select: suspend () -> Unit = {},
    onGroup: ElementWriter.CanAddTheme.(group: NavElement2.Group) -> Unit,
    content: ElementWriter.() -> Unit
) {
    when (this) {
        is NavElement2.Action -> writer.button {
            content()
            onClick { this@render.onSelect(); select() }
        }

        is NavElement2.Group -> writer.onGroup(this@render)

        is NavElement2.Link -> writer.selectedIfRouteMatches(this@render).link {
            content()
            resetsStack = true
            to = this@render.to
            accessibleLabel = this@render.fullTitle
            onNavigate { select() }
        }
    }
}


private fun ElementWriter.CanAddTheme.selectedIfRouteMatches(it: NavElement2.Link): ElementWriter.CanAddScrolling =
    dynamicThemed {
        val matchingPage = context.mainPageNavigator.currentPage()
            ?.let { context.mainPageNavigator.routes.render(it) }?.urlLikePath?.segments == context.mainPageNavigator.routes.render(
            it.to()
        )?.urlLikePath?.segments
        if (matchingPage) SelectedSemantic else UnselectedSemantic
    }