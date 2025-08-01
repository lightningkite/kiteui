package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.lazyExpanding

@Routable("recycler-view/horizontal")
public object HorizontalRecyclerViewPage : Page {
    public override val title: Readable<String>
        get() = super.title

    public override fun ViewWriter.render(): ViewModifiable = run {
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
                gap = 0.5.rem
                columns = 2
                this.scrollToIndex(10, Align.Start)
                children(items) {
                    col {
                        row {
                            dynamicTheme {
                                if (it() == 50) ImportantSemantic
                                else if (it() % 7 == 0) HoverSemantic
                                else null
                            }
                            expanding - centered - text { ::content { "Item ${it()}" } }
                            centered - button {
                                text {
                                    ::content { if (expanded() == it()) "Expanded" else "Expand" }
                                }
                                onClick {
                                    expanded.value = if (it.await() == expanded.value) -1 else it.await()
//                                    scrollIntoView(null, Align.Start, true)
                                }
                            }
                        }
                        lazyExpanding(shared { expanded() == it() }) {
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
                        "Min: ${recyclerView!!.firstVisibleIndex()}, Max: ${recyclerView!!.lastVisibleIndex()}"
                    }
                }
            }
        }
    }
}