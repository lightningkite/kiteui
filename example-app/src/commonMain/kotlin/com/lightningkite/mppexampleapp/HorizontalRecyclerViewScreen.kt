package com.lightningkite.mppexampleapp

import ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.lazyExpanding

@Routable("recycler-view/horizontal")
object HorizontalRecyclerViewScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        var expanded = Property(-1)
        val items = Property((1..101).toList())
        var recyclerView: RecyclerView? = null
        col {
            row {
                for (align in Align.values()) {
                    expanding - button {
                        subtext("Jump ${align.name}")
                        onClick { recyclerView?.scrollToIndex(49, align, false) }
                    }
                }
            }
            row {
                for (align in Align.values()) {
                    expanding - button {
                        subtext("Scroll ${align.name}")
                        onClick { recyclerView?.scrollToIndex(49, align, true) }
                    }
                }
            }
            row {
                repeat(4) {
                    val cols = it + 1
                    expanding - button {
                        subtext("${cols} columns")
                        onClick { recyclerView?.columns = cols }
                    }
                }
            }
            horizontalRecyclerView {
                recyclerView = this
                spacing = 0.5.rem
                columns = 2
                this.scrollToIndex(10, Align.Start)
                children(items) {
                    col {
                        row {
                            ::themeChoice { if (it() == 50) ThemeChoice.Derive { it.important() } else if (it() % 7 == 0) ThemeChoice.Derive { it.hover() } else null }
                            expanding - centered - text { ::content { "Item ${it.await()}" } }
                            centered - button {
                                text {
                                    ::content { if (expanded.await() == it.await()) "Expanded" else "Expand" }
                                }
                                onClick {
                                    expanded.value = if (it.await() == expanded.value) -1 else it.await()
                                    scrollIntoView(null, Align.Start, true)
                                }
                            }
                        }
                        lazyExpanding(shared { expanded.await() == it.await() }) {
                            row {
                                text("More Content")
                            }
                        }
                    }
                }
            } in weight(1f)
            row {
                text {
                    ::content {
                        "Min: ${recyclerView!!.firstVisibleIndex.await()}, Max: ${recyclerView!!.lastVisibleIndex.await()}"
                    }
                }
            }
        }
    }
}