package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*

expect class ScrollingBehaviorImpl constructor(
    on: RView,
    horizontal: Boolean,
    vertical: Boolean
): ScrollingBehaviors {
     override val horizontal: Boolean

     override val vertical: Boolean

     override var showScrollBars: Boolean


     override val viewport: Readable<Rect>

     override val content: Readable<Rect>

     override val directlyInteractingWithScroller: Readable<Boolean>

     override var snapToElements: Pair<Align?, Align?>


     override var scrollSnapStop: Boolean

     override fun onPullToRefresh(action: (suspend () -> Unit)?)

     override var showRefreshIndicator: Boolean

     override fun scrollTo(left: Double, top: Double, animated: Boolean)

     override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean)

     override fun scrollToKeepAnimations(x: Double, y: Double)
 }