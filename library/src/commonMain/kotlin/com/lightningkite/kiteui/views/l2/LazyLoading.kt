package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.load
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.viewUnits
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

/**
 * Renders children from a reactive list with lazy loading when scrolled near the end.
 * Uses [forEachById] for ID-based diffing with animations.
 *
 * Caller owns [items] and [loadMore]. This function renders children, monitors scroll position,
 * and calls [loadMore] when the user scrolls within [threshold] of the end.
 *
 * Stops loading automatically when [loadMore] completes without the list growing.
 * Retries are allowed after errors.
 */
fun <T, ID> RowOrCol.childrenLazyLoading(
    scroll: ScrollingBehaviors,
    items: Reactive<List<T>>,
    id: (T) -> ID,
    threshold: Dimension = 20.rem,
    loadMore: suspend () -> Unit,
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) {
    var isLoading = false
    var exhausted = false
    var sizeAtLoadStart = -1

    forEachById(items, id, render = render)

    val isVertical = this.vertical
    withoutLoadingAnimations {
        reactive {
            val list = items()
            val vp = scroll.viewport()
            val ct = scroll.content()

            // Detect exhaustion: load completed but list didn't grow
            if (sizeAtLoadStart >= 0 && !isLoading && list.size <= sizeAtLoadStart) {
                exhausted = true
            }

            val contentEnd = if (isVertical) ct.bottom else ct.right
            val viewportEnd = if (isVertical) vp.bottom else vp.right
            val distanceToEnd = contentEnd - viewportEnd
            println("${distanceToEnd} (${contentEnd} - ${viewportEnd}) < ${threshold.viewUnits}")
            if (distanceToEnd < threshold.viewUnits && !isLoading && !exhausted) {
                sizeAtLoadStart = list.size
                isLoading = true
                load {
                    try {
                        loadMore()
                    } catch (e: Throwable) {
                        sizeAtLoadStart = -1  // Allow retry on error
                        throw e
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }
}

/**
 * Creates a scrolling column that renders children with lazy loading.
 * Convenience wrapper around [childrenLazyLoading] that creates the scrolling container.
 */
fun <T, ID> ElementWriter.CanAddScrolling.lazyColumn(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    threshold: Dimension = 20.rem,
    loadMore: suspend () -> Unit,
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
): RowOrCol {
    lateinit var scroll: ScrollingBehaviors
    return scrolling { scroll = this }.col {
        childrenLazyLoading(scroll, items, id, threshold, loadMore, render)
    }
}

/**
 * Creates a scrolling row that renders children with lazy loading.
 * Convenience wrapper around [childrenLazyLoading] that creates the scrolling container.
 */
fun <T, ID> ElementWriter.CanAddScrolling.lazyRow(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    threshold: Dimension = 20.rem,
    loadMore: suspend () -> Unit,
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
): RowOrCol {
    lateinit var scroll: ScrollingBehaviors
    return scrollingHorizontally { scroll = this }.row {
        childrenLazyLoading(scroll, items, id, threshold, loadMore, render)
    }
}
