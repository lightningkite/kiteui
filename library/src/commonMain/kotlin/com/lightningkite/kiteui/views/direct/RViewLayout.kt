// by Claude
package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*

/**
 * Extension to apply size constraints directly to an RView.
 * by Claude
 */
expect fun RView.setSizeConstraints(
    width: Dimension? = null,
    height: Dimension? = null,
    minWidth: Dimension? = null,
    maxWidth: Dimension? = null,
    minHeight: Dimension? = null,
    maxHeight: Dimension? = null
)

/**
 * Extension to set translation transform on an RView.
 * This positions the view without triggering layout/scroll events.
 * by Claude
 */
expect fun RView.setTranslation(x: Double, y: Double)

/**
 * Disables CSS transition animations on the transform property so that
 * position corrections applied via setTranslation are instant.
 * by Claude
 */
expect fun RView.disableTransformTransition()
