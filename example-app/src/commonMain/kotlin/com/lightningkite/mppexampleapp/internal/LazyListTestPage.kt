package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.LazyListHandle
import com.lightningkite.kiteui.views.l2.lazyList
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal

@Routable("lazy-list-test")
object LazyListTestPage : Page {
    override val title: Reactive<String> = Constant("Lazy List Test")

    override fun ViewWriter.render(): Unit = run {
        col {
            h1 { content = "Lazy List Test" }
            text("This page tests the LazyList component with 1000 items.")

            // Create a list of 100 items
            val items = Constant((0 until 100).toList())

            // Store handle in a nullable signal so we can access it reactively
            val handleSignal = Signal<LazyListHandle?>(null)

            row {
                text {
                    ::content {
                        val handle = handleSignal()
                        if (handle != null) {
                            "Visible: ${handle.firstVisibleIndex()}..${handle.lastVisibleIndex()}"
                        } else {
                            "Loading..."
                        }
                    }
                }
                button {
                    text("Scroll to 50")
                    onClick { handleSignal.value?.scrollToIndex(50) }
                }
                button {
                    text("Scroll to 0")
                    onClick { handleSignal.value?.scrollToIndex(0) }
                }
            }

            separator()

            // Use the lazy list
            expanding.lazyList(
                items = items,
                itemHeight = 4.rem,
                overdraw = 5
            ) { itemData: Reactive<Int> ->
                card.row {
                    expanding.text {
                        ::content { "Item #${itemData()}" }
                    }
                    text {
                        ::content { "Value: ${itemData() * 2}" }
                    }
                }
            }.also { handleSignal.value = it }
        }
    }
}
