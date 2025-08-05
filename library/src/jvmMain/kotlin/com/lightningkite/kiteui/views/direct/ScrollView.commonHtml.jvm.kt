package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.UnsupportedOperationException

@InternalKiteUi
public actual class ScrollingBehaviorImpl public actual constructor(
    public val on: RView,
    public actual override val horizontal: Boolean,
    public actual override val vertical: Boolean
) : ScrollingBehaviors {
    public val native: FutureElement = on.native
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

    actual override val viewport: Reactive<Rect> = Reactive.Never
    actual override val content: Reactive<Rect> = Reactive.Never
    actual override val directlyInteractingWithScroller: Reactive<Boolean> get() = Constant(false)
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
    public actual override var scrollSnapStop: Boolean = false
        set(value) {
            field = value
            native.setStyleProperty("scroll-snap-stop", if(value) "always" else "normal")
        }
    actual override var ignoreInteraction: Boolean
        get() = throw UnsupportedOperationException("Ignoring ScrollView interaction is not supported for web targets")
        set(value) {}

    actual override fun scrollTo(left: Double, top: Double, animated: Boolean) {
    }
    public actual override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
    }

    public actual override fun scrollToKeepAnimations(x: Double, y: Double) {
        TODO("Not yet implemented")
    }
}