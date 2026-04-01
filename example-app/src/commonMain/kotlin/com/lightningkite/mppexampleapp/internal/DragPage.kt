package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.models.DragShadow
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.lighten
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.childrenReorderable
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.forEachReorderable
import com.lightningkite.kiteui.views.themed
import com.lightningkite.kiteui.views.dynamicTheme
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlinx.coroutines.launch




@Routable("drag")
object DragPage : Page {

    val numbers = Signal(List(9) { it + 1 })

    private data class Highlight(val amount: Int) : Semantic("highlight-$amount") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(
            background = theme.background.lighten(amount/50f)
        )
    }

    override fun ElementWriter.CanAddTheme.render(): Unit {
        scrolling.col {
            val title = atStart.h2("Drag test")

            h4("Reorderable List")
            themed(ListSemantic).col {
                forEachReorderable(
                    numbers,
                    reorder = { move ->
                        numbers.value = move.reorder(numbers.value)
                    },
                    dataTransform = {
                        it.copy(
                            dragShadow = DragShadow(title, xAlign = Align.Start, xOffset = (-1).rem)
                        )
                    }
                ) { number ->
                    card.frame {
                        dynamicTheme { Highlight(number()) }
                        centered.text { ::content { number().toString() } }
                    }
                }
            }

            space()

            h4("Recycler Reorderable")
            sizeConstraints(height = 20.rem).onNext(ListSemantic).recyclerView {
                placer = RecyclerViewPlacerVerticalGrid(3)
                childrenReorderable(
                    numbers,
                    id = { it },
                    reorder = { move ->
                        numbers.modify { move.reorder(it) }
                    }
                ) { number ->
                    card.frame {
                        dynamicTheme { Highlight(number()) }
                        centered.text { ::content { number().toString() } }
                    }
                }
            }

            space()

            text("Behold some dragging magic!")
            card.link {
                to = { this@DragPage }
                dragData = DragData("Stuff", "x-application/thing", "Hello there!")
                text {
                    content = "Dragging from here leaves the text 'Hello there!'"
                }
            }
            field("Sample input") { textInput { } }
            card.frame {
                text("Print dropped item to console")
                dropTargetDelegate = object : DropTargetDelegate {
                    override fun drop(event: DragEvent): Boolean {
                        println(event.data.data)
                        return true
                    }
                }
            }
            sizeConstraints(height = 10.rem).row {
                val left = Signal<List<String>>(listOf())
                val right = Signal<List<String>>(listOf())
                expanding.card.scrolling.col {
                    dropTargetDelegate = object : DropTargetDelegate {
                        override fun drop(event: DragEvent): Boolean {
                            val it = event.data
                            left.value += it.data
                            right.value -= it.data
                            return true
                        }
                    }
                    forEachAnimated(left) {
                        card.text {
                            content = it
                            dragData = DragData(it, "text/plain", it)
                        }
                    }
                }
                expanding.card.scrolling.col {
                    dropTargetDelegate = object : DropTargetDelegate {
                        override fun drop(event: DragEvent): Boolean {
                            val it = event.data
                            right.value += it.data
                            left.value -= it.data
                            return true
                        }
                    }
                    forEachAnimated(right) {
                        card.text {
                            content = it
                            dragData = DragData(it, "text/plain", it)
                        }
                    }
                }
            }
            text("Janky reorderable test")
            sizeConstraints(height = 30.rem).card.recyclerView {
                val data = Signal<List<String>>(listOf("A", "B", "C", "D", "E"))
                children(data, { it }) {
                    card.text {
                        ::content { it() }
                        ::dragData { DragData(it(), "text/plain", it()) }
                        dropTargetDelegate = object : DropTargetDelegate {
                            override fun drop(event: DragEvent): Boolean {
                                launch {
                                    val dropped = event.data
                                    val index = data.value.indexOf(it())
                                    val t = data.value.filter { it != dropped.data }
                                    data.value = t.subList(0, index) + dropped.data + t.subList(index, t.size)
                                }
                                return true
                            }
                        }
                    }
                }
                outerFrame.dropTargetDelegate = object : DropTargetDelegate {
                    override fun drop(event: DragEvent): Boolean {
                        val it = event.data
                        // Move it to the end
                        data.value -= it.data
                        data.value += it.data
                        return true
                    }
                }
            }
        }
    }

}