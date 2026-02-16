// by Claude
package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.informParentOfSizeChange
import platform.CoreGraphics.CGAffineTransformMakeTranslation

/**
 * iOS implementation of sizeConstraints for RView.
 * Uses the existing sizeConstraints property on RView.
 * by Claude
 */
actual fun RView.setSizeConstraints(
    width: Dimension?,
    height: Dimension?,
    minWidth: Dimension?,
    maxWidth: Dimension?,
    minHeight: Dimension?,
    maxHeight: Dimension?
) {
    this.sizeConstraints = SizeConstraints(
        width = width,
        height = height,
        minWidth = minWidth,
        maxWidth = maxWidth,
        minHeight = minHeight,
        maxHeight = maxHeight
    )
    native.informParentOfSizeChange()
}

/**
 * iOS implementation of setTranslation for RView.
 * Uses CGAffineTransform which doesn't trigger layout events.
 * by Claude
 */
actual fun RView.setTranslation(x: Double, y: Double) {
    native.layer.setAffineTransform(CGAffineTransformMakeTranslation(x, y))
}

/** No-op on iOS - UIKit doesn't have CSS transitions. by Claude */
actual fun RView.disableTransformTransition() {}
