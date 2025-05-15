package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.FrequencyCapAction
import com.lightningkite.readable.Readable
import com.lightningkite.kiteui.views.RView
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface ScrollingBehaviors {
    val horizontal: Boolean
    val vertical: Boolean
    var showScrollBars: Boolean
    val viewport: Readable<Rect>
    val content: Readable<Rect>
    val directlyInteractingWithScroller: Readable<Boolean>
    var snapToElements: Pair<Align?, Align?>
    var scrollSnapStop: Boolean
    fun scrollTo(left: Double, top: Double, animated: Boolean)
    fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean)
    fun onPullToRefresh(frequencyCap: Duration = 5.seconds, action: (suspend () -> Unit)) {
        val wrappedAction = Action("Refresh", Icon.download, action = action)
        pullToRefreshAction = FrequencyCapAction(wrappedAction, frequencyCap)
    }
    var pullToRefreshAction: FrequencyCapAction?
    var showRefreshIndicator: Boolean

    /**
     * Should not interrupt animations.
     */
    fun scrollToKeepAnimations(x: Double, y: Double)
}
