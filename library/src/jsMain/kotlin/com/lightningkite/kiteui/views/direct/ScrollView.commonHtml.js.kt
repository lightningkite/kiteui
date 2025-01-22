package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.browser.window
import kotlinx.dom.addClass
import org.w3c.dom.*


actual class ScrollingBehaviorImpl actual constructor(
    val on: RView,
    actual override val horizontal: Boolean,
    actual override val vertical: Boolean
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
    actual override var showScrollBars: Boolean = true
        set(value) {
            field = value
            if(value) native.classes -= "hideScrollbar"
            else native.classes += "hideScrollbar"
        }

    private val ro = native.resizeObserver()
    private val clientSize = on.reactive {
        rerunOn(ro)
        println("Reading clientWidth: ${native.element?.clientWidth} / clientHeight: ${native.element?.clientHeight}")
        Size(
            native.element?.clientWidth?.toDouble() ?: throw ReactiveLoading,
            native.element?.clientHeight?.toDouble() ?: throw ReactiveLoading,
        )
    }
    private val scrollSize = on.reactive {
        rerunOn(ro)
        println("Reading scrollWidth: ${native.element?.scrollWidth} / scrollHeight: ${native.element?.scrollHeight}")
        Size(
            native.element?.scrollWidth?.toDouble() ?: throw ReactiveLoading,
            native.element?.scrollHeight?.toDouble() ?: throw ReactiveLoading,
        )
    }
    private val scrollEvent = native.vevent("scroll")
    actual override val viewport: Readable<Rect> = on.reactive {
        rerunOn(scrollEvent)
        Rect.fromSize(
            left = native.element?.scrollLeft ?: 0.0,
            top = native.element?.scrollTop ?: 0.0,
            width = clientSize().width,
            height = clientSize().height,
        )
    }
    actual override val content: Readable<Rect> = on.reactive {
        rerunOn(scrollEvent)
        Rect.fromSize(
            left = 0.0,
            top = 0.0,
            width = scrollSize().width,
            height = scrollSize().height,
        )
    }
    val _directlyInteractingWithScroller = Property(false)
    actual override val directlyInteractingWithScroller: Readable<Boolean> get() = _directlyInteractingWithScroller
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
    actual override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        native.onElement {
            (it as HTMLElement).scrollTo(ScrollToOptions(
                left = left,
                top = top,
                behavior = if(animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
            ))
        }
    }
    actual override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
        element.native.element?.scrollIntoView(ScrollToOptions(
            behavior = if(animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
        ))
    }
    var sdn = 0
    actual override fun scrollToKeepAnimations(x: Double, y: Double) {
        native.classes.removeAll { it.startsWith("snapTo-") }
        native.setStyleProperty("scroll-snap-type", "unset")
        nativeScrollToKeepAnimations(x, y)
        afterTimeout(16) {
            snapToElements = snapToElements  // reset css
            native.element?.let {
                it.scrollTo(ScrollToOptions(it.scrollLeft, it.scrollTop, ScrollBehavior.INSTANT))
            }
        }
    }

    private fun nativeScrollToKeepAnimations(
        x: Double, y: Double
    ) {
        native.onElement {
            (it as HTMLElement)
            it.addClass("suppress-overflow-anchors")
            it.scrollTo(ScrollToOptions(x, y, ScrollBehavior.INSTANT))
//            var count = 0
//            var printer = {}
//            printer = label@{
//                val c = count++
//                println("expectedLeft $c: $x, actually ${it.scrollLeft}")
//                println("expectedTop $c: $y, actually ${it.scrollTop}")
//                if(count < 20) window.setTimeout(printer, 1)
//            }
//            printer()
        }
    }
}
