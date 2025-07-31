package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*

public actual class ScrollingBehaviorImpl public actual constructor(
    val on: RView,
    public actual override val horizontal: Boolean,
    public actual override val vertical: Boolean
) : ScrollingBehaviors {
    val native = on.native
    init {
        native.tag = "div"
        native.style.lineHeight = "0px !important"
        if(horizontal) {
            native.classes += "scroll-horizontal"
            native.style.overflowX = "auto"
        } else {
            native.style.overflowX = "none"
        }
        if(vertical) {
            native.classes += "scroll-vertical"
            native.style.overflowY = "auto"
        } else {
            native.style.overflowY = "none"
        }
    }
    public actual override var showScrollBars: Boolean = true
        set(value) {
            field = value
            if(value) native.classes -= "hideScrollbar"
            else native.classes += "hideScrollbar"
        }

    public actual override val viewport: Readable<Rect> = Readable.Never
    public actual override val content: Readable<Rect> = Readable.Never
    public actual override val directlyInteractingWithScroller: Readable<Boolean> get() = Constant(false)
    public actual override var snapToElements: Pair<Align?, Align?> = null to null
        set(value) {
            field = value
            native.classes.removeAll { it.startsWith("snapTo-") }
            native.classes.add("snapTo-${value.first}-${value.second}")
            native.setStyleProperty("scroll-snap-type", when {
                value.first != null && value.second != null -> "both mandatory"
                value.first != null -> "x mandatory"
                value.second != null -> "y mandatory"
                else -> "none"
            })
        }
    public actual override var scrollSnapStop: Boolean = false
        set(value) {
            field = value
            native.setStyleProperty("scroll-snap-stop", if(value) "always" else "normal")
        }
    public actual override fun scrollTo(left: Double, top: Double, animated: Boolean) {
    }
    public actual override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
    }

    public actual override fun scrollToKeepAnimations(x: Double, y: Double) {
        TODO("Not yet implemented")
    }
}