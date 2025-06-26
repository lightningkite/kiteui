package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*


/**
 * A frame that has transparency and blurs the views behind it.
 * If a background is applied to it, the background should go on top so that you can use a background with alpha to tint the glass effect.
 */
expect class GlassFrame(context: RContext) : RView {
    var blurStrength: Double
}