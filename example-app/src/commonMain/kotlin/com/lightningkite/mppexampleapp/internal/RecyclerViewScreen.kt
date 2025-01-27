package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("recycler-view")
object RecyclerViewScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    @QueryParameter
    val elementCount = Property(10000)

    override fun ViewWriter.render() {
        var expanded = Property(-1)
        val items = shared { (1..elementCount()).toList() }
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
                sizeConstraints(width = 10.rem) - field("Element Count") {
                    numberInput { content bind elementCount.nullable().asDouble() }
                }
            }
            recyclerView {
                recyclerView = this
                spacing = 0.5.rem
//                columns = 2
                reactive {
                    val index = expanded()
                    if(index == -1) return@reactive
                    launch {
                        delay(250)
//                        this@recyclerView.scrollToIndex(index - 1, Align.Start, true)
                    }
                }
                this.scrollToIndex(10, Align.Start)
                children(items) {
                    col child@{
                        dynamicTheme {
                            if (it() == 50) ImportantSemantic
                            else if (it() % 7 == 0) HoverSemantic
                            else null
                        }
                        row {
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
                        onlyWhen { expanded() == it() } - col {
//                            ::exists { expanded() == it() }
                            text { ::content { "Content for ${it()} == ${expanded()}" } }
                            text("More Content")
                            text("More Content")
                            text("More Content")
                            text("More Content")
                            text("More Content")
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