package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.readable.*
import com.lightningkite.kiteui.viewDebugTarget
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

    private val ro by lazy { native.resizeObserver() }
    private val clientSize by lazy {
        on.reactive {
            rerunOn(ro)
            Size(
                native.element?.clientWidth?.toDouble() ?: throw ReactiveLoading,
                native.element?.clientHeight?.toDouble() ?: throw ReactiveLoading,
            )
        }
    }
    private val scrollSize by lazy {
        on.reactive {
            rerunOn(ro)
            Size(
                native.element?.scrollWidth?.toDouble() ?: throw ReactiveLoading,
                native.element?.scrollHeight?.toDouble() ?: throw ReactiveLoading,
            )
        }
    }
    private val scrollEvent = native.vevent("scroll")
    private val lockScrollEnd = BasicListenable()
    private var lockScrollReportAt: Rect? = null
    actual override val viewport: Readable<Rect> by lazy {
        on.reactive {
            rerunOn(scrollEvent)
            rerunOn(lockScrollEnd)
            lockScrollReportAt ?: Rect.fromSize(
                left = native.element?.scrollLeft ?: 0.0,
                top = native.element?.scrollTop ?: 0.0,
                width = clientSize().width,
                height = clientSize().height,
            )
        }
    }
    actual override val content: Readable<Rect> by lazy {
        on.reactive {
            Rect.fromSize(
                left = 0.0,
                top = 0.0,
                width = scrollSize().width,
                height = scrollSize().height,
            )
        }
    }
    val _directlyInteractingWithScroller = Property(false)
    actual override val directlyInteractingWithScroller: Readable<Boolean> get() = _directlyInteractingWithScroller
    init {
        var lastTimeout: ()->Unit = {}
        native.addEventListener("scroll") {
            _directlyInteractingWithScroller.value = true
            lastTimeout.invoke()
            lastTimeout = afterTimeout(100) {
                // can restore snap
                snapToElements = snapToElements
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
            if(viewDebugTarget == on) println("ScrollView.snapToElements set")
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
            if(viewDebugTarget == on) println("ScrollView.scrollSnapStop set")
            field = value
            native.setStyleProperty("scroll-snap-stop", if(value) "always" else "normal")
        }
    actual override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        if(viewDebugTarget == on) println("ScrollView.scrollTo($left, $top, $animated)")
        disableSnapTemporarily()
        native.onElement {
            (it as HTMLElement).scrollTo(ScrollToOptions(
                left = left,
                top = top,
                behavior = if(animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
            ))
        }
    }
    actual override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
        if(viewDebugTarget == on) println("ScrollView.scrollTo($element, $horizontal, $vertical, $animated)")
        disableSnapTemporarily()
        element.native.element?.scrollIntoView(ScrollToOptions(
            behavior = if(animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
        ))
    }
    var sdn = 0
    fun disableSnapTemporarily() {
        native.classes.removeAll { it.startsWith("snapTo-") }
        native.setStyleProperty("scroll-snap-type", "unset")
    }
    actual override fun scrollToKeepAnimations(x: Double, y: Double) {
        if(viewDebugTarget == on) println("ScrollView.scrollToKeepAnimations($x, $y)")
        disableSnapTemporarily()
        native.onElement {
            (it as HTMLElement)
            it.addClass("suppress-overflow-anchors")
            lockScrollReportAt = Rect.fromSize(
                left = x,
                top = y,
                width = native.element?.clientWidth?.toDouble() ?: 0.0,
                height = native.element?.clientHeight?.toDouble() ?: 0.0,
            )
            it.scrollLeft = x
            it.scrollTop = y
//                it.scrollTo(ScrollToOptions(it.scrollLeft, it.scrollTop, ScrollBehavior.INSTANT))

            run {
                // ugly dirty painful safari fix
                var count = 0
                var printer = {}
                printer = label@{
                    val c = count++
                    // fuck you, set the position
                    it.scrollLeft = x
                    it.scrollTop = y
//                it.scrollTo(ScrollToOptions(it.scrollLeft, it.scrollTop, ScrollBehavior.INSTANT))
                    if (count < 15) window.setTimeout(printer, 1)
                }
                printer()
            }
            window.requestAnimationFrame {
                println("Animation frame occurred")
            }
            run {
                var count = 0
                var printer = {}
                printer = label@{
                    val c = count++
                    println("offset count $count: ${it.scrollLeft}, ${it.scrollTop}")
                    if(count < 15) window.setTimeout(printer, 1)
                }
                printer()
            }
            afterTimeout(16) {
                lockScrollReportAt = null
                if(snapToElements.first != null || snapToElements.second != null) {
                    snapToElements = snapToElements  // reset css
                    it.scrollLeft = x
                    it.scrollTop = y
                }
                lockScrollEnd.invokeAll()
            }
        }
    }
}
