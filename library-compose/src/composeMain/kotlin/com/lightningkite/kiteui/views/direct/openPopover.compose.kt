package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.PopoverSemantic
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

public actual fun Element.openPopover(preferredDirection: PopoverPreferredDirection, anchor: Element?, createMenu: Frame.() -> Unit) {
    val popover = Popover(source = this, anchor = anchor ?: this)
    popover.open(preferredDirection, createMenu)
    popover.block()
}

/**
 * A popover beside [anchor], in the overlay frame: the desktop counterpart of the web's FloatingInfoHolder.
 * Once [block]ed it closes on a click outside it.  Until then it closes when the pointer has left both
 * the popover and the anchor (as reported through [anchorHovered]), unless something inside it is
 * keeping it open.
 */
internal class Popover(private val source: Element, private val anchor: Element) {
    private var layer: PopoverLayer? = null
    private var popoverHovered = false
    var anchorHovered = false
        set(value) {
            field = value
            closeIfAbandoned()
        }

    fun open(direction: PopoverPreferredDirection, createMenu: Frame.() -> Unit) {
        if (layer != null) return
        val overlay = source.context.overlayFrame
        source.popoverWriter(overlay) {
            layer?.let { overlay.removeChild(it) }
            layer = null
        }.run {
            layer = write(PopoverLayer(context, anchor.underlyingNativeElement, direction, close = ::close)) {
                source.context.keepPopoverOpen(this)
                themed(PopoverSemantic).frame {
                    underlyingNativeElement.onHoverChange = {
                        popoverHovered = it
                        closeIfAbandoned()
                    }
                    createMenu()
                }
            }
        }
    }

    fun block() {
        layer?.blocking = true
    }

    fun close() {
        source.context.closeSiblingPopovers()
    }

    private fun closeIfAbandoned() {
        val layer = layer ?: return
        // Moving from the anchor onto the popover reports leaving one an event or so before entering the other.
        // The web allows for this with 32px of slack around both; this allows a moment instead.
        layer.launch {
            delay(100.milliseconds)
            if (!layer.blocking && !anchorHovered && !popoverHovered && layer.context.popoverKeepOpen <= 0) close()
        }
    }
}

/** Fills the overlay frame and places its one child, the popover, beside [anchor]. */
internal class PopoverLayer(
    context: ElementContext,
    private val anchor: NativeElement,
    private val direction: PopoverPreferredDirection,
    private val close: () -> Unit,
) : NativeContainerElement(context) {
    var blocking by mutableStateOf(false)
    private var popoverBounds = IntRect.Zero

    // The layer covers the whole overlay: when not blocking, the pointer must reach what's under it
    // (including the anchor, whose hover keeps a hover popover open).  When blocking it handles taps itself.
    init {
        ignoreInteraction = true
    }

    // TODO: Escape to close, focusing the popover's first focusable child, and following the anchor when it scrolls.
    @Composable
    override fun Content(modifier: Modifier) {
        val density = LocalDensity.current.density
        val m = if (blocking) modifier.pointerInput(Unit) {
            detectTapGestures { if (!popoverBounds.contains(it.round())) close() }
        } else modifier
        Layout(content = { ShownChildren() }, modifier = m) { measurables, constraints ->
            val popover = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(constraints.maxWidth, constraints.maxHeight) {
                // Null only while Compose works out alignment lines, which this layout has none of.
                val origin = (coordinates ?: return@layout).positionInWindow()
                val anchorRect = checkNotNull(anchor.screenRectangle()) { "Popover anchor $anchor is not on screen" }
                val (x, y) = direction.calculatePopoverOffset(
                    anchor = Rect(
                        left = anchorRect.left * density - origin.x,
                        top = anchorRect.top * density - origin.y,
                        right = anchorRect.right * density - origin.x,
                        bottom = anchorRect.bottom * density - origin.y,
                    ),
                    self = Rect(0.0, 0.0, popover.width.toDouble(), popover.height.toDouble()),
                    safeArea = Rect(0.0, 0.0, constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble()),
                )
                val left = x.roundToInt()
                val top = y.roundToInt()
                popoverBounds = IntRect(left, top, left + popover.width, top + popover.height)
                popover.place(left, top)
            }
        }
    }
}


