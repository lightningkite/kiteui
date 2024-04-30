package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*


fun ViewWriter.navGroupColumn(elements: Readable<List<NavElement>>, onNavigate: suspend ()->Unit = {}, setup: ContainingView.()->Unit = {}) {
    col {
        navGroupColumnInner(elements, onNavigate)
        setup()
    }
}
private fun ViewWriter.navGroupColumnInner(readable: Readable<List<NavElement>>, onNavigate: suspend ()->Unit = {}) {
    forEach(readable) {
        fun ViewWriter.display(navElement: NavElement) {
            row {
                centered - navElementIconAndCountHorizontal(navElement)
                text { ::content { navElement.title() } } in gravity(Align.Center, Align.Center)
                space(1.0)
            }
        }
        when (it) {
            is NavAction -> button {
                exists = false
                ::exists {it.hidden?.invoke() != true}
                display(it)
                onClick { it.onSelect() }
            }

            is NavExternal -> externalLink {
                exists = false
                ::exists {it.hidden?.invoke() != true}
                ::to { it.to() }
                display(it)
                this.onNavigate(onNavigate)
            }

            is NavGroup -> {
                col {
                    exists = false
                    ::exists {it.hidden?.invoke() != true}
                    spacing = 0.px
                    padded - row {
                        centered - navElementIconAndCountHorizontal(it)
                        centered - text { ::content { it.title() } }
                    }
                    row {
                        spacing = 0.px
                        space()
                        expanding - col {
                            spacing = 0.px
                            navGroupColumnInner(shared { it.children() }, onNavigate)
                        }
                    }
                }
            }

            is NavCustom -> {
                stack {
                    exists = false
                    ::exists { it.hidden?.invoke() != true }
                    it.long(this@forEach)
                }
            }

            is NavLink -> link {
                resetsStack = true
                exists = false
                ::exists {it.hidden?.invoke() != true}
                ::to { it.destination() }
                display(it)
                this.onNavigate(onNavigate)
            } in maybeThemeFromLast { existing ->
                if (navigator.currentScreen.await()
                        ?.let { navigator.routes.render(it) } == navigator.routes.render(it.destination())
                )
                    existing.selected()
                else
                    null
            }
        }
    }
}

fun ViewWriter.navGroupActions(elements: Readable<List<NavElement>>, setup: ContainingView.()->Unit = {}) {
     row {
        navGroupActionsInner(elements)
        setup()
    }
}
private fun ViewWriter.navGroupActionsInner(readable: Readable<List<NavElement>>) {
    fun ViewWriter.navElementIconAndCount(navElement: NavElement) {
        padded - stack {
            icon {
                ::source { navElement.icon() }
            } in gravity(Align.Center, Align.Center)
        }
        navElement.count?.let { count ->
            gravity(Align.End, Align.Start) - compact - critical - stack {
                exists = false
                ::exists { count() != null }
                space(0.01)
                centered - text {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "" }
                    textSize = 0.75.rem
                }
            }
        }
    }
    forEach(readable) {
        when (it) {
            is NavAction -> unpadded - button {
                exists = false
                ::exists {it.hidden?.invoke() != true}
//                text { ::content { it.title() } }
                navElementIconAndCount(it)
                onClick { it.onSelect() }
            }

            is NavExternal -> unpadded - externalLink {
                exists = false
                ::exists {it.hidden?.invoke() != true}
                ::to { it.to() }
//                text { ::content { it.title() } }
                navElementIconAndCount(it)
            }

            is NavGroup -> {
                row {
                    exists = false
                    ::exists {it.hidden?.invoke() != true}
                    navGroupActionsInner(shared { it.children() })
                }
            }

            is NavCustom -> {
                stack {
                    exists = false
                    ::exists { it.hidden?.invoke() != true }
                    it.square(this@forEach)
                }
            }

            is NavLink -> unpadded - link {
                resetsStack = true
                exists = false
                ::exists {it.hidden?.invoke() != true}
                ::to { it.destination() }
//                text { ::content { it.title() } }
                navElementIconAndCount(it)
            } in maybeThemeFromLast { existing ->
                if (navigator.currentScreen.await()
                        ?.let { navigator.routes.render(it) } == navigator.routes.render(it.destination())
                )
                    existing.down()
                else
                    null
            }
        }
    }
}

