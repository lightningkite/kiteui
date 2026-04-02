package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerHorizontalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.lazyExpanding
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("recycler-view/horizontal")
object HorizontalRecyclerViewPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        var expanded = Signal(-1)
        val items = Signal((1..101).toList())
        var recyclerView: RecyclerView? = null
        col {
            row {
                for (align in Align.values()) {
                    expanding.button {
                        subtext("Jump ${align.name}")
                        onClick { recyclerView?.scrollToIndex(49, align, false) }
                    }
                }
            }
            row {
                for (align in Align.values()) {
                    expanding.button {
                        subtext("Scroll ${align.name}")
                        onClick { recyclerView?.scrollToIndex(49, align, true) }
                    }
                }
            }
            row {
                repeat(4) {
                    val cols = it + 1
                    expanding.button {
                        subtext("$cols columns")
                        onClick { recyclerView?.placer = RecyclerViewPlacerHorizontalGrid(cols) }
                    }
                }
            }
            weight(1f).horizontalRecyclerView {
                recyclerView = this
                gap = 0.5.rem
                placer = RecyclerViewPlacerHorizontalGrid(2)
                scrollToIndex(10, Align.Start)
                children(items, id = { it }) {
                    col {
dynamicThemed {
                                if (it() == 50) ImportantSemantic
                                else if (it() % 7 == 0) HoverSemantic
                                else null
                            }.                        row {
                            
                            centered.expanding.text { ::content { "Item ${it()}" } }
                            centered.button {
                                text {
                                    ::content { if (expanded() == it()) "Expanded" else "Expand" }
                                }
                                onClick {
                                    expanded.value = if (it.await() == expanded.value) -1 else it.await()
            //                                    scrollIntoView(null, Align.Start, true)
                                }
                            }
                        }
                        lazyExpanding(remember { expanded() == it() }) {
                            row {
                                text("More Content")
                            }
                        }
                    }
                }
            }
            row {
                text {
                    ::content {
                        "Min: ${recyclerView!!.firstIndex()}, Max: ${recyclerView!!.lastIndex()}"
                    }
                }
            }
        }
    }
}