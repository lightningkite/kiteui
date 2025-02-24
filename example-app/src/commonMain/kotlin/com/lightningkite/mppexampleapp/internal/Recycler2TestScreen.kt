package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import kotlin.random.Random

@Routable("recycler2-test")
object Recycler2TestScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    @QueryParameter
    val elementCount = Property(10000)

    override fun ViewWriter.render() {
        col {
            val expanded = Property(-1)
            var recyclerView: Recycler2? = null
            row {
                for (align in Align.values()) {
                    expanding - button {
                        subtext("Jump ${align.name}")
                        onClick { recyclerView?.scrollToIndex(75, align, false) }
                    }
                }
            }
            row {
                for (align in Align.values()) {
                    expanding - button {
                        subtext("Scroll ${align.name}")
                        onClick { recyclerView?.scrollToIndex(75, align, true) }
                    }
                }
            }
            row {
                repeat(4) {
                    val cols = (it + 1) * 4
                    expanding - button {
                        subtext("${cols} columns")
                        onClick {
                            recyclerView?.placer = RecyclerViewPlacerVerticalGrid(cols, 1.0)
                        }
                    }
                }
                sizeConstraints(width = 10.rem) - field("Element Count") {
                    numberInput { content bind elementCount.nullable().asDouble() }
                }
            }
            recyclerView = expanding - Recycler2(this).apply {
                log = ConsoleRoot.tag("R2")
                placer = RecyclerViewPlacerVerticalGrid(1).also { it.log = ConsoleRoot.tag("Placer") }
//                this.snapToElements = null to Align.Start
                val main: RecyclerViewRenderer<Int> = object : RecyclerViewRenderer<Int> {
                    override fun render(
                        viewWriter: ViewWriter,
                        data: Readable<Int>,
                        index: Readable<Int>
                    ): ViewModifiable {
                        return with(viewWriter) {
                            card - button {
                                col {
                                    centered - text { ::content { data().toString() } }
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