package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*


fun ViewWriter.navGroupColumn(
    elements: Readable<List<NavElement>>,
    onNavigate: suspend () -> Unit = {},
    setup: ContainingView.() -> Unit = {}
): ViewModifiable {
    return nav - col {
        navGroupColumnInner(elements, onNavigate)
        setup()
    }
}

private fun RView.selectedIfRouteMatches(it: NavLink) {
    dynamicTheme {
        val matchingPage = mainPageNavigator.currentPage()
            ?.let { mainPageNavigator.routes.render(it) }?.urlLikePath?.segments == mainPageNavigator.routes.render(
            it.destination.invoke(this)()
        )?.urlLikePath?.segments
        if (matchingPage) SelectedSemantic else ForcePaddingSemantic
    }
}

private fun RView.navGroupColumnInner(readable: Readable<List<NavElement>>, onNavigate: suspend () -> Unit = {}) {
    forEach(readable) {
        fun ViewWriter.display(navElement: NavElement) {
            row {
                centered - navElementIconAndCountHorizontal(navElement)
                centered - text { ::content { navElement.title(this) } }
                space(1.0)
            }
        }
        it.weight?.let { w -> weight(w) }
        when (it) {
            is NavAction -> button {
                shown = false
                ::shown { it.hidden?.invoke(this) != true }
                display(it)
                onClick {
                    it.onSelect()
                    onNavigate()
                }
            }

            is NavExternal -> externalLink {
                shown = false
                ::shown { it.hidden?.invoke(this) != true }
                ::to { it.to(this) }
                display(it)
                this.onNavigate(onNavigate)
            }

            is NavGroup -> {
                col {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    gap = 0.px
                    padded - row {
                        centered - navElementIconAndCountHorizontal(it)
                        centered - text { ::content { it.title(this) } }
                    }
                    row {
                        gap = 0.px
                        space()
                        expanding - col {
                            gap = 0.px
                            navGroupColumnInner(shared { it.children(this) }, onNavigate)
                        }
                    }
                }
            }

            is NavCustom -> {
                frame {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    it.long(this@frame)
                }
            }

            is NavLink -> link {
                selectedIfRouteMatches(it)
                resetsStack = true
                shown = false

                ::shown { it.hidden?.invoke() != true }
                ::to { it.destination(this) }
                display(it)
                this.onNavigate(onNavigate)
            }
        }
    }
}

fun ViewWriter.navGroupActions(elements: Readable<List<NavElement>>, setup: ContainingView.() -> Unit = {}): ViewModifiable {
    return row {
        navGroupActionsInner(elements)
        setup()
    }
}

private fun RView.navGroupActionsInner(readable: Readable<List<NavElement>>) {
    fun ViewWriter.navElementIconAndCount(navElement: NavElement) {
        padded - frame {
            centered - icon {
                ::source { navElement.icon() }
                ::description { navElement.title() }
            }
        }
        navElement.count?.let { count ->
            atTopEnd - compact - critical - frame {
                shown = false
                ::shown { count() != null }
                subtext {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "*" }
                }
            }
        }
    }
    forEach(readable) {
        when (it) {
            is NavAction -> unpadded - button {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                navElementIconAndCount(it)
                onClick { it.onSelect() }
            }

            is NavExternal -> unpadded - externalLink {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                ::to { it.to() }
                navElementIconAndCount(it)
            }

            is NavGroup -> row {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                navGroupActionsInner(shared { it.children() })
            }

            is NavCustom -> frame {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                it.square(this@forEach)
            }

            is NavLink -> unpadded - link {
                selectedIfRouteMatches(it)
                resetsStack = true
                shown = false
                ::shown { it.hidden?.invoke() != true }
                ::to { it.destination() }
                navElementIconAndCount(it)
            }
        }
    }
}

fun ViewWriter.navGroupTop(readable: Readable<List<NavElement>>, setup: ContainingView.() -> Unit = {}): ViewModifiable {
    return row {
        navGroupTopInner(readable)
        setup()
    }
}

private fun RView.navGroupTopInner(readable: Readable<List<NavElement>>) {
    forEach(readable) {
        when (it) {
            is NavAction -> button {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                text { ::content { it.title() } }
                onClick { it.onSelect() }
            }

            is NavExternal -> externalLink {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                ::to { it.to() }
                text { ::content { it.title() } }
            }

            is NavCustom -> {
                frame {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    it.square(this@forEach)
                }
            }

            is NavGroup -> menuButton {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                preferredDirection = PopoverPreferredDirection.belowRight
                opensMenu {
                    navGroupColumn(shared { it.children() }, { closePopovers() })
                }
                text { ::content { it.title() } }
            }

            is NavLink -> link {
                selectedIfRouteMatches(it)
                resetsStack = true
                shown = false
                ::shown { it.hidden?.invoke() != true }
                ::to { it.destination() }
                text { ::content { it.title() } }
            }
        }
    }
}

fun ViewWriter.navElementIconAndCount(navElement: NavElement): ViewModifiable {
    return frame {
        centered - icon {
            ::source { navElement.icon() }
            ::description { navElement.title() }
        }
        navElement.count?.let { count ->
            align(Align.End, Align.Start) - compact - critical - frame {
                shown = false
                ::shown { count() != null }
                space(0.01)
                centered - subtext {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "" }
                }
            }
        }
    }
}

fun ViewWriter.navElementIconAndCountHorizontal(navElement: NavElement): ViewModifiable {
    return row {
        centered - icon {
            ::source { navElement.icon().copy(width = 1.5.rem, height = 1.5.rem) }
            ::description { navElement.title() }
        }
        navElement.count?.let { count ->
            centered - compact - critical - frame {
                shown = false
                ::shown { count() != null }
                space(0.01)
                centered - text {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "" }
                }
            }
        }
    }
}

fun ViewWriter.navGroupTabs(readable: Readable<List<NavElement>>, setup: ContainingView.() -> Unit): ViewModifiable {
    return nav - unpadded - row {
        setup()
        fun ViewWriter.display(navElement: NavElement) {
            compact - col {
                centered - navElementIconAndCount(navElement)
                centered - subtext { ::content { navElement.title() } }
            }
        }
        forEach(readable) {
            when (it) {
                is NavAction -> expanding - button {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    display(it)
                    onClick { it.onSelect() }
                }

                is NavExternal -> expanding - externalLink {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    ::to { it.to() }
                    display(it)
                }

                is NavGroup -> expanding - menuButton {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    display(it)
                    preferredDirection = PopoverPreferredDirection.aboveCenter
                    opensMenu {
                        navGroupColumn(shared { it.children() }, { closePopovers() })
                    }
                }

                is NavCustom -> {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    expanding
                    it.tall(this)
                }

                is NavLink -> {
                    expanding - link {
                        selectedIfRouteMatches(it)
                        resetsStack = true
                        shown = false
                        ::shown { it.hidden?.invoke() != true }
                        display(it)
                        ::to { it.destination() }
                    }
                    Unit
                }
            }
        }
    }
}