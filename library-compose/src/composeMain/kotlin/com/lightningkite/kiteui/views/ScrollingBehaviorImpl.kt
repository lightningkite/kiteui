package com.lightningkite.kiteui.views

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.views.direct.ScrollingBehaviors
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.launch

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
    // TODO: snapping.
    override var snapToElements: Pair<Align?, Align?> = null to null
    override var scrollSnapStop: Boolean = false
    override var ignoreInteraction: Boolean by mutableStateOf(false)

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
    }

    fun applyTo(modifier: Modifier, elementTakesInput: Boolean): Modifier {
        val enabled = elementTakesInput && !ignoreInteraction
        var m = modifier.onSizeChanged { viewportSize = it }
        if (vertical) m = m.verticalScroll(verticalState, enabled = enabled)
        if (horizontal) m = m.horizontalScroll(horizontalState, enabled = enabled)
        return m
    }

    override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        element.launch {
            val density = element.density
            val x = (left * density).toInt()
            val y = (top * density).toInt()
            if (animated) {
                if (horizontal) launch { horizontalState.animateScrollTo(x) }
                if (vertical) launch { verticalState.animateScrollTo(y) }
            } else {
                if (horizontal) horizontalState.scrollTo(x)
                if (vertical) verticalState.scrollTo(y)
            }
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
