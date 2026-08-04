package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.NavLink
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.icon
import com.lightningkite.kiteui.views.direct.link
import com.lightningkite.kiteui.views.direct.menuButton
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.onNavigate
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.dynamicThemed
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.themed

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


//fun ViewWriter.



private fun ElementWriter.displayH(element: NavElement2) {
    row {
        centered.navIcon(element)
        centered.text(element.title)
    }
}

private fun ElementWriter.displayV(element: NavElement2) {
    col {
        centered.navIcon(element)
        centered.text(element.title)
    }
}

private fun ElementWriter.displayText(element: NavElement2) {
    text(element.title)
}

private fun ElementWriter.CanAddTheme.outerGroupMenu(
    element: NavElement2,
    direction: PopoverPreferredDirection,
    select: suspend () -> Unit = {},
    content: ElementWriter.() -> Unit
) {
    outer(element, select, { element ->
        menuButton {
            content()
            preferredDirection = direction
            opensMenu {
                themed(ListSemantic).col {
                    for (child in element.children) {
                        outerGroupMenu(child, direction, { context.closePopovers() }) {
                            displayH(child)
                        }
                    }
                }
            }
        }
    }, content)
}

private fun ElementWriter.CanAddTheme.outerGroupIndent(
    element: NavElement2,
    select: suspend () -> Unit = {},
    content: ElementWriter.() -> Unit
) {
    outer(element, select, { element ->
        col {
            gap = 0.px
            padded.content()
            row {
                gap = 0.px
                space()
                expanding.themed(ListSemantic).col {
                    gap = 0.px
                    for (child in element.children) {
                        outerGroupIndent(child, select) {
                            displayH(child)
                        }
                    }
                }
            }
        }
    }, content)
}

private fun ElementWriter.CanAddTheme.outer(
    element: NavElement2,
    select: suspend () -> Unit = {},
    onGroup: ElementWriter.CanAddTheme.(group: NavElement2.Group) -> Unit,
    content: ElementWriter.() -> Unit
) {
    when (element) {
        is NavElement2.Action -> button {
            content()
            onClick { element.onSelect(); select() }
        }

        is NavElement2.Group -> onGroup(element)

        is NavElement2.Link -> selectedIfRouteMatches(element).link {
            content()
            resetsStack = true
            to = element.to
            accessibleLabel = element.fullTitle
            onNavigate { select() }
        }
    }
}

private fun ElementWriter.navIcon(element: NavElement2) {
    icon {
        source = element.icon.copy(width = 1.5.rem, height = 1.5.rem)
        description = ""
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