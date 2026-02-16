// by Claude
package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
