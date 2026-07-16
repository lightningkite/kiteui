package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.load
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.viewUnits
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Renders children from a reactive list with lazy loading when scrolled near the end.
 * Uses [renderList] for ID-based diffing without animations.
 *
 * Caller owns [items] and [loadMore]. This function renders children, monitors scroll position,
 * and calls [loadMore] when the user scrolls within [threshold] of the end.
 *
 * Stops loading automatically when [loadMore] completes without the list growing.
 * Retries are allowed after errors.
 *
 * @deprecated Use [lazyColumn] or [lazyRow] instead, which handle rendering + lazy loading together.
 */
@OptIn(InternalKiteUi::class)
@Deprecated(
    "Use lazyColumn or lazyRow instead",
    ReplaceWith("lazyColumn(items, id, threshold, loadMore, render)", "com.lightningkite.kiteui.views.l2.lazyColumn")
)
fun <T, ID> RowOrCol.childrenLazyLoading(
    scroll: ScrollingBehaviors,
    items: Reactive<List<T>>,
    id: (T) -> ID,
    threshold: Dimension = 20.rem,
    loadMore: suspend () -> Unit,
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) {
    var loadJob: Job? = null
    var sizeAtLoadStart = -1

    renderList(items, id = id, animate = false, render = render)

    val isVertical = this.vertical
    withoutLoadingAnimations {
        reactive {
            val list = items()
            val vp = scroll.viewport()
            val ct = scroll.content()
            if(loadJob != null) return@reactive

            val contentEnd = if (isVertical) ct.bottom else ct.right
            val viewportEnd = if (isVertical) vp.bottom else vp.right
            val distanceToEnd = contentEnd - viewportEnd
            if (distanceToEnd < threshold.viewUnits) {
                sizeAtLoadStart = list.size
                loadJob = this@childrenLazyLoading.async {
                    try {
                        loadMore()
                        delay(0.1.seconds)
                    } catch (e: Throwable) {
                        sizeAtLoadStart = -1  // Allow retry on error
                        throw e
                    } finally {
                        loadJob = null
                    }
                }
            }
        }
    }
}

/**
 * Creates a scrolling column that renders children with lazy loading.
 * Convenience wrapper that creates the scrolling container.
 */
@Suppress("DEPRECATION")
@OptIn(InternalKiteUi::class)
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
 * Convenience wrapper that creates the scrolling container.
 */
@Suppress("DEPRECATION")
@OptIn(InternalKiteUi::class)
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
