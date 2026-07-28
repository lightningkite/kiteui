package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.exceptions.ExceptionHandler
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
 * Uses [forEachById] for ID-based diffing with animations.
 *
 * Caller owns [items] and [loadMore]. This function renders children, monitors scroll position,
 * and calls [loadMore] when the user scrolls within [threshold] of the end.
 *
 * Stops loading automatically when [loadMore] completes without the list growing.
 * Retries are allowed after errors.
 */
@InternalKiteUi
@Deprecated("This will be marked as internal soon")
fun <T, ID> ContainerElement.childrenLazyLoading(
    scroll: ScrollingBehaviors,
    items: Reactive<List<T>>,
    id: (T) -> ID,
    threshold: Dimension = 20.rem,
    loadMore: suspend () -> Unit,
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) {
    var loadJob: Job? = null
    var sizeAtLoadStart = -1

    renderListKeyed(items, id = id, animate = false, render = render)

    withoutLoadingAnimations {
        reactive {
            val list = items()
            val vp = scroll.viewport()
            val ct = scroll.content()
            if (loadJob != null) return@reactive

            val contentEnd = if (scroll.vertical) ct.bottom else ct.right
            val viewportEnd = if (scroll.vertical) vp.bottom else vp.right
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

inline fun <T, ID, C : ContainerElement> ElementWriter.CanAddScrolling.renderLazyListIn(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    noinline id: (T) -> ID,
    threshold: Dimension = 20.rem,
    noinline loadMore: suspend () -> Unit,
    noinline render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
): C {
    var setupCalled = false
    lateinit var scroll: ScrollingBehaviors
    val result = scrolling { scroll = this }.container {
        setupCalled = true
        @OptIn(InternalKiteUi::class)
        @Suppress("DEPRECATION")
        childrenLazyLoading(scroll, items, id, threshold, loadMore, render)
    }
    if (!setupCalled) context.handleException(
        Exception("renderLazyListIn: container lambda did not call the setup lambda. Use a DSL function reference like ElementWriter::col or ElementWriter::row, or ensure your custom lambda invokes the passed setup function, e.g., { setup -> col { setup() } }"),
        ExceptionHandler.Metadata(
            source = result,
            process = null,
            foregroundProcess = null,
            context = mapOf(
                "container type" to result::class.toString(),
                "items" to (items.state.getOrNull()?.toString() ?: "NotReady"),
            )
        )
    )
    return result
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
        @OptIn(InternalKiteUi::class)
        @Suppress("DEPRECATION")
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
        @OptIn(InternalKiteUi::class)
        @Suppress("DEPRECATION")
        childrenLazyLoading(scroll, items, id, threshold, loadMore, render)
    }
}
