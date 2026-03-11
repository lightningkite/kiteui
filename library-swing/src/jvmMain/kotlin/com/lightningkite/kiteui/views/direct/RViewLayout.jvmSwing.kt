package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*

/**
 * Extension to apply size constraints directly to an RView.
 * by Claude
 */
actual fun RView.setSizeConstraints(
    width: Dimension?,
    height: Dimension?,
    minWidth: Dimension?,
    maxWidth: Dimension?,
    minHeight: Dimension?,
    maxHeight: Dimension?,
): Unit = Unit

/**
 * Extension to set translation transform on an RView.
 * This positions the view without triggering layout/scroll events.
 * by Claude
 */
actual fun RView.setTranslation(x: Double, y: Double): Unit = Unit

/**
 * Disables CSS transition animations on the transform property so that
 * position corrections applied via setTranslation are instant.
 * by Claude
 */
actual fun RView.disableTransformTransition(): Unit = Unit
