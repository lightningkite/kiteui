package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.views.Element
import com.lightningkite.reactive.core.*

interface ScrollingBehaviors {
    val horizontal: Boolean
    val vertical: Boolean
    var showScrollBars: Boolean
    val viewport: Reactive<Rect>
    val content: Reactive<Rect>
    val directlyInteractingWithScroller: Reactive<Boolean>
    var snapToElements: Pair<Align?, Align?>
    var scrollSnapStop: Boolean
    fun scrollTo(left: Double, top: Double, animated: Boolean)
    fun scrollTo(element: Element, horizontal: Align, vertical: Align, animated: Boolean)
    var ignoreInteraction: Boolean

    /**
     * Should not interrupt animations.
     */
    fun scrollToKeepAnimations(x: Double, y: Double)

    /**
     * Disables browser scroll anchoring on this scroll container.
     * On web, the browser auto-adjusts scrollTop when content is added above the viewport
     * (CSS overflow-anchor). This conflicts with manual scroll compensation (e.g., transform-based
     * anchoring in virtualized lists). Call this to opt out of browser scroll anchoring.
     * No-op on non-web platforms.
     * by Claude
     */
    fun disableScrollAnchoring() {}
}
