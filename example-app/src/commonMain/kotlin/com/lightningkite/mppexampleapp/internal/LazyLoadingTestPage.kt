package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.lazyColumn
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.delay

@Routable("lazy-loading-test")
object LazyLoadingTestPage : Page {
    override val title: Reactive<String> get() = Constant("Lazy Loading Test")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val items = Signal<List<Int>>(emptyList())
        val pageSize = 30

        col {
            h1 { content = "Lazy Loading Column" }
            text { ::content { "Loaded: ${items().size} items" } }
            separator()

            expanding.lazyColumn(
                items = items,
                id = { it },
                loadMore = {
                    delay(500) // Simulate network delay
                    val current = items.value
                    val next = (current.size until current.size + pageSize).toList()
                    items.value = current + next
                }
            ) { item ->
                card.row {
                    expanding.text { ::content { "Item #${item()}" } }
                    subtext { ::content { "Page ${item() / pageSize + 1}" } }
                }
            }
        }
    }
}
