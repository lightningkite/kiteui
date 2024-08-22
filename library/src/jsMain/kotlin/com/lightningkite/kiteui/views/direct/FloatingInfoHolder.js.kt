package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.reactive.BasicListenable
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayStack
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class FloatingInfoHolder actual constructor(val source: RView) {
    val theme get() = source.theme
    val maxDist = 32
    var blockView: RView? = null
    var existingView: RView? = null

    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    actual var menuGenerator: Stack.() -> Unit = { space() }

    actual fun block() {
        if (blockView != null) return
        val o = source.overlayStack ?: return
        val v = existingView ?: return
        o.addChild(
            o.children.indexOf(v),
            object : RView(o.context) {
                init {
                    native.tag = "div"
                    native.addEventListener("click") {
                        close()
                    }
                    native.style.position = "absolute"
                    native.style.left = "0"
                    native.style.right = "0"
                    native.style.bottom = "0"
                    native.style.top = "0"
                    native.style.opacity = "0"
                    native.style.zIndex = "998"
                    native.classes.add("active-${Random.nextInt()}")
                    blockView = this
                }
            }
        )
    }

    actual fun open() {
        if (existingView != null) return
        val writer = object : ViewWriter() {
            override val context: RContext = source.context.split()
            override fun addChild(view: RView) = source.overlayStack!!.addChild(view)
        }
        source.closeSiblingPopovers()
        val childCloser = BasicListenable()
        var closeCurrent = {}
        var stopListeningToCloser = {}
        fun close() {
            stopListeningToCloser()
            closeCurrent()
            blockView?.let {
                source.overlayStack!!.removeChild(it)
            }
            blockView = null
        }
        stopListeningToCloser = source.popoverClosers.addListener {
            childCloser.invokeAll()
            close()
        }
        writer.popoverClosers = childCloser
        with(writer) {
            stack {
                existingView = this
                themeChoice = DialogSemantic
                native.style.position = "absolute"
                native.style.zIndex = "999"
                native.style.height = "auto"
                native.style.width = "unset"
                native.style.height = "unset"
                var tx = 0.0
                var txm = 0
                var ty = 0.0
                var tym = 0

                // Corrective measures: force it back on-screen
                native.onElement { e ->
                    e as HTMLElement
                    ResizeObserver { entry, observer ->
                        val screen = document.body!!.getBoundingClientRect()
                        val rect = e.getBoundingClientRect()
                        var altered = false
                        if (rect.right > screen.right) {
                            tx -= rect.right - screen.right
                            altered = true
                        }
                        if (rect.left < screen.left) {
                            tx -= rect.left - screen.left
                            altered = true
                        }
                        if (rect.bottom > screen.bottom) {
                            ty -= rect.bottom - screen.bottom
                            altered = true
                        }
                        if (rect.top < screen.top) {
                            ty -= rect.top - screen.top
                            altered = true
                        }
                        if (altered)
                            e.style.transform = "translate(${tx}px, ${ty}px) translate($txm%, $tym%)"
                    }.observe(e)
                }

                fun reposition() {
                    native.onElement { e ->
                        e as HTMLElement
                        with(e) {
                            val r = source.native.element!!.getBoundingClientRect()
                            style.removeProperty("top")
                            style.removeProperty("left")
                            style.removeProperty("right")
                            style.removeProperty("bottom")
                            style.removeProperty("transform")
                            if (preferredDirection.horizontal) {
                                if (preferredDirection.after) {
                                    tx = r.right
                                    txm = 0
                                } else {
                                    tx = r.left
                                    txm = -100
                                }
                                when (preferredDirection.align) {
                                    Align.Start -> {
                                        ty = r.bottom
                                        tym = -100
                                    }

                                    Align.End -> {
                                        ty = r.top
                                        tym = 0
                                    }

                                    else -> {
                                        ty = (r.top + r.bottom) / 2
                                        tym = -50
                                    }
                                }
                            } else {
                                if (preferredDirection.after) {
                                    ty = r.bottom
                                    tym = 0
                                } else {
                                    ty = r.top
                                    tym = -100
                                }
                                when (preferredDirection.align) {
                                    Align.Start -> {
                                        tx = r.right
                                        txm = -100
                                    }

                                    Align.End -> {
                                        tx = r.left
                                        txm = 0
                                    }

                                    else -> {
                                        tx = (r.left + r.right) / 2
                                        txm = -50
                                    }
                                }
                            }
                            style.transform = "translate(${tx}px, ${ty}px) translate($txm%, $tym%)"
                        }
                    }
                }

                menuGenerator(this)
                native.create()

                reposition()
                window.addEventListener("scroll", { reposition() }, true)

                val mouseMove = { it: Event ->
                    it as MouseEvent
                    if (blockView == null) {
                        val clientRect = (source.native.element as HTMLElement).getBoundingClientRect()
                        val popUpRect = (native.element as HTMLElement).getBoundingClientRect()
                        if (min(
                                maxOf(
                                    it.x - popUpRect.right,
                                    popUpRect.left - it.x,
                                    it.y - popUpRect.bottom,
                                    popUpRect.top - it.y,
                                ), maxOf(
                                    it.x - clientRect.right,
                                    clientRect.left - it.x,
                                    it.y - clientRect.bottom,
                                    clientRect.top - it.y,
                                )
                            ) > maxDist
                        ) close()
                    }
                }
                window.addEventListener("mousemove", mouseMove)

                closeCurrent = {
                    window.removeEventListener("mousemove", mouseMove)
                    native.onElement { e ->
                        this.shutdown()
                        (e as HTMLElement)
                        window.getComputedStyle(e).getPropertyValue("transition-duration")
                            .let { Duration.parseOrNull(it) ?: 0.25.seconds }
                            .let {
                                window.setTimeout({
                                    source.overlayStack!!.removeChild(this)
                                }, it.inWholeMilliseconds.toInt())
                            }
                        e.style.opacity = "0"
                    }
                    existingView = null
                }
            }
        }
    }

    actual fun close() {
        source.closePopovers()
    }
}