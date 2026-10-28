package com.lightningkite.kiteui.views

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.views.direct.ScrollingBehaviors
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Scrolling lives on the element itself (as on the web), applied by [NativeElement.Render] between its
 * background and its padding.  Positions are in dp, which on this backend is the same unit as KiteUI's px.
 */
internal class ScrollingBehaviorImpl(
    private val element: NativeElement,
    override val horizontal: Boolean,
    override val vertical: Boolean,
) : ScrollingBehaviors {
    private val horizontalState = ScrollState(0)
    private val verticalState = ScrollState(0)
    private var viewportSize by mutableStateOf(IntSize.Zero)

    // TODO: scrollbars; desktop needs them since there's no touch scrolling.
    override var showScrollBars: Boolean = true
    override var snapToElements: Pair<Align?, Align?> = null to null
    override var scrollSnapStop: Boolean = false
    override var ignoreInteraction: Boolean by mutableStateOf(false)
    override var userScrollEnabled: Boolean by mutableStateOf(true)

    private val _viewport = Signal(Rect(0.0, 0.0, 0.0, 0.0))
    private val _content = Signal(Rect(0.0, 0.0, 0.0, 0.0))
    private val _interacting = Signal(false)
    override val viewport: Reactive<Rect> get() = _viewport
    override val content: Reactive<Rect> get() = _content
    override val directlyInteractingWithScroller: Reactive<Boolean> get() = _interacting

    init {
        element.launch {
            snapshotFlow {
                val density = element.density
                val x = horizontalState.value / density
                val y = verticalState.value / density
                val w = viewportSize.width / density
                val h = viewportSize.height / density
                Triple(
                    Rect(x.toDouble(), y.toDouble(), (x + w).toDouble(), (y + h).toDouble()),
                    Rect(0.0, 0.0, (w + horizontalState.maxValue / density).toDouble(), (h + verticalState.maxValue / density).toDouble()),
                    horizontalState.isScrollInProgress || verticalState.isScrollInProgress,
                )
            }.collect { (viewport, content, interacting) ->
                _viewport.value = viewport
                _content.value = content
                _interacting.value = interacting
            }
        }
        element.launch {
            // Like a desktop browser, settles on an element once a scroll ends, rather than predicting a fling.
            snapshotFlow { horizontalState.isScrollInProgress || verticalState.isScrollInProgress }.collect { scrolling ->
                if (!scrolling) {
                    snapStopRange = null
                    settle()
                }
            }
        }
    }

    // Scroll positions, in px, that line each of the content's children up with the viewport.  Scrolling lives
    // on the content itself, so its children are the snap targets, as on the web.
    private fun snapStops(align: Align?, state: ScrollState, vertical: Boolean): List<Float> {
        align ?: return emptyList()
        val self = element.screenRectangle() ?: return emptyList()
        val density = element.density
        val viewSize = if (vertical) viewportSize.height else viewportSize.width
        return (element as? ContainerElement)?.children.orEmpty()
            .filter { it.shown }
            .mapNotNull { it.underlyingNativeElement.screenRectangle() }
            .map { target ->
                val start = ((if (vertical) target.top - self.top else target.left - self.left) * density).toFloat() + state.value
                val end = ((if (vertical) target.bottom - self.top else target.right - self.left) * density).toFloat() + state.value
                when (align) {
                    Align.Start -> start
                    Align.End -> end - viewSize
                    Align.Center, Align.Stretch -> (start + end - viewSize) / 2
                }
            }
    }

    private fun settle() {
        fun target(align: Align?, state: ScrollState, vertical: Boolean): Int? {
            val now = state.value.toFloat()
            val nearest = snapStops(align, state, vertical).minByOrNull { abs(it - now) }?.roundToInt() ?: return null
            // Settling is a scroll too, so it must not start when there's nowhere to go, or it would settle forever.
            return nearest.takeIf { abs(it - state.value) > 1 }
        }
        val x = target(snapToElements.first, horizontalState, vertical = false)
        val y = target(snapToElements.second, verticalState, vertical = true)
        if (x == null && y == null) return
        val context = animationContext ?: return
        element.launch(context) {
            if (x != null) launch { horizontalState.animateScrollTo(x) }
            if (y != null) launch { verticalState.animateScrollTo(y) }
        }
    }

    // With scrollSnapStop, a scroll can't pass the stops on either side of where it started, as with CSS's
    // scroll-snap-stop.  It's held during the scroll: a recycler may have reused the elements it passed by the end.
    private var snapStopRange: Pair<Offset, Offset>? = null

    private val snapStopLimiter = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!scrollSnapStop) return Offset.Zero
            val (low, high) = snapStopRange ?: run {
                fun neighbours(align: Align?, state: ScrollState, vertical: Boolean): Pair<Float, Float> {
                    val now = state.value
                    val stops = snapStops(align, state, vertical)
                    return (stops.filter { it < now - 1 }.maxOrNull() ?: Float.NEGATIVE_INFINITY) to
                        (stops.filter { it > now + 1 }.minOrNull() ?: Float.POSITIVE_INFINITY)
                }
                val (left, right) = neighbours(snapToElements.first, horizontalState, vertical = false)
                val (top, bottom) = neighbours(snapToElements.second, verticalState, vertical = true)
                (Offset(left, top) to Offset(right, bottom)).also { snapStopRange = it }
            }
            // A positive delta scrolls back towards the start.
            fun consumed(delta: Float, state: ScrollState, low: Float, high: Float): Float {
                val allowed = state.value - (state.value - delta).coerceIn(low, high)
                return delta - allowed
            }
            return Offset(
                consumed(available.x, horizontalState, low.x, high.x),
                consumed(available.y, verticalState, low.y, high.y),
            )
        }
    }

    // A jump waits for the scroller's next measure, so it lands against the content's new size (Recycler2 jumps
    // while laying out the content).  dispatchRawDelta doesn't cancel a fling in progress, as scrollTo would.
    private var pendingJump: Offset? by mutableStateOf(null)

    fun applyTo(modifier: Modifier, elementTakesInput: Boolean): Modifier {
        val enabled = elementTakesInput && !ignoreInteraction && userScrollEnabled
        var m = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                pendingJump?.let { jump ->
                    pendingJump = null
                    val moved = Offset(
                        if (horizontal) horizontalState.dispatchRawDelta(jump.x - horizontalState.value) else 0f,
                        if (vertical) verticalState.dispatchRawDelta(jump.y - verticalState.value) else 0f,
                    )
                    snapStopRange = snapStopRange?.let { (low, high) -> low + moved to high + moved }
                }
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .onSizeChanged { viewportSize = it }
            .nestedScroll(snapStopLimiter)
        if (vertical) m = m.verticalScroll(verticalState, enabled = enabled)
        if (horizontal) m = m.horizontalScroll(horizontalState, enabled = enabled)
        return m
    }

    override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        val density = element.density
        val x = (left * density).toFloat()
        val y = (top * density).toFloat()
        val context = animationContext
        if (!animated || context == null) {
            pendingJump = Offset(x, y)
            return
        }
        element.launch(context) {
            if (horizontal) launch { horizontalState.animateScrollTo(x.roundToInt()) }
            if (vertical) launch { verticalState.animateScrollTo(y.roundToInt()) }
        }
    }

    override fun scrollTo(element: Element, horizontal: Align, vertical: Align, animated: Boolean) {
        val target = element.underlyingNativeElement.screenRectangle() ?: return
        val self = this.element.screenRectangle() ?: return
        val view = _viewport.value
        // Target's position within the scrolled content, in dp.
        val left = target.left - self.left + view.left
        val top = target.top - self.top + view.top
        val width = target.right - target.left
        val height = target.bottom - target.top
        fun offset(align: Align, start: Double, size: Double, viewSize: Double) = when (align) {
            Align.Start, Align.Stretch -> start
            Align.Center -> start - (viewSize - size) / 2
            Align.End -> start - (viewSize - size)
        }
        scrollTo(
            offset(horizontal, left, width, view.right - view.left),
            offset(vertical, top, height, view.bottom - view.top),
            animated,
        )
    }

    override fun scrollToKeepAnimations(x: Double, y: Double): Unit = scrollTo(x, y, animated = false)
}
