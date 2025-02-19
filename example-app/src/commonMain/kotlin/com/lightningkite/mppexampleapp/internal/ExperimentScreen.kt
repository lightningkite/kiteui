package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.Resources
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Routable("experiment")
object ExperimentScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        scrolls - col {
            spacing = 0.5.rem
            paddingByEdge = Edges(left = 3.rem, top = 1.rem, right = 0.rem, bottom = 2.rem)
            h1("Weird spacing time")
            spacingOverrideBeforeNext(10.rem)
            text("Really far down")
            spacingOverrideBeforeNext(0.rem)
            text("Really close")
            spacingOverrideBeforeNext(0.rem)
            text {
                content = "Really close"
                exists = false
            }
            spacingOverrideBeforeNext(1.rem)
            text("Really close")
        }
//        col {
//            expanding - recyclerView {
//                log = ConsoleRoot.tag("X")
////                children(Constant((1..20).toList()), id = { it }) {
////                    text { ::content { it().toString() } }
////                }
//                childrenMultipleTypes(Constant((1..200).toList()), id = { it }) {
//                    println("Building...")
//                    elementsMatching { it % 2 == 0 } renderedAs { text { ::content { it().toString() } } }
//                    elementsMatching { it % 2 == 1 } renderedAs { card - text { ::content { it().toString() } } }
//                }
//                println("OK")
//            }
//        }

//        col {
//            val expanded = Property(-1)
//            val data = Property((1..10).toList())
//            var recyclerView: Recycler2? = null
//            expanding
//            recyclerView = Recycler2(this, vertical = false).apply {
////                log = ConsoleRoot.tag("R2")
//                scrollToIndex(2, Align.Center, animate = false)
//                this.snapToElements = Align.Center
//                this.scrollSnapStop = true
//                val main: RecyclerViewRenderer<Int> = object : RecyclerViewRenderer<Int> {
//                    override fun render(viewWriter: ViewWriter, data: Readable<Int>, index: Readable<Int>) =
//                        with(viewWriter) {
//                            card - button {
//                                sizeConstraints(minHeight = 10.rem) - col {
//                                    text { ::content { data().toString() } }
//                                    onlyWhen { expanded() == data() } - col {
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                    }
//                                }
//                                onClick {
//                                    expanded.value = data()
//                                }
//                            }
//
//                        }
//                }
////                scrollToIndex(50, Align.Center)
//                placer = RecyclerViewPagingPlacer().apply { log = ConsoleRoot.tag("RVP2") }
//                rendererSet = object : RecyclerViewRendererSet<Int, Int> {
//                    override fun id(item: Int): Int = item
//                    override fun renderer(item: Int): RecyclerViewRenderer<Int> = main
//                }
//                reactive {
//                    val d = data()
//                    this@apply.data = object : RecyclerViewData<Int, Int> {
//                        override val range: IntRange = d.indices
//                        override fun get(index: Int): Int = d[index]
//                    }
//                }
//            }
//            row {
//                button {
//                    text("left")
//                    onClick {
//                        recyclerView.centerIndex set recyclerView.centerIndex() - 1
//                    }
//                }
//                button {
//                    text("delete")
//                    onClick {
//                        val toRemove = data().get(recyclerView.centerIndex())
//                        data.value = data.value.filter { it != toRemove }
//                    }
//                }
//                button {
//                    text("right")
//                    onClick {
//                        recyclerView.centerIndex set recyclerView.centerIndex() + 1
//                    }
//                }
//            }
//        }
    }
}