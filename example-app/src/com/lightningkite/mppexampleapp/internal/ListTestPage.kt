package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.ExceptionMessage
import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.DangerSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewRenderer
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveMutableList
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.lensing.lens
import kotlin.random.Random

@Routable("/tests/lists")
object ListTestPage : Page {
    data class SimpleItem(val id: Int, val name: String)

    sealed interface HeterogeneousItem {
        val id: Int
        data class TypeA(override val id: Int, val value: String) : HeterogeneousItem
        data class TypeB(override val id: Int, val count: Int) : HeterogeneousItem
        data class TypeC(override val id: Int, val flag: Boolean) : HeterogeneousItem
    }

    /**
     * Shared renderer set for heterogeneous tests.
     * Uses the multi-type DSL builder to define different renderers for each item type.
     */
    private val heterogeneousRendererSet = RecyclerViewRendererSet.multi<HeterogeneousItem, Int>(
        id = { it.id }
    ) {
        elementsMatching { it is HeterogeneousItem.TypeA } renderedAs { data ->
            important.row {
                text("TypeA:")
                space()
                text { ::content { (data() as HeterogeneousItem.TypeA).value } }
            }
        }
        elementsMatching { it is HeterogeneousItem.TypeB } renderedAs { data ->
            warning.row {
                text("TypeB:")
                space()
                text { ::content { (data() as HeterogeneousItem.TypeB).count.toString() } }
            }
        }
        elementsMatching { it is HeterogeneousItem.TypeC } renderedAs { data ->
            affirmative.row {
                text("TypeC:")
                space()
                text { ::content { (data() as HeterogeneousItem.TypeC).flag.toString() } }
            }
        }
    }

