@file:OptIn(ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.EvolvingAppearance
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.models.CompactSemantic
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Edges
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
import com.lightningkite.kiteui.models.dp
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
import com.lightningkite.kiteui.views.atTopEnd
import com.lightningkite.kiteui.views.bar
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.compact
import com.lightningkite.kiteui.views.critical
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.dynamicThemed
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.nav
import com.lightningkite.kiteui.views.padding
import com.lightningkite.kiteui.views.textSize
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember

/**
 * An app's navigation: what is in it, what state it is in, and the pieces that draw it.
 *
 * Construct one, then place its parts wherever your layout wants them:
 *
 * ```kotlin
 * val nav = Nav(this, appLogo, "My App", menuItems = { listOf(Nav.Link(...), ...) })
 * themed(OuterSemantic).col {
 *     nav.appBar(this, menuButtonFor = MediaQuery.MaxWidth(40.rem))
 *     expanding.frame {
 *         navigatorView(context.pageNavigator)
 *         nav.drawer(this, closeAbove = 40.rem)
 *     }
 * }
 * ```
 *
 * The four `nav*` variants below are built from exactly these parts and nothing else - read one of
 * them as a worked example. Reach for them first; assemble by hand when your layout does not fit
 * any of the four.
 *
 * One instance drives every part you place, which is the point: they share the selection, the
 * drawer's open state, and which groups are expanded, so a drawer and a rail cannot disagree.
 *
 * @param writer The writer the nav is built under. It is needed at construction because the
 *   selection is derived from the page navigator reachable through it.
 * @param footerItems Pinned to the far end of the rail and drawer, below a spacer. Account,
 *   settings, sign out - the things that are not destinations within the app's content. Only the
 *   parts with somewhere to put them ([rail], [drawer]) show these.
 */
@ExperimentalKiteUi
public class Nav(
    writer: ViewWriter,
    public val appLogo: ImageSource,
    public val appName: String,
    public val showNav: ReactiveContext.() -> Boolean = { true },
    menuItems: ReactiveContext.() -> List<Item> = { listOf() },
    footerItems: ReactiveContext.() -> List<Item> = { listOf() },
    actionItems: ReactiveContext.() -> List<Item> = { listOf() },
) {

    // -----------------------------------------------------------------------------------------
    // What is in the nav
    // -----------------------------------------------------------------------------------------

    /**
     * A destination, grouping, or action in an app's navigation.
     *
     * The same list is handed to any of the parts below; each one decides how to present it at the
     * width it has. Nothing here describes appearance - a [Group] becomes an expanding section in
     * a rail and a dropdown in a top bar, from the same declaration.
     */
    public sealed interface Item {
        public val title: String
        public val icon: Icon

        /**
         * A badge on this item - unread messages, pending approvals.
         *
         * - `null` shows nothing.
         * - `0` shows a bare dot, for "there is something here" without a number.
         * - Anything higher shows the number.
         *
         * This is a plain value rather than a reactive one, so a badge that changes rebuilds the
         * nav rather than just the badge. That is fine for something that ticks over occasionally
         * and wasteful for something that ticks every second.
         */
        public val count: Int?
    }

    /**
     * A named set of destinations. Presented inline with a disclosure control where there is
     * vertical room for it, and as a popover menu where there is not.
     */
    public data class Group(
        override val title: String,
        override val icon: Icon,
        val children: List<Item>,
        /** See [Link.fullTitle]. */
        val fullTitle: String = title,
        override val count: Int? = null,
    ) : Item

    /**
     * A destination.
     *
     * @param title The label shown in the nav.
     * @param fullTitle A longer form used as the accessible label, for when [title] is abbreviated
     *   to fit a rail or a tab.
     */
    public data class Link(
        override val title: String,
        val fullTitle: String = title,
        override val icon: Icon,
        override val count: Int? = null,
        val to: () -> Page,
    ) : Item

    /** A destination outside the app - docs, a status page, a support portal. */
    public data class External(
        override val title: String,
        val fullTitle: String = title,
        override val icon: Icon,
        override val count: Int? = null,
        val to: String,
    ) : Item

    /** Something that happens in place - sign out, open a dialog - rather than a destination. */
    public data class Action(
        override val title: String,
        override val icon: Icon,
        override val count: Int? = null,
        val onSelect: suspend () -> Unit,
    ) : Item

    /**
     * An escape hatch: your own content in a nav slot. A user avatar with a name beside it, a
     * search field, a workspace switcher - anything the other four cannot express.
     *
     * You get two forms because the parts ask for two. [wide] is used where there is room for a
     * label beside an icon (the labeled rail, the drawer, a popover menu, inline top-bar links);
     * [narrow] where there is not (the compact rail, the tab bar, bar actions). [narrow] defaults
     * to [wide], which is right when your content is already small.
     *
     * Both run in a plain frame, so the surrounding part contributes nothing but position -
     * selection highlighting, badges, and accessible labelling are yours to handle. [title] is
     * still used as the accessible label for the slot.
     */
    public data class Custom(
        override val title: String,
        override val icon: Icon = Icon.moreHoriz,
        val wide: ViewWriter.() -> Unit,
        val narrow: ViewWriter.() -> Unit = wide,
    ) : Item {
        /** [Custom] draws its own content, so there is nowhere to hang a badge. */
        override val count: Int? get() = null
    }

    // -----------------------------------------------------------------------------------------
    // State, shared by every part
    // -----------------------------------------------------------------------------------------

    /** The destinations themselves. */
    public val items: Reactive<List<Item>> = remember(action = menuItems)

    /** See the `footerItems` constructor parameter. */
    public val footerItems: Reactive<List<Item>> = remember(action = footerItems)

    /** Shown in the app bar, as icons only. */
    public val actionItems: Reactive<List<Item>> = remember(action = actionItems)

    /** The path of the nav link that best matches the current page. See [navSelectedPath]. */
    public val selectedPath: Reactive<List<String>?> = with(writer) { navSelectedPath(items) }

    /** Whether the [drawer] is open. Set it to close the drawer after acting on something. */
    public val drawerOpen: Signal<Boolean> = Signal(false)

    /**
     * Groups the person has explicitly opened or closed, by title. A group that is absent falls
     * back to opening itself when it contains the current page.
     */
    public val groupOverrides: Signal<Map<String, Boolean>> = Signal(mapOf())

    // -----------------------------------------------------------------------------------------
    // Parts
    //
    // Each takes the writer to draw into, so it can be placed under any modifier chain.
    // -----------------------------------------------------------------------------------------

    /**
     * The app bar: back button, logo, the current page's title, and the bar actions.
     *
     * @param menuButtonFor Where the [drawer] toggle appears, or null if there is no drawer to
     *   toggle. `MediaQuery.MinWidth(0.dp)` reads as "at every width".
     * @param inlineLinksFor Where [items] appear in the bar itself, as text links, or null to keep
     *   them out of the bar.
     */
    @EvolvingAppearance
    public fun appBar(
        writer: ElementWriter.CanAddAlignment,
        menuButtonFor: MediaQuery? = null,
        inlineLinksFor: MediaQuery? = null,
    ) {
        writer.shownWhen(default = false, condition = showNav).bar.row {
            applySafeInsets(bottom = false)
            debugName = "app bar"
            showOnPrint = false

            // The web has the browser's own back button; everywhere else the bar has to provide one.
            if (Platform.current != Platform.Web) centered.button {
                icon(Icon.arrowBack, "Go back")
                ::visible { context.pageNavigator.canGoBack() }
                onClick { context.pageNavigator.goBack() }
            }
            if (menuButtonFor != null) centered.shownForQuery(menuButtonFor).toggleButton {
                checked bind drawerOpen
                icon(Icon.menu, "Navigation menu")
            }
            centered.sizeConstraints(height = 2.5.rem, width = 2.5.rem).link {
                rawImage(appLogo, appName)
                accessibleLabel = "$appName home"
                to = { context.mainPageNavigator.routes.parse(UrlLikePath(listOf(), mapOf()))!! }
            }

            centered.expanding.themed(HeaderSemantic).text {
                ::content { context.pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            if (inlineLinksFor != null) {
                centered.shownForQuery(inlineLinksFor).themed(ListSemantic).rowOfExpensive(items) { element ->
                    renderAsMenus(element, PopoverPreferredDirection.belowLeft, selectedPath) {
                        navTextContent(element)
                    }
                }
            }
            centered.themed(ListSemantic).rowOfExpensive(actionItems) { element ->
                renderAsMenus(element, PopoverPreferredDirection.belowLeft, selectedPath) {
                    navIconContent(element)
                }
            }
        }
    }

    /**
     * A vertical strip of destinations beside the content.
     *
     * @param labeled True for the roomy form (icon beside label, groups expand in place); false for
     *   the narrow form (icon above a small label, groups open as popovers - there is no room to
     *   grow downward in a rail this narrow).
     */
    @EvolvingAppearance
    public fun rail(writer: ElementWriter.CanAddAlignment, labeled: Boolean, width: Dimension) {
        fun ElementWriter.CanAddTheme.item(element: Item) {
            if (labeled) renderInline(element, this@Nav, selectedPath) {}
            else renderAsMenus(element, PopoverPreferredDirection.rightBottom, selectedPath) {
                navStackedContent(element)
            }
        }
        writer.shownWhen(default = false, condition = showNav).sizeConstraints(width = width).nav.col {
            debugName = if (labeled) "nav rail" else "nav rail compact"
            showOnPrint = false
            gap = 0.px
            applySafeInsets(top = false)
            expanding.themed(ListSemantic).scrolling.colOfExpensive(items) { item(it) }
            // Account and sign-out belong at the far end, away from the destinations.
            shownWhen(default = false) { footerItems().isNotEmpty() }
                .themed(ListSemantic).colOfExpensive(footerItems) { item(it) }
        }
    }

    /** The bottom tab bar. One tab per destination, each taking an equal share of the width. */
    @EvolvingAppearance
    public fun tabBar(writer: ElementWriter.CanAddAlignment, shown: Reactive<Boolean>) {
        writer.shownWhen(default = false) { shown() }.nav.themed(ListSemantic).rowOfExpensive(
            items,
            beforeModifier = { expanding },
        ) { element ->
            renderAsMenus(element, PopoverPreferredDirection.aboveCenter, selectedPath) {
                navStackedContent(element)
            }
        }.apply {
            applySafeInsets(top = false)
            debugName = "nav tabs"
            showOnPrint = false
        }
    }

    /**
     * The drawer: a panel over the content, with a scrim behind it. Opened by [drawerOpen], which
     * [appBar]'s menu button toggles.
     *
     * The scrim is what makes a drawer readable. Without it the panel sits on live content with no
     * separation, and the only way to dismiss it is to find the toggle again. [dismissBackground]
     * supplies both the dimming and the tap-to-close.
     *
     * @param closeAbove The width at which the layout reveals a rail or inline links instead, or
     *   null when the drawer is the nav at every width.
     */
    @EvolvingAppearance
    public fun drawer(writer: ElementWriter.CanAddAlignment, closeAbove: Dimension?) {
        // The layout swap itself is a media query, but the open/closed state behind it is not:
        // growing the window past the breakpoint would otherwise leave the drawer stranded open
        // over the rail.
        if (closeAbove != null) writer.reactive {
            if (AppState.windowInfo().width > closeAbove) drawerOpen.value = false
        }
        writer.shownWhen(default = false, transition = ScreenTransition.Fade) { drawerOpen() && showNav(this) }
            .dismissBackground {
                padding = 0.px
                debugName = "nav drawer"
                showOnPrint = false
                onClick { drawerOpen.value = false }
                atStart.shownWhen(default = false, transition = ScreenTransition.FromLeft) { drawerOpen() }.frame {
                    sizeConstraints(width = 17.rem).nav.col {
                        gap = 0.px
                        applySafeInsets(right = false)
                        expanding.themed(ListSemantic).scrolling.colOfExpensive(items) {
                            renderInline(it, this@Nav, selectedPath) { drawerOpen.value = false }
                        }
                        shownWhen(default = false) { footerItems().isNotEmpty() }
                            .themed(ListSemantic).colOfExpensive(footerItems) {
                                renderInline(it, this@Nav, selectedPath) { drawerOpen.value = false }
                            }
                    }
                }
            }
    }
}

// ---------------------------------------------------------------------------------------------
// Variants
//
// Each one is responsive across the full width range on its own, using `shownForQuery` - a CSS
// media query, so every form is present in the tree and the browser picks one. Nothing re-renders
// on resize, and the correct form is right on the first paint, including server-side.
//
// Pick by the shape of the app, not by the device:
//   navSidebar - tools, admin consoles, workspaces. Many destinations, often grouped.
//   navTopBar  - docs, marketing, content apps. A handful of shallow sections.
//   navTabs    - consumer apps. Three to five flat destinations.
//   navDrawer  - editors and readers. Nav should never hold permanent screen space.
// ---------------------------------------------------------------------------------------------

/**
 * A sidebar that thins as the window does: labeled rail, then icon-and-label rail, then a drawer.
 *
 * The best fit for apps with more destinations than a tab bar can hold, especially grouped ones -
 * admin consoles, dashboards, anything with a "workspace" feel.
 *
 * | Width | Nav |
 * | --- | --- |
 * | `>= expandedBreakpoint` | Labeled rail. Groups expand in place. |
 * | `>= compactBreakpoint` | Narrow rail, icon above label. Groups open as popovers. |
 * | below | Menu button in the bar opens a drawer over the content. |
 *
 * @param footerItems Pinned to the bottom of the rail and drawer, below a spacer. Account,
 *   settings, sign out - the things that are not destinations within the app's content.
 */
@EvolvingAppearance
@ExperimentalKiteUi
public fun ViewWriter.navSidebar(
    appLogo: ImageSource,
    appName: String,
    expandedBreakpoint: Dimension = 60.rem,
    compactBreakpoint: Dimension = 40.rem,
    showNav: ReactiveContext.() -> Boolean = { true },
    menuItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
    footerItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
    actionItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
) {
    val nav = Nav(this, appLogo, appName, showNav, menuItems, footerItems, actionItems)
    themed(OuterSemantic).col {
        debugName = "nav-sidebar"
        nav.appBar(this, menuButtonFor = MediaQuery.MaxWidth(compactBreakpoint))
        expanding.themed(OuterSemantic).frame {
            applySafeInsets(top = false)
            debugName = "nav and content"
            themed(OuterSemantic).row {
                shownForQuery(MediaQuery.MinWidth(expandedBreakpoint)).frame {
                    nav.rail(this, labeled = true, width = 16.rem)
                }
                shownForQuery(
                    MediaQuery.And(
                        setOf(
                            MediaQuery.MinWidth(compactBreakpoint),
                            MediaQuery.MaxWidth(expandedBreakpoint),
                        )
                    )
                ).frame {
                    nav.rail(this, labeled = false, width = 5.5.rem)
                }
                expanding.navigatorView(context.pageNavigator)
            }
            nav.drawer(this, closeAbove = compactBreakpoint)
        }
        frame { applySafeInsets(top = false) }
    }
}

/**
 * Links live in the top bar itself, and collapse into a drawer when they no longer fit.
 *
 * The best fit for content-shaped apps - documentation, marketing, anything with a handful of
 * shallow sections where the content wants the full width.
 *
 * | Width | Nav |
 * | --- | --- |
 * | `>= breakpoint` | Text links in the bar. Groups open as dropdowns. |
 * | below | Menu button in the bar opens a drawer over the content. |
 */
@EvolvingAppearance
@ExperimentalKiteUi
public fun ViewWriter.navTopBar(
    appLogo: ImageSource,
    appName: String,
    breakpoint: Dimension = 50.rem,
    showNav: ReactiveContext.() -> Boolean = { true },
    menuItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
    actionItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
) {
    val nav = Nav(this, appLogo, appName, showNav, menuItems, { listOf() }, actionItems)
    themed(OuterSemantic).col {
        debugName = "nav-top-bar"
        nav.appBar(
            this,
            menuButtonFor = MediaQuery.MaxWidth(breakpoint),
            inlineLinksFor = MediaQuery.MinWidth(breakpoint),
        )
        expanding.themed(OuterSemantic).frame {
            applySafeInsets(top = false)
            navigatorView(context.pageNavigator)
            nav.drawer(this, closeAbove = breakpoint)
        }
        frame { applySafeInsets(top = false) }
    }
}

/**
 * Bottom tabs on a phone, a rail on anything wider.
 *
 * The best fit for consumer apps with three to five flat destinations. Deeper structure does not
 * survive a tab bar; reach for [navSidebar] instead when there is more than a handful.
 *
 * Bottom tabs are a narrow-window idiom rather than a mobile-platform one, so at width the same
 * destinations become a left rail - the behavior iPadOS and Material 3 both settle on. The tab bar
 * also yields to the on-screen keyboard, which would otherwise sit on top of it.
 *
 * | Width | Nav |
 * | --- | --- |
 * | `>= breakpoint` | Rail on the left, icon above label. Groups open as popovers. |
 * | below | Tab bar along the bottom. Groups open upward as popovers. |
 */
@EvolvingAppearance
@ExperimentalKiteUi
public fun ViewWriter.navTabs(
    appLogo: ImageSource,
    appName: String,
    breakpoint: Dimension = 40.rem,
    showNav: ReactiveContext.() -> Boolean = { true },
    menuItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
    actionItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
) {
    val nav = Nav(this, appLogo, appName, showNav, menuItems, { listOf() }, actionItems)
    // The tab bar covers the bottom safe area itself, so the content only needs to when the tabs
    // step aside for the keyboard.
    val tabsHandleBottom = remember { nav.showNav(this) && !AppState.softInputOpen() }
    themed(OuterSemantic).col {
        debugName = "nav-tabs"
        nav.appBar(this)
        expanding.themed(OuterSemantic).frame {
            applySafeInsets { edges ->
                edges.copy(top = 0.px, bottom = if (tabsHandleBottom()) 0.px else edges.bottom)
            }
            themed(OuterSemantic).row {
                shownForQuery(MediaQuery.MinWidth(breakpoint)).frame {
                    nav.rail(this, labeled = false, width = 5.5.rem)
                }
                expanding.navigatorView(context.pageNavigator)
            }
        }
        shownForQuery(MediaQuery.MaxWidth(breakpoint)).frame {
            nav.tabBar(this, tabsHandleBottom)
        }
    }
}

/**
 * A drawer at every width, opened from a menu button in the bar.
 *
 * The best fit for apps where the content is the point and nav is an interruption - editors,
 * readers, canvases. This is not [navSidebar] with the rail turned off: choosing never to spend
 * screen space on nav even at 1600px is a decision about the product, not about the window.
 */
@EvolvingAppearance
@ExperimentalKiteUi
public fun ViewWriter.navDrawer(
    appLogo: ImageSource,
    appName: String,
    showNav: ReactiveContext.() -> Boolean = { true },
    menuItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
    footerItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
    actionItems: ReactiveContext.() -> List<Nav.Item> = { listOf() },
) {
    val nav = Nav(this, appLogo, appName, showNav, menuItems, footerItems, actionItems)
    themed(OuterSemantic).col {
        debugName = "nav-drawer"
        // MinWidth(0) reads as "at every width" - the drawer is this variant's only nav.
        nav.appBar(this, menuButtonFor = MediaQuery.MinWidth(0.dp))
        expanding.themed(OuterSemantic).frame {
            applySafeInsets(top = false)
            navigatorView(context.pageNavigator)
            nav.drawer(this, closeAbove = null)
        }
        frame { applySafeInsets(top = false) }
    }
}

// ---------------------------------------------------------------------------------------------
// Item content
// ---------------------------------------------------------------------------------------------

/**
 * The badge bubble, small enough to sit on the corner of an icon without swallowing it.
 *
 * The count is left out of the bubble entirely at 0 - see [Nav.count]. It is still announced, via
 * [accessibleName]; a dot that a screen reader skips is a dot that only sighted users get.
 */
private fun ElementWriter.CanAddTheme.navBadge(count: Int) {
    compact.critical.frame {
        debugName = "nav badge"
        // Sized outright rather than by the theme. This bubble sits on the corner of a 1.5rem
        // icon, and at even the compact theme's text size and padding it covers the icon instead
        // of annotating it - what it has to fit beside is what decides the numbers here.
        padding = 0.15.rem
        if (count > 0) {
            centered.textSize(0.7.rem).text(count.toString())
        } else {
            // At 0 the bubble collapses to a dot, with no text inside to give it a size.
            sizeConstraints(width = 0.35.rem, height = 0.35.rem).space()
        }
    }
}

/** The name a screen reader reads, with the badge folded in so it is not silently visual. */
private fun Nav.Item.accessibleName(full: String): String = when (count) {
    null -> full
    0 -> "$full, has updates"
    else -> "$full, $count"
}

/** The icon with its badge on the corner - the form used wherever there is no room beside it. */
private fun ElementWriter.CanAddTheme.navIconAndBadge(element: Nav.Item) {
    fun ElementWriter.navIcon() = icon {
        source = element.icon.copy(width = 1.5.rem, height = 1.5.rem)
        description = ""
    }

    val count = element.count
    if (count == null) {
        navIcon()
        return
    }
    // The outer frame only exists to get back a writer that still accepts sizing - `sizeConstraints`
    // comes before `theme` in the modifier order, and this function is handed a themed writer.
    frame {
        // Deliberately larger than the icon it holds. A badge aligned to the corner of a frame
        // that fits the icon exactly lands on top of the icon and hides what it was annotating;
        // the slack is what lets it overlap only the corner.
        sizeConstraints(width = 2.25.rem, height = 2.rem).frame {
            centered.navIcon()
            atTopEnd.navBadge(count)
        }
    }
}

/** Icon beside label - the roomy form, used in the labeled rail and the drawer. */
private fun ElementWriter.CanAddTheme.navRowContent(element: Nav.Item, disclosure: Reactive<Boolean>? = null) {
    if (element is Nav.Custom) {
        frame { element.wide(this) }
        return
    }
    row {
        centered.icon {
            source = element.icon.copy(width = 1.5.rem, height = 1.5.rem)
            description = ""
        }
        centered.expanding.text(element.title)
        // There is room for the badge beside the label here, which reads better than on the icon.
        element.count?.let { centered.navBadge(it) }
        // Points right when closed and down when open, at the content it revealed.
        if (disclosure != null) centered.icon {
            ::source { (if (disclosure()) Icon.chevronDown else Icon.chevronRight).copy(width = 1.rem, height = 1.rem) }
            description = ""
        }
    }
}

/** Icon above a small label - the narrow rail and the tab bar. */
private fun ElementWriter.CanAddTheme.navStackedContent(element: Nav.Item) {
    if (element is Nav.Custom) {
        frame { element.narrow(this) }
        return
    }
    themed(CompactSemantic).col {
        centered.navIconAndBadge(element)
        centered.subtext {
            content = element.title
            wraps = false
            ellipsis = true
        }
    }
}

/** Label only - inline links in a top bar, where an icon beside every word is just noise. */
private fun ElementWriter.CanAddTheme.navTextContent(element: Nav.Item) {
    if (element is Nav.Custom) {
        frame { element.wide(this) }
        return
    }
    val count = element.count
    if (count == null) {
        text(element.title)
        return
    }
    row {
        centered.text(element.title)
        centered.navBadge(count)
    }
}

/** Icon only - bar actions, which are identified by their icon and their accessible label. */
private fun ElementWriter.CanAddTheme.navIconContent(element: Nav.Item) {
    if (element is Nav.Custom) {
        frame { element.narrow(this) }
        return
    }
    navIconAndBadge(element)
}

// ---------------------------------------------------------------------------------------------
// Group strategies
// ---------------------------------------------------------------------------------------------

/**
 * Groups expand in place, indented under a disclosure control.
 *
 * A group opens itself when it contains the current page, so arriving by URL or by back button
 * shows you where you are without a click. Toggling it by hand takes over from then on.
 */
private fun ElementWriter.CanAddTheme.renderInline(
    element: Nav.Item,
    shell: Nav,
    selectedPath: Reactive<List<String>?>,
    select: suspend () -> Unit,
) {
    if (element !is Nav.Group) {
        renderLeaf(element, selectedPath, select) { navRowContent(element) }
        return
    }
    col {
        val open = remember {
            shell.groupOverrides()[element.title] ?: run {
                val selected = selectedPath() ?: return@run false
                val navigator = context.mainPageNavigator
                element.contains(selected) { navigator.routes.render(it)?.urlLikePath?.segments }
            }
        }
        gap = 0.px
        // The header matches its sibling links' unselected treatment. It never shows as selected
        // itself - the selected child is right below it, and marking both just adds noise.
        themed(UnselectedSemantic).button {
            accessibleLabel = element.accessibleName(element.fullTitle)
            navRowContent(element, disclosure = open)
            onClick {
                shell.groupOverrides.value = shell.groupOverrides.value + (element.title to !open.awaitOnce())
            }
        }
        shownWhen(default = false) { open() }.row {
            gap = 0.px
            space(0.5)
            expanding.themed(ListSemantic).col {
                gap = 0.px
                for (child in element.children) renderInline(child, shell, selectedPath, select)
            }
        }
    }
}

/** Groups open as popover menus - the only option where there is no room to grow in place. */
private fun ElementWriter.CanAddTheme.renderAsMenus(
    element: Nav.Item,
    direction: PopoverPreferredDirection,
    selectedPath: Reactive<List<String>?>,
    select: suspend () -> Unit = {},
    content: ElementWriter.CanAddTheme.() -> Unit,
) {
    if (element !is Nav.Group) {
        renderLeaf(element, selectedPath, select, content)
        return
    }
    selectedIfContains(element, selectedPath).menuButton {
        accessibleLabel = element.accessibleName(element.fullTitle)
        content()
        preferredDirection = direction
        opensMenu {
            themed(ListSemantic).col {
                for (child in element.children) {
                    renderAsMenus(child, direction, selectedPath, { context.closePopovers() }) {
                        navRowContent(child)
                    }
                }
            }
        }
    }
}

/** Everything that is not a [Nav.Group] renders the same way in every variant. */
private fun ElementWriter.CanAddTheme.renderLeaf(
    element: Nav.Item,
    selectedPath: Reactive<List<String>?>,
    select: suspend () -> Unit,
    content: ElementWriter.CanAddTheme.() -> Unit,
) {
    when (element) {
        is Nav.Action -> button {
            accessibleLabel = element.accessibleName(element.title)
            content()
            onClick { element.onSelect(); select() }
        }

        is Nav.Link -> selectedIfCurrent(element, selectedPath).link {
            content()
            resetsStack = true
            to = element.to
            accessibleLabel = element.accessibleName(element.fullTitle)
            onNavigate { select() }
        }

        // Never selected - it leads out of the app, so it is never where you are - but still
        // explicitly unselected, or it renders brighter than the siblings it sits beside.
        is Nav.External -> themed(UnselectedSemantic).externalLink {
            content()
            to = element.to
            accessibleLabel = element.accessibleName(element.fullTitle)
            onNavigate { select() }
        }

        // No button, no link, no selection - the content owns the whole slot. Whichever of
        // Custom.wide/Custom.narrow fits was already chosen by [content].
        is Nav.Custom -> content()

        is Nav.Group -> throw IllegalStateException(
            "Groups are handled by the caller's group strategy, not by renderLeaf"
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Selection
// ---------------------------------------------------------------------------------------------

/**
 * The path of the nav link that best describes where the person is.
 *
 * Comparing each link's path to the current one for equality only lights an item up on that item's
 * own page - land on `/docs/icons` and the "Documentation" entry goes dark, which reads as having
 * left the section. Instead every link whose path prefixes the current one is a candidate and the
 * longest wins, so a section stays lit throughout its subtree while a more specific entry still
 * takes precedence over its parent. The root link has an empty path, which prefixes everything, so
 * it only wins when nothing else matches - exactly what it wants.
 */
private fun ViewWriter.navSelectedPath(items: Reactive<List<Nav.Item>>): Reactive<List<String>?> = remember {
    val navigator = context.mainPageNavigator
    val current = navigator.currentPage()?.let { navigator.routes.render(it) }?.urlLikePath?.segments
        ?: return@remember null
    items().allLinks()
        .mapNotNull { navigator.routes.render(it.to())?.urlLikePath?.segments }
        .filter { it.size <= current.size && current.subList(0, it.size) == it }
        .maxByOrNull { it.size }
}

// Only Link participates in selection: External leaves the app, and Action and Custom have no
// destination to compare against.
private fun List<Nav.Item>.allLinks(): List<Nav.Link> = flatMap {
    when (it) {
        is Nav.Group -> it.children.allLinks()
        is Nav.Link -> listOf(it)
        is Nav.External, is Nav.Action, is Nav.Custom -> listOf()
    }
}

/** Whether this element, or anything under it, is the destination at [path]. */
private fun Nav.Item.contains(path: List<String>, pathOf: (Page) -> List<String>?): Boolean = when (this) {
    is Nav.Group -> children.any { it.contains(path, pathOf) }
    is Nav.Link -> pathOf(to()) == path
    is Nav.External, is Nav.Action, is Nav.Custom -> false
}

/**
 * A group whose children are hidden behind a popover is the only thing on screen standing for
 * them, so it shows their selection. Without this a collapsed section goes dark the moment you
 * open one of its pages, and the nav stops answering "where am I".
 */
private fun ElementWriter.CanAddTheme.selectedIfContains(
    group: Nav.Group,
    selectedPath: Reactive<List<String>?>,
): ElementWriter.CanAddScrolling = dynamicThemed {
    val selected = selectedPath()
    val navigator = context.mainPageNavigator
    val holdsCurrentPage = selected != null &&
            group.contains(selected) { navigator.routes.render(it)?.urlLikePath?.segments }
    if (holdsCurrentPage) SelectedSemantic else UnselectedSemantic
}

private fun ElementWriter.CanAddTheme.selectedIfCurrent(
    link: Nav.Link,
    selectedPath: Reactive<List<String>?>,
): ElementWriter.CanAddScrolling = dynamicThemed {
    val navigator = context.mainPageNavigator
    val own = navigator.routes.render(link.to())?.urlLikePath?.segments
    if (own != null && own == selectedPath()) SelectedSemantic else UnselectedSemantic
}
