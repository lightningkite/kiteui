package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import kotlin.UnsupportedOperationException

actual class ScrollingBehaviorImpl actual constructor(
    val on: RView,
    actual override val horizontal: Boolean,
    actual override val vertical: Boolean
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
    actual override var showScrollBars: Boolean = true
        set(value) {
            field = value
            if(value) native.classes -= "hideScrollbar"
            else native.classes += "hideScrollbar"
        }

    actual override val viewport: Readable<Rect> = Readable.Never
    actual override val content: Readable<Rect> = Readable.Never
    actual override val directlyInteractingWithScroller: Readable<Boolean> get() = Constant(false)
    actual override var snapToElements: Pair<Align?, Align?> = null to null
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
    actual override var scrollSnapStop: Boolean = false
        set(value) {
            field = value
            native.setStyleProperty("scroll-snap-stop", if(value) "always" else "normal")
        }
    actual override var ignoreInteraction: Boolean
        get() = throw UnsupportedOperationException("Ignoring ScrollView interaction is not supported for web targets")
        set(value) {}

    actual override fun scrollTo(left: Double, top: Double, animated: Boolean) {
    }
    actual override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
    }

    actual override fun scrollToKeepAnimations(x: Double, y: Double) {
        TODO("Not yet implemented")
    }
}