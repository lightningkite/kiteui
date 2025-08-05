package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

public interface ScrollingBehaviors {
    public val horizontal: Boolean
    public val vertical: Boolean
    public var showScrollBars: Boolean
    public val viewport: Reactive<Rect>
    public val content: Reactive<Rect>
    public val directlyInteractingWithScroller: Reactive<Boolean>
    public var snapToElements: Pair<Align?, Align?>
    public var scrollSnapStop: Boolean
    public fun scrollTo(left: Double, top: Double, animated: Boolean)
    public fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean)
    public var ignoreInteraction: Boolean

    /**
     * Should not interrupt animations.
     */
    public fun scrollToKeepAnimations(x: Double, y: Double)
}
