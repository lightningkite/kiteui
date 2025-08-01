package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

public interface ScrollingBehaviors {
    public val horizontal: Boolean
    public val vertical: Boolean
    public var showScrollBars: Boolean
    public val viewport: Readable<Rect>
    public val content: Readable<Rect>
    public val directlyInteractingWithScroller: Readable<Boolean>
    public var snapToElements: Pair<Align?, Align?>
    public var scrollSnapStop: Boolean
    public fun scrollTo(left: Double, top: Double, animated: Boolean)
    public fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean)

    /**
     * Should not interrupt animations.
     */
    public fun scrollToKeepAnimations(x: Double, y: Double)
}