    override fun ElementWriter.CanAddTheme.render() {
        scrolling.col {
            h1 { content = "List Rendering Tests" }

            // Shared data for all homogeneous list variants
            val simpleItems = ReactiveMutableList(
                SimpleItem(1, "Alpha"),
                SimpleItem(2, "Beta"),
                SimpleItem(3, "Gamma"),
                SimpleItem(4, "Delta"),
                SimpleItem(5, "Epsilon")
            )

            val nextId = Signal(6)

            // Controls section
            fun ElementWriter.CanAddTheme.controls() = compact.col {
                row {
                    card.button {
                        text("Add Item")
                        onClick {
                            val id = nextId.value++
                            simpleItems.add(SimpleItem(id, "Item $id"))
                        }
                    }

                    card.button {
                        text("Remove First")
                        onClick {
                            if (simpleItems.isNotEmpty()) {
                                simpleItems.removeAt(0)
                            }
                        }
                    }

                    card.button {
                        text("Remove Last")
                        onClick {
                            if (simpleItems.isNotEmpty()) {
                                simpleItems.removeAt(simpleItems.lastIndex)
                            }
                        }
                    }

                    card.button {
                        text("Remove Middle")
                        onClick {
                            if (simpleItems.size > 1) {
                                simpleItems.removeAt(simpleItems.size / 2)
                            }
                        }
                    }
                }

                row {
                    card.button {
                        text("Randomize Order")
                        onClick {
                            val shuffled = simpleItems.toList().shuffled(Random.Default)
                            simpleItems.clear()
                            simpleItems.addAll(shuffled)
                        }
                    }

                    card.button {
                        text("Reverse Order")
                        onClick {
                            val reversed = simpleItems.toList().reversed()
                            simpleItems.clear()
                            simpleItems.addAll(reversed)
                        }
                    }

                    card.button {
                        text("Reset")
                        onClick {
                            simpleItems.clear()
                            simpleItems.addAll(
                                listOf(
                                    SimpleItem(1, "Alpha"),
                                    SimpleItem(2, "Beta"),
                                    SimpleItem(3, "Gamma"),
                                    SimpleItem(4, "Delta"),
                                    SimpleItem(5, "Epsilon")
                                )
                            )
                            nextId.value = 6
                        }
                    }
                }
            }

            space()

            // Test 1: Keyed with animation (RECOMMENDED)
            card.col {
                h2 { content = "1. Keyed ID + Animation (colOf - RECOMMENDED)" }
                text { content = "Uses ID diffing with animated entry/exit transitions" }

                controls()

                card.colOf(simpleItems, id = { it.id }, animate = true) { item ->
                    row {
                        text { ::content { "ID: ${item().id}" } }
                        space()
                        text { ::content { "Name: ${item().name}" } }
                    }
                }
            }

            space()

            // Test 2: Keyed without animation
            card.col {
                h2 { content = "2. Keyed ID + No Animation (colOf)" }
                text { content = "Uses ID diffing without transitions (instant show/hide)" }

                controls()

                card.colOf(simpleItems, id = { it.id }, animate = false) { item ->
                    row {
                        text { ::content { "ID: ${item().id}" } }
                        space()
                        text { ::content { "Name: ${item().name}" } }
                    }
                }
            }

            space()

            // Test 3: Positional with placeholders
            card.col {
                h2 { content = "3. Positional Slot Reuse (colOf)" }
                text { content = "Reuses views by position. Shows 3 placeholders while loading." }

                controls()

                card.colOf(
                    simpleItems,
                    placeholdersWhileLoading = 3,
                    poolCap = 10
                ) { item ->
                    row {
                        text { ::content { item().let { "ID: ${it.id}" } } }
                        space()
                        text { ::content { item().let { "Name: ${it.name}" } } }
                    }
                }
            }

            space()

            // Test 4: Expensive with animation
            card.col {
                h2 { content = "4. Full Rebuild + Animation (colOfExpensive)" }
                text { content = "LAST RESORT: Clears and recreates all views. Uses object equality for animation." }

                controls()

                card.colOfExpensive(simpleItems, animate = true) { item ->
                    row {
                        text { content = "ID: ${item.id}" }
                        space()
                        text { content = "Name: ${item.name}" }
                    }
                }
            }

            space()

            // Test 5: Expensive without animation
            card.col {
                h2 { content = "5. Full Rebuild + No Animation (colOfExpensive)" }
                text { content = "LAST RESORT: Clears and recreates all views instantly" }

                controls()

                card.colOfExpensive(simpleItems) { item ->
                    row {
                        text { content = "ID: ${item.id}" }
                        space()
                        text { content = "Name: ${item.name}" }
                    }
                }
            }

            space()

            // Test 6: renderListIn rowWrapping
            card.col {
                h2 { content = "6. renderListIn Keyed ID + rowWrapping" }
                text { content = "Uses ID diffing with animated entry/exit transitions on a custom container" }

                controls()

                card.renderListIn(ElementWriter::rowWrapping, simpleItems, id = { it.id }, animate = true) { item ->
                    row {
                        text { ::content { "ID: ${item().id}" } }
                        space()
                        text { ::content { "Name: ${item().name}" } }
                    }
                }
            }

            // Test 7: renderListIn missing setup call
            card.col {
                h2 { content = "7. renderListIn reports an error with no setup call" }

                val errorReported = Signal<ExceptionMessage?>(null)

                context.exceptionHandlers += ExceptionHandler(10f) { it, meta ->
                    if (it.message?.contains("renderListIn") == true) {
                        errorReported.value = exceptionMessage(it, meta);
                        {}
                    }
                    else null
                }

                dynamicThemed {
                    if (errorReported() != null) CardSemantic else DangerSemantic
                }.col {
                    text {
                        ::content {
                            val report = errorReported()
                            if (report != null) "All Good - Error Reported:\n\n${report.title}\n${report.body}"
                            else "ERROR - No error was reported even though setup was never called"
                        }
                    }

                    renderListIn(
                        container = { _ -> col() }, // BAD!! Doesn't call passed setup lambda
                        simpleItems,
                        id = { it.id },
                        animate = true
                    ) { item ->
                        row {
                            text { ::content { "ID: ${item().id}" } }
                            space()
                            text { ::content { "Name: ${item().name}" } }
                        }
                    }

                    shownWhen { !errorReported()?.actions.isNullOrEmpty() }.rowOf(errorReported.lens { it?.actions.orEmpty() }) {
                        card.button {
                            text { ::content { it().title } }
                            ::action bind it
                        }
                    }
                }
            }

            // Test 6: Heterogeneous with animation
            card.col {
                h2 { content = "6. Heterogeneous + Animation (colOf with RecyclerViewRendererSet)" }
                text { content = "Multiple renderer types with keyed diffing and animation" }

                val heteroItems = ReactiveMutableList<HeterogeneousItem>(
                    HeterogeneousItem.TypeA(1, "First"),
                    HeterogeneousItem.TypeB(2, 42),
                    HeterogeneousItem.TypeC(3, true),
                    HeterogeneousItem.TypeA(4, "Second"),
                    HeterogeneousItem.TypeB(5, 99)
                )

                row {
                    button {
                        text("Add TypeA")
                        onClick {
                            val id = heteroItems.maxOfOrNull { it.id }?.plus(1) ?: 1
                            heteroItems.add(HeterogeneousItem.TypeA(id, "Item $id"))
                        }
                    }

                    button {
                        text("Add TypeB")
                        onClick {
                            val id = heteroItems.maxOfOrNull { it.id }?.plus(1) ?: 1
                            heteroItems.add(HeterogeneousItem.TypeB(id, Random.nextInt(100)))
                        }
                    }

                    button {
                        text("Add TypeC")
                        onClick {
                            val id = heteroItems.maxOfOrNull { it.id }?.plus(1) ?: 1
                            heteroItems.add(HeterogeneousItem.TypeC(id, Random.nextBoolean()))
                        }
                    }

                    button {
                        text("Remove Random")
                        onClick {
                            if (heteroItems.isNotEmpty()) {
                                heteroItems.removeAt(Random.nextInt(heteroItems.size))
                            }
                        }
                    }

                    button {
                        text("Shuffle")
                        onClick {
                            val shuffled = heteroItems.toList().shuffled()
                            heteroItems.clear()
                            heteroItems.addAll(shuffled)
                        }
                    }
                }

                card.colOf(heteroItems, heterogeneousRendererSet, animate = true)
            }

            space()

            // Test 7: Heterogeneous positional (no animation)
            card.col {
                h2 { content = "7. Heterogeneous + Positional (colOf with RecyclerViewRendererSet)" }
                text { content = "Multiple renderer types with positional slot reuse (maximum efficiency)" }

                val heteroItemsPositional = ReactiveMutableList<HeterogeneousItem>(
                    HeterogeneousItem.TypeA(101, "Pos-1"),
                    HeterogeneousItem.TypeB(102, 10),
                    HeterogeneousItem.TypeC(103, false),
                    HeterogeneousItem.TypeA(104, "Pos-2")
                )

                row {
                    button {
                        text("Add Random Type")
                        onClick {
                            val id = heteroItemsPositional.maxOfOrNull { it.id }?.plus(1) ?: 101
                            val item = when (Random.nextInt(3)) {
                                0 -> HeterogeneousItem.TypeA(id, "Pos-$id")
                                1 -> HeterogeneousItem.TypeB(id, Random.nextInt(100))
                                else -> HeterogeneousItem.TypeC(id, Random.nextBoolean())
                            }
                            heteroItemsPositional.add(item)
                        }
                    }

                    button {
                        text("Remove First")
                        onClick {
                            if (heteroItemsPositional.isNotEmpty()) {
                                heteroItemsPositional.removeAt(0)
                            }
                        }
                    }

                    button {
                        text("Replace All")
                        onClick {
                            heteroItemsPositional.clear()
                            repeat(5) { i ->
                                val id = 200 + i
                                val item = when (i % 3) {
                                    0 -> HeterogeneousItem.TypeA(id, "New-$id")
                                    1 -> HeterogeneousItem.TypeB(id, i * 10)
                                    else -> HeterogeneousItem.TypeC(id, i % 2 == 0)
                                }
                                heteroItemsPositional.add(item)
                            }
                        }
                    }
                }

                card.colOf(heteroItemsPositional, heterogeneousRendererSet, animate = false, poolCap = 10)
            }

            space()
        }
    }
}