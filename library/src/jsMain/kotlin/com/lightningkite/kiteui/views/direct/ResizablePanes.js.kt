package com.lightningkite.kiteui.views.direct

import kotlinx.browser.document
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.dom.MouseEvent
import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.RView
import org.w3c.dom.events.EventListener

/**
 * JS-specific extension for ResizablePanes to handle document-level events.
 * This adds a setupDocumentDragHandling function that will be used in the setupDragHandling function.
 */
fun ResizablePanes.setupDocumentDragHandling(
    divider: FutureElement,
    vertical: Boolean,
    startX: Double,
    startY: Double,
    beforeSize: Double,
    afterSize: Double,
    minSize: Double,
    onResize: (newBeforeSize: Double, newAfterSize: Double) -> Unit
) {
    // Mouse move handler
    val mouseMoveListener = EventListener { event ->
        val moveEvent = event as MouseEvent
        moveEvent.preventDefault()

        // Calculate delta
        val deltaX = moveEvent.pageX - startX
        val deltaY = moveEvent.pageY - startY
        val delta = if (vertical) deltaY else deltaX

        // Calculate new sizes with minimum constraints
        val newBeforeSize = (beforeSize + delta).coerceAtLeast(minSize)
        val newAfterSize = (afterSize - delta).coerceAtLeast(minSize)

        // Apply new sizes through the callback
        onResize(newBeforeSize, newAfterSize)
    }

    // Mouse up handler
    val mouseUpListener = EventListener { _ ->
        // Remove document-level event listeners
        document.removeEventListener("mousemove", mouseMoveListener)
        document.removeEventListener("mouseup", mouseUpListener)
    }

    // Add document-level event listeners
    document.addEventListener("mousemove", mouseMoveListener)
    document.addEventListener("mouseup", mouseUpListener)
}

/**
 * Override the setupDragHandling function in ResizablePanes to use document-level events.
 * This is done by adding a mousedown event listener that sets up document-level event handling.
 */
fun ResizablePanes.setupJsDragHandling(divider: FutureElement, beforePane: RView, afterPane: RView) {
    // Replace any existing mousedown listeners
    divider.replaceEventListener("mousedown") { event ->
        event as MouseEvent
        event.preventDefault()

        // Get initial positions
        val startX = event.pageX
        val startY = event.pageY

        // Get initial sizes
        val beforeRect = beforePane.screenRectangle() ?: return@replaceEventListener
        val afterRect = afterPane.screenRectangle() ?: return@replaceEventListener

        val beforeSize = if (vertical) beforeRect.height else beforeRect.width
        val afterSize = if (vertical) afterRect.height else afterRect.width

        // Set up document-level drag handling
        setupDocumentDragHandling(
            divider = divider,
            vertical = vertical,
            startX = startX,
            startY = startY,
            beforeSize = beforeSize,
            afterSize = afterSize,
            minSize = minPaneSize.value.roughPx,
            onResize = { newBeforeSize, newAfterSize ->
                // Apply new sizes
                if (vertical) {
                    beforePane.native.style.height = "${newBeforeSize}px"
                    afterPane.native.style.height = "${newAfterSize}px"
                } else {
                    beforePane.native.style.width = "${newBeforeSize}px"
                    afterPane.native.style.width = "${newAfterSize}px"
                }
            }
        )
    }
}
