package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("recycler2-test")
object Recycler2PullToRefreshTest : Page {
    override val title: Reactive<String>
        get() = super.title

    @QueryParameter
    val elementCount = Signal(10000)

    override fun ViewWriter.render(): Unit = run {
        col {
            val expanded = Signal(-1)
            var recyclerView: Recycler2? = null
            row {
                for (align in Align.values()) {
                    expanding.button {
                        subtext("Jump ${align.name}")
                        onClick { recyclerView?.scrollToIndex(75, align, false) }
                    }
                }
            }
            row {
                for (align in Align.values()) {
                    expanding.button {
                        subtext("Scroll ${align.name}")
                        onClick { recyclerView?.scrollToIndex(75, align, true) }
                    }
                }
            }
            row {
                repeat(4) {
                    val cols = (it + 1) * 4
                    expanding.button {
                        subtext("${cols} columns")
                        onClick {
                            recyclerView?.placer = RecyclerViewPlacerVerticalGrid(cols, 1.0)
                        }
                    }
                }
                sizeConstraints(width = 10.rem).field("Element Count") {
                    numberInput { content bind elementCount.nullable().asDouble() }
                }
            }
            expanding.recyclerView(Action("refresh") {
                println("DEBUG refreshing")
            }) {
                recyclerView = this
                log = LogRoot.tag("R2")
                placer = RecyclerViewPlacerVerticalGrid(1).also { it.log = LogRoot.tag("Placer") }
//                this.snapToElements = null to Align.Start
                val main: RecyclerViewRenderer<Int> = object : RecyclerViewRenderer<Int> {
                    override fun render(
                        viewWriter: ViewWriter,
                        data: Reactive<Int>,
                        index: Reactive<Int>
                    ): Unit {
                        return with(viewWriter) {
                            card.button {
                                col {
                                    centered.text { ::content { data().toString() } }
                                    text("Tall element")
                                    text("We've got to")
                                    text("get this to fill")
                                    text("more than ")
                                    text("one whole page.")
                                    text("Tall element")
                                    text("We've got to")
                                    text("get this to fill")
                                    text("more than ")
                                    text("one whole page.")
                                    text("Tall element")
                                    text("We've got to")
                                    text("get this to fill")
                                    text("more than ")
                                    text("one whole page.")
                                    text("Tall element")
                                    text("We've got to")
                                    text("get this to fill")
                                    text("more than ")
                                    text("one whole page.")
                                    text("Tall element")
                                    text("We've got to")
                                    text("get this to fill")
                                    text("more than ")
                                    text("one whole page.")
                                }
                                onClick {
                                    if (data() == expanded.value)
                                        expanded.value = -1
                                    else
                                        expanded.value = data()
                                }
                            }
                        }
                    }
                }
//                scrollToIndex(50, Align.Center)
                rendererSet = object : RecyclerViewRendererSet<Int, Int> {
                    override fun id(item: Int): Int = item
                    override fun renderer(item: Int): RecyclerViewRenderer<Int> = main
                }
                reactive {
                    val c = elementCount()
                    data = object : RecyclerViewData<Int, Int> {
                        override val range: IntRange = 0..<c
                        override fun get(index: Int): Int {
                            if (index !in range) throw IllegalStateException("Index out of range")
                            return index
                        }
                    }
                }
            }
        }
    }
}