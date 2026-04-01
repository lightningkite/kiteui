// by Claude
package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Test page for WindowedList virtualized list implementation.
 * Tests scrolling, jumping, dynamic data manipulation, edge cases, and stress scenarios.
 * by Claude
 */
@Routable("windowed-list-test")
object WindowedListTestPage : Page {
    override val title: Reactive<String> = Constant("WindowedList Test")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        col {
            // Data source - mutable list of ints, each int is a unique item ID - by Claude
            var nextId = 100
            val data = Signal((0 until 100).toList())
            val variableHeights = Signal(false)

            // Shared signals for visible indices - by Claude
            val firstVisible = Signal(0)
            val lastVisible = Signal(0)
            val centerVisible = Signal(0)
            var list: WindowedList<Int>? = null
            var rapidMutationJob: Job? = null

            // Header
            text("WindowedList: Spacers + Transform Anchoring")

            // Status row - by Claude
            row {
                expanding.col {
                    text { ::content { "Items: ${data().size}" } }
                    text { ::content { "First: ${firstVisible()} | Last: ${lastVisible()} | Center: ${centerVisible()}" } }
                }
            }

            // Jump controls - by Claude
            row {
                for (align in listOf(Align.Start, Align.Center, Align.End)) {
                    expanding.button {
                        text("Jump ${align.name} 50")
                        onClick { list?.scrollToIndex(50, align, true) }
                    }
                }
                expanding.button {
                    text("Jump End")
                    onClick { list?.scrollToIndex(data.value.size - 1, Align.End, true) }
                }
            }

            // Data manipulation controls - by Claude
            row {
                expanding.button {
                    text("Insert at Start")
                    onClick {
                        data.value = listOf(nextId++) + data.value
                    }
                }
                expanding.button {
                    text("Insert at Center")
                    onClick {
                        val mList = data.value.toMutableList()
                        val idx = centerVisible.value.coerceIn(0, mList.size)
                        mList.add(idx, nextId++)
                        data.value = mList
                    }
                }
                expanding.button {
                    text("Insert at End")
                    onClick {
                        data.value = data.value + nextId++
                    }
                }
            }

            row {
                expanding.button {
                    text("Remove at Start")
                    onClick {
                        if (data.value.isNotEmpty()) data.value = data.value.drop(1)
                    }
                }
                expanding.button {
                    text("Remove at Center")
                    onClick {
                        val mList = data.value.toMutableList()
                        if (mList.isNotEmpty()) {
                            val idx = centerVisible.value.coerceIn(0, mList.lastIndex)
                            mList.removeAt(idx)
                            data.value = mList
                        }
                    }
                }
                expanding.button {
                    text("Remove at End")
                    onClick {
                        if (data.value.isNotEmpty()) data.value = data.value.dropLast(1)
                    }
                }
            }

            row {
                expanding.button {
                    text("Insert 10 at Center")
                    onClick {
                        val mList = data.value.toMutableList()
                        val idx = centerVisible.value.coerceIn(0, mList.size)
                        for (i in 0 until 10) mList.add(idx, nextId++)
                        data.value = mList
                    }
                }
                expanding.button {
                    text("Remove 10 at Center")
                    onClick {
                        val mList = data.value.toMutableList()
                        val idx = centerVisible.value.coerceIn(0, mList.lastIndex)
                        repeat(10) { if (mList.isNotEmpty() && idx <= mList.lastIndex) mList.removeAt(idx.coerceAtMost(mList.lastIndex)) }
                        data.value = mList
                    }
                }
                expanding.button {
                    text("Shuffle")
                    onClick {
                        data.value = data.value.shuffled()
                    }
                }
                expanding.button {
                    text("Reset 100")
                    onClick {
                        nextId = 100
                        data.value = (0 until 100).toList()
                    }
                }
            }

            // Edge case data sizes - by Claude
            row {
                expanding.button {
                    text("Empty")
                    onClick { data.value = emptyList() }
                }
                expanding.button {
                    text("1 Item")
                    onClick { nextId = 1; data.value = listOf(0) }
                }
                expanding.button {
                    text("2 Items")
                    onClick { nextId = 2; data.value = listOf(0, 1) }
                }
                expanding.button {
                    text("1000 Items")
                    onClick { nextId = 1000; data.value = (0 until 1000).toList() }
                }
            }

            row {
                expanding.button {
                    text("10000 Items")
                    onClick { nextId = 10000; data.value = (0 until 10000).toList() }
                }
                expanding.button {
                    text { ::content { if (variableHeights()) "Uniform Heights" else "Variable Heights" } }
                    onClick { variableHeights.value = !variableHeights.value }
                }
                expanding.button {
                    text("Replace All")
                    onClick {
                        // Simulate server refresh: completely replace data - by Claude
                        val newStart = nextId
                        nextId += data.value.size.coerceAtLeast(50)
                        data.value = (newStart until nextId).toList()
                    }
                }
            }

            // Stress testing - by Claude
            row {
                expanding.button {
                    text("Rapid Mutations (Start)")
                    onClick {
                        rapidMutationJob?.cancel()
                        rapidMutationJob = launch {
                            repeat(100) {
                                val mList = data.value.toMutableList()
                                if (it % 2 == 0) {
                                    // Insert at random position
                                    val idx = if (mList.isEmpty()) 0 else (0..mList.size).random()
                                    mList.add(idx, nextId++)
                                } else {
                                    // Remove at random position
                                    if (mList.isNotEmpty()) mList.removeAt((0..mList.lastIndex).random())
                                }
                                data.value = mList
                                delay(50)
                            }
                            rapidMutationJob = null
                        }
                    }
                }
                expanding.button {
                    text("Rapid Mutations (Stop)")
                    onClick {
                        rapidMutationJob?.cancel()
                        rapidMutationJob = null
                    }
                }
            }

            // The windowed list - by Claude
            expanding.frame {
                val useVariableHeights = variableHeights.state.getOrNull() ?: false
                val wl = windowedList(data, {
                    log = Log.tag("WL")
                    this.initialRenderCount = 10
                    this.initialRenderIndex = 0
                }) { item, index ->
                    card.button {
                        col {
                            centered.text { ::content { "Item #${item()} (idx ${index()})" } }
                            text("Content line 1")
                            text("Content line 2")
                            if (useVariableHeights) {
                                // Wildly varying heights: 1-8 extra lines based on item value - by Claude
                                val extraLines = (item.state.getOrNull() ?: 0) % 8
                                for (i in 0 until extraLines) {
                                    text("Extra line $i for item #${item.state.getOrNull()}")
                                }
                            } else {
                                // Moderate variation: extra lines for items divisible by 5 - by Claude
                                if ((item.state.getOrNull() ?: 0) % 5 == 0) {
                                    text("Extra line for items divisible by 5")
                                    text("And another extra line")
                                }
                            }
                        }
                        onClick {
                            println("Clicked item #${item.state.getOrNull()} at index ${index.state.getOrNull()}")
                        }
                    }
                }
                list = wl
                // Bind local signals to the list's signals - by Claude
                reactive {
                    firstVisible.value = wl.firstVisibleIndex()
                    lastVisible.value = wl.lastVisibleIndex()
                    centerVisible.value = wl.centerIndex()
                }
            }
        }
    }
}
