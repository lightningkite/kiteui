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

@Routable("r2vp")
object R2VPScreen : Screen {
    override val title: Readable<String>
        get() = super.title

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
            expanding
            recyclerView = Recycler2(this, vertical = false).apply {
                this.snapToElements = Align.Center
                val main: RecyclerViewRenderer<Int> = object: RecyclerViewRenderer<Int> {
                    override fun render(viewWriter: ViewWriter, data: Readable<Int>, index: Readable<Int>) {
                        with(viewWriter) {
                            ThemeDerivation {
                                it.copy(background = HSVColor(hue = Angle(Random.nextFloat()), saturation = 1f, value = 0.5f).toRGB()).withBack
                            }.onNext - button {
                                sizeConstraints(minHeight = 10.rem) - col {
                                    text { ::content { data().toString() } }
                                    onlyWhen { expanded() == data() } - col {
                                        text { content = "Expanded Content"  }
                                        text { content = "Expanded Content"  }
                                        text { content = "Expanded Content"  }
                                        text { content = "Expanded Content"  }
                                        text { content = "Expanded Content"  }
                                        text { content = "Expanded Content"  }
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
                placer = RecyclerViewPagingPlacer()
                rendererSet = object: RecyclerViewRendererSet<Int, Int> {
                    override fun id(item: Int): Int = item
                    override fun renderer(item: Int): RecyclerViewRenderer<Int> = main
                }
                data = object: RecyclerViewData<Int, Int> {
                    override val range: IntRange = 0..100
                    override fun get(index: Int): Int {
                        if(index !in range) throw IllegalStateException("Index out of range")
                        return index
                    }
                }
            }
        }
    }
}