package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.math.max
import kotlin.math.min

/**
 * A lightweight lazy loading scrolling list that only renders items within the viewport.
 *
 * Unlike [Recycler2], this component uses a standard scrolling column layout and simply
 * shows/hides items based on scroll position. This makes it lighter weight and compatible
 * with SSR (server-side rendering).
 *
 * How it works:
 * - Creates placeholder slots for all items upfront (just empty frames with fixed height)
 * - Only "activates" slots within the viewport by rendering actual content into them
 * - Deactivates slots when they leave the viewport
 *
 * For SSR: The server renders all placeholder slots, which can be hydrated on the client.
 * Initial page load shows empty slots that fill in as the user scrolls or immediately for
 * items in the initial viewport.
 *
 * @param items The reactive list of items to display
 * @param itemHeight Fixed height for each item (required for calculating scroll positions)
 * @param overdraw Number of items to render above/below the viewport for smooth scrolling
 * @param render Function to render each item. Receives a Reactive<T> for the item data.
 */
fun <T> ViewWriter.lazyList(
    items: Reactive<List<T>>,
    itemHeight: Dimension,
    overdraw: Int = 3,
    render: ViewWriter.(Reactive<T>) -> Unit
): LazyListHandle {
    lateinit var scroll: ScrollingBehaviors
    val handle = LazyListHandle()

    // Track the visible range - use signals so each item can subscribe
    val visibleFirst = Signal(0)
    val visibleLast = Signal(overdraw + 10)

    expanding.scrolling { scroll = this }.col {
        val itemHeightPx = itemHeight.px
        val gapPx = theme.gap.px
        val itemWithGap = itemHeightPx + gapPx

        // Track creation index during forEach render
        var creationIndex = 0

        // Use forEach which creates all items but with visibility control
        forEach(items) { item ->
            val myIndex = creationIndex++
            val itemSignal = Constant(item)

            // Fixed-height placeholder that always exists
            sizeConstraints(height = itemHeight).frame {
                // Use frame with exists binding for visibility
                frame {
                    ::exists {
                        val first = visibleFirst()
                        val last = visibleLast()
                        myIndex in first..last
                    }
                    render(itemSignal)
                }
            }
        }

        // Update visible range based on scroll position
        reactive {
            val viewport = scroll.viewport()
            val itemList = items()
            val totalCount = itemList.size

            val viewportTop = viewport.top
            val viewportHeight = viewport.height
            val viewportBottom = viewportTop + viewportHeight

            val first = max(0, ((viewportTop / itemWithGap).toInt() - overdraw))
            val last = min(totalCount - 1, ((viewportBottom / itemWithGap).toInt() + overdraw))

            visibleFirst.value = first
            visibleLast.value = last

            // Update handle
            handle._firstVisibleIndex.value = if (totalCount > 0) max(0, (viewportTop / itemWithGap).toInt()) else 0
            handle._lastVisibleIndex.value = if (totalCount > 0) min(totalCount - 1, (viewportBottom / itemWithGap).toInt()) else 0
        }

        handle._scrollBehaviors = scroll
        handle._itemHeight = itemHeightPx
        handle._gap = gapPx
    }

    return handle
}

/**
 * A handle returned by [lazyList] that provides information about the visible range
 * and allows programmatic scrolling.
 */
class LazyListHandle {
    internal val _firstVisibleIndex = Signal(0)
    internal val _lastVisibleIndex = Signal(0)
    internal var _scrollBehaviors: ScrollingBehaviors? = null
    internal var _itemHeight: Double = 50.0
    internal var _gap: Double = 0.0

    /** The index of the first fully visible item */
    val firstVisibleIndex: Reactive<Int> get() = _firstVisibleIndex

    /** The index of the last fully visible item */
    val lastVisibleIndex: Reactive<Int> get() = _lastVisibleIndex

    /** Scroll to show a specific item index */
    fun scrollToIndex(index: Int, animated: Boolean = true) {
        _scrollBehaviors?.let { scroll ->
            val targetY = index * (_itemHeight + _gap)
            scroll.scrollTo(0.0, targetY, animated)
        }
    }
}

/**
 * Simpler lazy list that just renders everything with forEachUpdating.
 * Useful when the item count is reasonable and you don't need virtualization.
 *
 * @param items The reactive list of items to display
 * @param render Function to render each item
 */
fun <T> ViewWriter.lazyListSimple(
    items: Reactive<List<T>>,
    render: ViewWriter.(Reactive<T>) -> Unit
): Unit {
    expanding.scrolling.col {
        forEachUpdating(items, render = render)
    }
}
