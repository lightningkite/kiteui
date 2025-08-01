package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.browser.window
import kotlinx.dom.addClass
import org.w3c.dom.*


actual class ScrollingBehaviorImpl actual constructor(
    val on: RView,
    actual override val horizontal: Boolean,
    actual override val vertical: Boolean
) : ScrollingBehaviors {
    val native = on.native

    init {
        native.style.lineHeight = "0px !important"
        if (horizontal) {
            native.classes += "scroll-horizontal"
            native.style.overflowX = "auto"
        } else {
            native.style.overflowX = "none"
        }
        if (vertical) {
            native.classes += "scroll-vertical"
            native.style.overflowY = "auto"
        } else {
            native.style.overflowY = "none"
        }
    }

    actual override var showScrollBars: Boolean = true
        set(value) {
            field = value
            if (value) native.classes -= "hideScrollbar"
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
    actual override val viewport: Reactive<Rect> by lazy {
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
    actual override val content: Reactive<Rect> by lazy {
        on.reactive {
            Rect.fromSize(
                left = 0.0,
                top = 0.0,
                width = scrollSize().width,
                height = scrollSize().height,
            )
        }
    }
    val _directlyInteractingWithScroller = Signal(false)
    actual override val directlyInteractingWithScroller: Reactive<Boolean> get() = _directlyInteractingWithScroller

    init {
//        var lastTimeout: () -> Unit = {}
//        native.addEventListener("scroll") {
//            _directlyInteractingWithScroller.value = true
//            lastTimeout.invoke()
//            lastTimeout = afterTimeout(100) {
//                // can restore snap
//                snapToElements = snapToElements
//                _directlyInteractingWithScroller.value = false
//            }
//        }
//        native.addEventListener("scrollend") {
//            _directlyInteractingWithScroller.value = false
//        }
//        var touches = 0
//        native.addEventListener("touchstart") {
//            touches++
//        }
//        native.addEventListener("touchend") {
//            if (--touches == 0) _directlyInteractingWithScroller.value = false
//        }
//        native.addEventListener("touchcancel") {
//            if (--touches == 0) _directlyInteractingWithScroller.value = false
//        }
    }

    actual override var snapToElements: Pair<Align?, Align?> = null to null
        set(value) {
            on.debugPrint { "ScrollView.snapToElements set" }
            field = value
            native.classes.removeAll { it.startsWith("snapTo-") }
            native.classes.add("snapTo-${value.first}-${value.second}")
            native.setStyleProperty(
                "scroll-snap-type", when {
                    value.first != null && value.second != null -> "both mandatory"
                    value.first != null -> "x mandatory"
                    value.second != null -> "y mandatory"
                    else -> "none"
                }
            )
        }
    actual override var scrollSnapStop: Boolean = false
        set(value) {
            on.debugPrint { "ScrollView.scrollSnapStop set" }
            field = value
            native.setStyleProperty("scroll-snap-stop", if (value) "always" else "normal")
        }

    actual override var ignoreInteraction: Boolean
        get() = throw UnsupportedOperationException("Ignoring ScrollView interaction is not supported for web targets")
        set(value) {}

    actual override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        on.debugPrint { ("ScrollView.scrollTo($left, $top, $animated)") }
        disableSnapTemporarily()
        native.onElement {
            (it as HTMLElement).scrollTo(
                ScrollToOptions(
                    left = left,
                    top = top,
                    behavior = if (animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
                )
            )
        }
    }

    actual override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
        on.debugPrint { ("ScrollView.scrollTo($element, $horizontal, $vertical, $animated)") }
        disableSnapTemporarily()
        element.native.element?.scrollIntoView(
            ScrollToOptions(
                behavior = if (animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
            )
        )
    }

    private var scrollToInstance = 0
    fun disableSnapTemporarily() {
        native.classes.removeAll { it.startsWith("snapTo-") }
        native.setStyleProperty("scroll-snap-type", "unset")
    }

    actual override fun scrollToKeepAnimations(x: Double, y: Double) {
        val myInstance = ++scrollToInstance
        on.debugPrint { ("ScrollView.scrollToKeepAnimations($x, $y)") }
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
            val amount = 32
            for (count in 1..(amount - 1)) {
                window.setTimeout(label@{
                    if (myInstance != scrollToInstance) return@label
                    it.scrollLeft = x
                    it.scrollTop = y
                }, count)
            }
            afterTimeout(amount.toLong()) label@{
                if (myInstance != scrollToInstance) return@label
                if (snapToElements.first != null || snapToElements.second != null) {
                    snapToElements = snapToElements  // reset css
                    it.scrollLeft = x
                    it.scrollTop = y
                }
                lockScrollReportAt = null
                lockScrollEnd.invokeAll()
            }
        }
    }
}
