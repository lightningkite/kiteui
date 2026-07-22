package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*

@Routable("foreach-by-id-test")
object ForEachByIdTestPage : Page {

    private data class Item(val id: Int, val name: String)

    private val full = listOf(
        Item(1, "A"),
        Item(2, "B"),
        Item(3, "C"),
        Item(4, "D"),
        Item(5, "E"),
    )

    override fun ElementWriter.CanAddTheme.render(): Unit {
        scrolling.col {
            h1("renderList Test")
            text(
                "Reproduces the bug where filtering down (especially to a single item that wasn't first) " +
                "and then restoring the full list produced the wrong visual order. " +
                "Compare the animated, non-animated, and reference columns — all three should match the 'Expected' line."
            )

            val items = Signal(full)

            h2("Controls")
            col {
                row {
                    button {
                        text("All")
                        action = Action("All", frequencyCap = null) { items.value = full }
                    }
                    button {
                        text("First only (A)")
                        action = Action("First only", frequencyCap = null) { items.value = full.take(1) }
                    }
                    button {
                        text("Middle only (C)")
                        action = Action("Middle only", frequencyCap = null) { items.value = listOf(full[2]) }
                    }
                    button {
                        text("Last only (E)")
                        action = Action("Last only", frequencyCap = null) { items.value = full.takeLast(1) }
                    }
                    button {
                        text("None")
                        action = Action("None", frequencyCap = null) { items.value = emptyList() }
                    }
                }
                row {
                    button {
                        text("Odd indices (A, C, E)")
                        action = Action("Odd indices", frequencyCap = null) {
                            items.value = full.filterIndexed { i, _ -> i % 2 == 0 }
                        }
                    }
                    button {
                        text("Even indices (B, D)")
                        action = Action("Even indices", frequencyCap = null) {
                            items.value = full.filterIndexed { i, _ -> i % 2 == 1 }
                        }
                    }
                    button {
                        text("Reversed")
                        action = Action("Reversed", frequencyCap = null) { items.value = full.reversed() }
                    }
                    button {
                        text("Swap ends (E, B, C, D, A)")
                        action = Action("Swap ends", frequencyCap = null) {
                            items.value = listOf(full.last()) + full.subList(1, full.size - 1) + listOf(full.first())
                        }
                    }
                }
            }

            card.text {
                ::content { "Expected: [" + items().joinToString(", ") { it.name } + "]" }
            }

            row {
                expanding.card.col {
                    h2("renderList (animated)")
                    colOf(items, id = { it.id }) { item ->
                        card.text {
                            ::content { item().name }
                            ::debugName { item().name }
                        }
                    }
                }
                expanding.card.col {
                    h2("renderList (no animation)")
                    colOf(items, id = { it.id }, animate = false) { item ->
                        card.text {
                            ::content { item().name }
                            ::debugName { item().name }
                        }
                    }
                }
                expanding.card.col {
                    h2("Reference (renderList unkeyed)")
                    colOf(items) { item ->
                        card.text {
                            ::content { item().name }
                            ::debugName { item().name }
                        }
                    }
                }
            }
        }
    }
}
