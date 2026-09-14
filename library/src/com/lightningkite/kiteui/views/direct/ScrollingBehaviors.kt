package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.views.Element
import com.lightningkite.reactive.core.*

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
    public fun scrollTo(element: Element, horizontal: Align, vertical: Align, animated: Boolean)
    public var ignoreInteraction: Boolean

    /**
     * Should not interrupt animations.
     */
    public fun scrollToKeepAnimations(x: Double, y: Double)

    /**
     * Disables browser scroll anchoring on this scroll container.
     * On web, the browser auto-adjusts scrollTop when content is added above the viewport
     * (CSS overflow-anchor). This conflicts with manual scroll compensation (e.g., transform-based
     * anchoring in virtualized lists). Call this to opt out of browser scroll anchoring.
     * No-op on non-web platforms.
     * by Claude
     */
    public fun disableScrollAnchoring() {}
}
