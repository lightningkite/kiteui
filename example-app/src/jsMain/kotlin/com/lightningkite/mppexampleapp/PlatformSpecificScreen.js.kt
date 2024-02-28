package com.lightningkite.mppexampleapp

import com.lightningkite.rock.dom.CSSStyleDeclaration
import com.lightningkite.rock.models.Align
import com.lightningkite.rock.models.dp
import com.lightningkite.rock.reactive.*
import com.lightningkite.rock.views.*
import com.lightningkite.rock.views.direct.*
import kotlinx.browser.window
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.math.abs
import kotlin.math.roundToInt

val data = Property<List<String>>((0..2500).map { "Item $it" }.toList())

actual fun ViewWriter.platformSpecific() {
//    val current = Property(0)
//    col {
//        val showExtra = Property(true)
//        button {
//            onClick { showExtra.value = !showExtra.value }
//            text("Toggle")
////            ::exists { native.assistAnimateHidden(); current.await() != 4 }
//        }
//        important - button {
//            text("Animation Test Toggle")
//            reactiveScope {
//                println("Animating? $animationsEnabled")
//                exists = !(showExtra.await())
//            }
////            ::exists { native.assistAnimateHidden(); current.await() != 4 }
//        }
//        important - button {
//            onClick { current set 1 }
//            text("Animation Test 1")
//            reactiveScope { exists = current.await() != 1 }
//        }
//        important - button {
//            onClick { current set 2 }
//            text("Animation Test 2")
//            reactiveScope { exists = current.await() != 2 }
//        }
//        important - button {
//            onClick { current set 3 }
//            text("Animation Test 3")
//            reactiveScope { exists = current.await() != 3 }
//        }
//        important - button {
//            onClick { current set 4 }
//            text("Animation Test 4")
//            reactiveScope { exists = current.await() != 4 }
//        }
//    }

    col {
        var controller: RecyclerController? = null
        button {
            text("Alter Data")
            var version = 2
            onClick {
                val v = version++
                if (v % 2 == 0) data.value = (0..50).map { "Item $it (v$v)" }.toList()
                else data.value = (0..20).map { "Item $it (v$v)" }.toList()
            }
        }
        var rv: RecyclerView? = null
        expanding - horizontalRecyclerView {
            rv = this
            columns = 1
            children(data) { obs ->
                card - sizeConstraints(minWidth = 100.dp, minHeight = 100.dp) - stack {
                    text {
                        ::content { obs.await() }
                    }
                }
            }
        }
        row {
            button {
                text("Jump S")
                onClick {
                    rv!!.scrollToIndex(100, Align.Start)
                }
            }
            button {
                text("Jump C")
                onClick {
                    rv!!.scrollToIndex(100, Align.Center)
                }
            }
            button {
                text("Jump E")
                onClick {
                    rv!!.scrollToIndex(100, Align.End)
                }
            }
        }
    }
}