fun ViewWriter.navGroupTop(readable: Readable<List<NavElement>>, setup: ContainingView.()->Unit = {}) {
    row {
        navGroupTopInner(readable)
        setup()
    }
}
private fun ViewWriter.navGroupTopInner(readable: Readable<List<NavElement>>) {
    forEach(readable) {
        when (it) {
            is NavAction -> button {
                exists = false
                ::exists {it.hidden?.invoke() != true}
                text { ::content { it.title() } }
                onClick { it.onSelect() }
            }

            is NavExternal -> externalLink {
                exists = false
                ::exists {it.hidden?.invoke() != true}
                ::to { it.to() }
                text { ::content { it.title() } }
            }

            is NavCustom -> {
                stack {
                    exists = false
                    ::exists { it.hidden?.invoke() != true }
                    it.square(this@forEach)
                }
            }

            is NavGroup -> menuButton {
                exists = false
                ::exists {it.hidden?.invoke() != true}
                preferredDirection = PopoverPreferredDirection.belowRight
                val closer = popoverClosers
                opensMenu {
                    navGroupColumn(shared { it.children() }, { closer.forEach { it() }; closer.clear() })
                }
                text { ::content { it.title() } }
            }

            is NavLink -> link {
                resetsStack = true
                exists = false
                ::exists {it.hidden?.invoke() != true}
                ::to { it.destination() }
                text { ::content { it.title() } }
            }
        }
    }
}

fun ViewWriter.navElementIconAndCount(navElement: NavElement) {
    stack {
        icon {
            ::source { navElement.icon() }
        } in gravity(Align.Center, Align.Center)
        navElement.count?.let { count ->
            gravity(Align.End, Align.Start) - compact - critical - stack {
                exists = false
                ::exists { count() != null }
                space(0.01)
                centered - text {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "" }
                    textSize = 0.75.rem
                }
            }
        }
    }
}

fun ViewWriter.navElementIconAndCountHorizontal(navElement: NavElement) {
    row {
        centered - icon {
            ::source { navElement.icon().copy(width = 1.5.rem, height = 1.5.rem) }
        }
        navElement.count?.let { count ->
            centered  - compact - critical - stack {
                exists = false
                ::exists { count() != null }
                space(0.01)
                centered - text {
                    ::content { count()?.takeIf { it > 0 }?.toString() ?: "" }
                }
            }
        }
    }
}

fun ViewWriter.navGroupTabs(readable: Readable<List<NavElement>>, setup: ContainingView.()->Unit) {
    navSpacing - nav - unpadded - row {
        setup()
        fun ViewWriter.display(navElement: NavElement) {
                compact - col {
                    centered - navElementIconAndCount(navElement)
                    subtext { ::content { navElement.title() } } in gravity(Align.Center, Align.Center)
                }
        }
        forEach(readable) {
            when (it) {
                is NavAction -> expanding - button {
                    exists = false
                    ::exists {it.hidden?.invoke() != true}
                    display(it)
                    onClick { it.onSelect() }
                }

                is NavExternal -> expanding - externalLink {
                    exists = false
                    ::exists {it.hidden?.invoke() != true}
                    ::to { it.to() }
                    display(it)
                }

                is NavGroup -> expanding - menuButton {
                    exists = false
                    ::exists {it.hidden?.invoke() != true}
                    display(it)
                    preferredDirection = PopoverPreferredDirection.aboveCenter
                    val closer = popoverClosers
                    opensMenu {
                        navGroupColumn(shared { it.children() }, { closer.forEach { it() }; closer.clear() })
                    }
                }

                is NavCustom -> {
                    exists = false
                    ::exists {it.hidden?.invoke() != true}
                    expanding - it.tall(this)
                }

                is NavLink -> {

                    expanding - link {
                        resetsStack = true
                        exists = false
                        ::exists {it.hidden?.invoke() != true}
                        display(it)
                        ::to { it.destination() }
                    } in maybeThemeFromLast { existing ->
                        if (navigator.currentScreen.await()?.let { navigator.routes.render(it) }?.urlLikePath?.segments == navigator.routes.render(
                                it.destination()
                            )?.urlLikePath?.segments)
                            existing.selected()
                        else
                            null
                    }
                    Unit
                }
            }
        }
    } 
}