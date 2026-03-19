// by Claude
package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.Element

/**
 * HTML implementation of sizeConstraints.
 * Sets CSS properties directly on the element.
 * by Claude
 */
actual fun Element.setSizeConstraints(
    width: Dimension?,
    height: Dimension?,
    minWidth: Dimension?,
    maxWidth: Dimension?,
    minHeight: Dimension?,
    maxHeight: Dimension?
) {
    width?.let { this.native.setStyleProperty("width", "${it.px}px") }
    height?.let { this.native.setStyleProperty("height", "${it.px}px") }
    minWidth?.let { this.native.setStyleProperty("min-width", "${it.px}px") }
    maxWidth?.let { this.native.setStyleProperty("max-width", "${it.px}px") }
    minHeight?.let { this.native.setStyleProperty("min-height", "${it.px}px") }
    maxHeight?.let { this.native.setStyleProperty("max-height", "${it.px}px") }

    // Prevent flex shrinking for spacers
    this.native.setStyleProperty("flex-shrink", "0")
}

/**
 * HTML implementation of setTranslation.
 * Uses CSS transform which doesn't trigger layout or scroll events.
 * by Claude
 */
actual fun Element.setTranslation(x: Double, y: Double) {
    this.native.setStyleProperty("transform", "translate(${x}px, ${y}px)")
}

/**
 * Disables CSS transition animations on the transform property.
 * Used by WindowedList to prevent the browser from animating transform changes
 * that are meant to be instant position corrections.
 * by Claude
 */
actual fun Element.disableTransformTransition() {
    this.native.setStyleProperty("transition-property", "none")
}
