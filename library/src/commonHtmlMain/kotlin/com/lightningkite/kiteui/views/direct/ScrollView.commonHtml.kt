package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

class ScrollingBehaviorImpl(
    val on: RView,
    override val horizontal: Boolean,
    override val vertical: Boolean
): ScrollingBehaviors {
    val native = on.native
    init {
        native.tag = "div"
        native.style.lineHeight = "0px !important"
        native.style.overflowAnchor = "none"
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
    override var showScrollBars: Boolean = true
        set(value) {
            field = value
            if(value) native.classes -= "hideScrollbar"
            else native.classes += "hideScrollbar"
        }

    private val rs = native.resizeObserver()
    override val viewport: Readable<Rect> = (native.vevent("scroll") + rs).lens { nativeViewport() }
    override val content: Readable<Rect> = rs.lens { nativeContent() }
    val _directlyInteractingWithScroller = Property(false)
    override val directlyInteractingWithScroller: Readable<Boolean> get() = _directlyInteractingWithScroller
    init {
        var lastTimeout: ()->Unit = {}
        native.addEventListener("scroll") {
            _directlyInteractingWithScroller.value = true
            lastTimeout.invoke()
            lastTimeout = afterTimeout(100) {
                _directlyInteractingWithScroller.value = false
            }
        }
        native.addEventListener("scrollend") {
            _directlyInteractingWithScroller.value = false
        }
        var touches = 0
        native.addEventListener("touchstart") {
            touches++
        }
        native.addEventListener("touchend") {
            if(--touches == 0) _directlyInteractingWithScroller.value = false
        }
        native.addEventListener("touchcancel") {
            if(--touches == 0) _directlyInteractingWithScroller.value = false
        }
    }
    override var snapToElements: Pair<Align?, Align?> = null to null
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
    override var scrollSnapStop: Boolean = false
        set(value) {
            field = value
            native.setStyleProperty("scroll-snap-stop", if(value) "always" else "normal")
        }
    override fun scrollTo(left: Double, top: Double, animated: Boolean) = nativeScrollTo(top, left, animated)
    override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) = nativeScrollToElement(element, horizontal, vertical, animated)
    override fun offset(x: Double, y: Double) = nativeScrollOffset(x, y)
}
internal expect fun ScrollingBehaviorImpl.nativeScrollTo(top: Double, left: Double, animated: Boolean)
internal expect fun ScrollingBehaviorImpl.nativeScrollToElement(element: RView, horizontal: Align, vertical: Align, animated: Boolean)
internal expect fun ScrollingBehaviorImpl.nativeScrollOffset(x: Double, y: Double)
internal expect fun ScrollingBehaviorImpl.nativeViewport(): Rect
internal expect fun ScrollingBehaviorImpl.nativeContent(): Rect