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

@Routable("r2vp")
object R2VPPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
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
            expanding
            recyclerView = Recycler2(this, vertical = false).apply {
//                log = ConsoleRoot.tag("R2")
                scrollToIndex(2, Align.Center, animate = false)
                this.snapToElements = Align.Center
                this.scrollSnapStop = true
                val main: RecyclerViewRenderer<Int> = object : RecyclerViewRenderer<Int> {
                    override fun render(viewWriter: ViewWriter, data: Reactive<Int>, index: Reactive<Int>): Unit =
                        with(viewWriter) {
                            padded.stack {
                                card.button {
                                    sizeConstraints(minHeight = 10.rem).col {
                                        text { ::content { data().toString() } }
                                        shownWhen { expanded() == data() }.col {
                                            text { content = "Expanded Content" }
                                            text { content = "Expanded Content" }
                                            text { content = "Expanded Content" }
                                            text { content = "Expanded Content" }
                                            text { content = "Expanded Content" }
                                            text { content = "Expanded Content" }
                                        }
                                    }
                                    onClick {
                                        expanded.value = data()
                                    }
                                }
                            }

                        }
                }
//                scrollToIndex(50, Align.Center)
                placer = RecyclerViewPagingPlacer().apply { log = LogRoot.tag("RVP2") }
                rendererSet = object : RecyclerViewRendererSet<Int, Int> {
                    override fun id(item: Int): Int = item
                    override fun renderer(item: Int): RecyclerViewRenderer<Int> = main
                }
                data = object : RecyclerViewData<Int, Int> {
                    override val range: IntRange = 0..100
                    override fun get(index: Int): Int {
                        if (index !in range) throw IllegalStateException("Index out of range")
                        return index
                    }
                }
            }
            row {
                button {
                    text("left")
                    onClick {
                        recyclerView.centerIndex set recyclerView.centerIndex() - 1
                    }
                }
                button {
                    text("right")
                    onClick {
                        recyclerView.centerIndex set recyclerView.centerIndex() + 1
                    }
                }
            }
        }
    }
}