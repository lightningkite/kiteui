package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


fun ElementWriter.navGroupColumn(
    elements: Reactive<List<NavElement>>,
    onNavigate: suspend () -> Unit = {},
    setup: LinearLayoutElement.() -> Unit = {}
) {
    col {
        navGroupColumnInner(elements, onNavigate)
        setup()
    }
}

private fun ElementWriter.CanAddTheme.selectedIfRouteMatches(it: NavLink): ElementWriter.CanAddScrolling =
    dynamicThemed {
        val matchingPage = context.mainPageNavigator.currentPage()
            ?.let { context.mainPageNavigator.routes.render(it) }?.urlLikePath?.segments == context.mainPageNavigator.routes.render(
            it.destination.invoke(this)()
        )?.urlLikePath?.segments
        if (matchingPage) SelectedSemantic else ForcePaddingSemantic
    }

private fun ContainerElement.navGroupColumnInner(readable: Reactive<List<NavElement>>, onNavigate: suspend () -> Unit = {}) {
    themeChoice += ListSemantic
    forEach(readable) {
        fun ViewWriter.display(navElement: NavElement) {
            row {
                centered.navElementIconAndCountHorizontal(navElement)
                centered.text { ::content { navElement.title(this) } }
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
                this.onNavigate(action = onNavigate)
            }

            is NavGroup -> {
                col {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    gap = 0.px
                    padded.row {
                        centered.navElementIconAndCountHorizontal(it)
                        centered.text { ::content { it.title(this) } }
                    }
                    row {
                        gap = 0.px
                        space()
                        expanding.col {
                            gap = 0.px
                            navGroupColumnInner(remember { it.children(this) }, onNavigate)
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

            is NavLink -> selectedIfRouteMatches(it).link {
                resetsStack = true
                shown = false

                ::shown { it.hidden?.invoke() != true }
                ::to { it.destination(this) }
                display(it)
                this.onNavigate(action = onNavigate)
            }
        }
    }
}

fun ElementWriter.navGroupActions(elements: Reactive<List<NavElement>>, setup: ContainingView.() -> Unit = {}): Unit {
    row {
        navGroupActionsInner(elements)
        setup()
    }
}

private fun ContainerElement.navGroupActionsInner(readable: Reactive<List<NavElement>>) {
    fun ViewWriter.navElementIconAndCount(navElement: NavElement) {
        frame {
            centered.icon {
                ::source { navElement.icon() }
                ::description { navElement.title() }
            }
        }
        navElement.count?.let { count ->
            atTopEnd.compact.critical.frame {
                shown = false
                ::shown { count() != null }
                subtext {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "*" }
                }
            }
        }
    }
    themeChoice += ListSemantic
    forEach(readable) {
        when (it) {
            is NavAction -> button {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                navElementIconAndCount(it)
                onClick { it.onSelect() }
            }

            is NavExternal -> externalLink {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                ::to { it.to() }
                navElementIconAndCount(it)
            }

            is NavGroup -> row {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                navGroupActionsInner(remember { it.children() })
            }

            is NavCustom -> frame {
                shown = false
                ::shown { it.hidden?.invoke() != true }
                it.square(this@forEach)
            }

            is NavLink -> selectedIfRouteMatches(it).link {
                resetsStack = true
                shown = false
                ::shown { it.hidden?.invoke() != true }
                ::to { it.destination() }
                navElementIconAndCount(it)
            }
        }
    }
}

fun ElementWriter.navGroupTop(readable: Reactive<List<NavElement>>, setup: ContainingView.() -> Unit = {}) {
    row {
        navGroupTopInner(readable)
        setup()
    }
}

private fun ContainerElement.navGroupTopInner(readable: Reactive<List<NavElement>>) {
    themeChoice += ListSemantic
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
                    navGroupColumn(remember { it.children() }, { closePopovers() })
                }
                text { ::content { it.title() } }
            }

            is NavLink -> selectedIfRouteMatches(it).link {
                resetsStack = true
                shown = false
                ::shown { it.hidden?.invoke() != true }
                ::to { it.destination() }
                text { ::content { it.title() } }
            }
        }
    }
}

fun ElementWriter.navElementIconAndCount(navElement: NavElement): Unit {
    frame {
        centered.icon {
            ::source { navElement.icon() }
            ::description { navElement.title() }
        }
        navElement.count?.let { count ->
            align(Align.End, Align.Start).compact.critical.frame {
                shown = false
                ::shown { count() != null }
                space(0.01)
                centered.subtext {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "" }
                }
            }
        }
    }
}

fun ElementWriter.navElementIconAndCountHorizontal(navElement: NavElement): Unit {
    row {
        centered.icon {
            ::source { navElement.icon().copy(width = 1.5.rem, height = 1.5.rem) }
            ::description { navElement.title() }
        }
        navElement.count?.let { count ->
            centered.compact.critical.frame {
                shown = false
                ::shown { count() != null }
                space(0.01)
                centered.text {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "" }
                }
            }
        }
    }
}

fun ElementWriter.navGroupTabs(readable: Reactive<List<NavElement>>, setup: ContainingView.() -> Unit): Unit {
    row {
        setup()
        fun ViewWriter.display(navElement: NavElement) {
            compact.col {
                centered.navElementIconAndCount(navElement)
                centered.subtext { ::content { navElement.title() } }
            }
        }
        themeChoice += ListSemantic
        forEach(readable) {
            when (it) {
                is NavAction -> expanding.button {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    display(it)
                    onClick { it.onSelect() }
                }

                is NavExternal -> expanding.externalLink {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    ::to { it.to() }
                    display(it)
                }

                is NavGroup -> expanding.menuButton {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    display(it)
                    preferredDirection = PopoverPreferredDirection.aboveCenter
                    opensMenu {
                        navGroupColumn(remember { it.children() }, { closePopovers() })
                    }
                }

                is NavCustom -> expanding.frame {
                    shown = false
                    ::shown { it.hidden?.invoke() != true }
                    it.tall(this)
                }

                is NavLink -> {
                    expanding.selectedIfRouteMatches(it).link {
                        resetsStack = true
                        shown = false
                        ::shown { it.hidden?.invoke() != true }
                        display(it)
                        ::to { it.destination() }
                    }
                }
            }
        }
    }
}