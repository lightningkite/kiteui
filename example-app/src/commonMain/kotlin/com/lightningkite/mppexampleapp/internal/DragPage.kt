package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.AudioSource
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.mppexampleapp.Resources
import kotlinx.coroutines.launch

@Routable("drag")
object DragPage : Page {

    override fun ViewWriter.render(): ViewModifiable = col {
        h2("Drag test")
        text("Behold some dragging magic!")
        card - text {
            content = "Dragging from here leaves the text 'Hello there!'"
            dragData = DragData("Stuff", "text/plain", "Hello there!")
        }
        field("Sample input") { textInput {  }}
        card - frame {
            text("Print dropped item to console")
            onDrop = {
                println(it)
                true
            }
        }
        sizeConstraints(height = 10.rem) - row {
            val left = Property<List<String>>(listOf())
            val right = Property<List<String>>(listOf())
            expanding - card - scrolling - col {
                onDrop = {
                    left.value += it.data
                    right.value -= it.data
                    true
                }
                forEachAnimated(left) {
                    card - text {
                        content = it
                        dragData = DragData(it, "text/plain", it)
                    }
                }
            }
            expanding - card - scrolling - col {
                onDrop = {
                    right.value += it.data
                    left.value -= it.data
                    true
                }
                forEachAnimated(right) {
                    card - text {
                        content = it
                        dragData = DragData(it, "text/plain", it)
                    }
                }
            }
        }
        text("Janky reorderable test")
        sizeConstraints(height = 30.rem) - card - recyclerView {
            val data = Property<List<String>>(listOf("A", "B", "C", "D", "E"))
            children(data, { it }) {
                card - text {
                    ::content { it() }
                    ::dragData { DragData(it(), "text/plain", it()) }
                    onDrop = { dropped ->
                        launch {
                            val index = data.value.indexOf(it())
                            val t = data.value.filter { it != dropped.data }
                            data.value = t.subList(0, index) + dropped.data + t.subList(index, t.size)
                        }
                        true
                    }
                }
            }
            onDrop = {
                // Move it to the end
                data.value -= it.data
                data.value += it.data
                true
            }
        }
    }

}