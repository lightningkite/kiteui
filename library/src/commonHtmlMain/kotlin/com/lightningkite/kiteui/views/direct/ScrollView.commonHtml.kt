package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

expect class ScrollingBehaviorImpl constructor(
    on: Element,
    horizontal: Boolean,
    vertical: Boolean
): ScrollingBehaviors {
     override val horizontal: Boolean

     override val vertical: Boolean

     override var showScrollBars: Boolean


     override val viewport: Reactive<Rect>

     override val content: Reactive<Rect>

     override val directlyInteractingWithScroller: Reactive<Boolean>

     override var snapToElements: Pair<Align?, Align?>


     override var scrollSnapStop: Boolean

     override var ignoreInteraction: Boolean

     override fun scrollTo(left: Double, top: Double, animated: Boolean)

     override fun scrollTo(element: Element, horizontal: Align, vertical: Align, animated: Boolean)

     override fun scrollToKeepAnimations(x: Double, y: Double)

     override fun disableScrollAnchoring()
 }