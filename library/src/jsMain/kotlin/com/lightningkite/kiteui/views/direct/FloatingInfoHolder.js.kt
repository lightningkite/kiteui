package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.reactive.BasicListenable
import com.lightningkite.kiteui.reactive.WindowInfo
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayStack
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual class FloatingInfoHolder actual constructor(val source: RView) {
    val theme get() = source.theme
    var existingElement: Element? = null
    var existingDismisser: Element? = null
    val maxDist = 32
    var stayOpen = false

    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    actual var menuGenerator: ViewWriter.() -> Unit = { space() }

    actual fun open() {
        if (existingElement != null) return
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
        }
        stopListeningToCloser = source.popoverClosers.addListener {
            childCloser.invokeAll()
            close()
        }
        writer.popoverClosers = childCloser
        with(writer) {
            stack {
                native.onElement { existingElement = it }
                themeChoice = ThemeChoice.Derive { it.dialog() }
                useBackground = UseBackground.Yes
                native.onElement {
                    it as HTMLElement
                    it.style.position = "absolute"
                    it.style.zIndex = "999"
                    it.style.height = "auto"
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
                                    style.left = "${r.right}px"
                                } else {
                                    style.right = "${window.innerWidth - r.left}px"
                                }
                                when (preferredDirection.align) {
                                    Align.Start -> style.bottom =
                                        "${window.innerHeight - r.bottom}px"

                                    Align.End -> style.top = "${r.top}px"
                                    else -> {
                                        style.top = "${(r.top + r.bottom) / 2}px"
                                        style.transform = "translateY(-50%)"
                                    }
                                }
                            } else {
                                if (preferredDirection.after) {
                                    style.top = "${r.bottom}px"
                                } else {
                                    style.bottom = "${window.innerHeight - r.top}px"
                                }
                                when (preferredDirection.align) {
                                    Align.Start -> style.right =
                                        "${window.innerWidth - r.right}px"

                                    Align.End -> style.left = "${r.left}px"
                                    else -> {
                                        style.left = "${(r.left + r.right) / 2}px"
                                        style.transform = "translateX(-50%)"
                                    }
                                }
                            }

                            // Corrective measures: force it back on-screen
                            val screen = document.body!!.getBoundingClientRect()
                            val rect = e.getBoundingClientRect()
                            if(preferredDirection.horizontal) {
                                if(preferredDirection.after) {
                                    if (rect.right > screen.right) {
                                        style.removeProperty("left")
                                        style.right = "${window.innerWidth - r.left}px"
                                    }
                                } else {
                                    if (rect.left < screen.left) {
                                        style.removeProperty("right")
                                        style.left = "${r.right}px"
                                    }
                                }
                                when(preferredDirection.align) {
                                    Align.Start -> {
                                        if(rect.top < screen.top) {
                                            style.removeProperty("bottom")
                                            style.top = "0px"
                                        }
                                    }
                                    Align.End -> {
                                        if(rect.bottom > screen.bottom) {
                                            style.removeProperty("top")
                                            style.bottom = "0px"
                                        }
                                    }
                                    else -> {}
                                }
                            } else {
                                if(preferredDirection.after) {
                                    if (rect.bottom > screen.bottom) {
                                        style.removeProperty("top")
                                        style.bottom = "${window.innerHeight - r.top}px"
                                    }
                                } else {
                                    if (rect.top < screen.top) {
                                        style.removeProperty("bottom")
                                        style.top = "${r.bottom}px"
                                    }
                                }
                                when(preferredDirection.align) {
                                    Align.Start -> {
                                        if(rect.left < screen.left) {
                                            style.removeProperty("right")
                                            style.left = "0px"
                                        }
                                    }
                                    Align.End -> {
                                        if(rect.right > screen.right) {
                                            style.removeProperty("left")
                                            style.right = "0px"
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }

                menuGenerator(this)
                native.create()

                reposition()
                window.addEventListener("scroll", { reposition() }, true)

                val mouseMove = { it: Event ->
                    it as MouseEvent
                    if (!stayOpen) {
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
                window.removeEventListener("mousemove", mouseMove)
                window.addEventListener("mousemove", mouseMove)

                closeCurrent = {
                    native.onElement { e ->
                        this.shutdown()
                        (e as HTMLElement)
                        window.getComputedStyle(e).getPropertyValue("transition-duration")
                            .let { Duration.parseOrNull(it) ?: 0.25.seconds }
                            .let { window.setTimeout({
                                e.parentNode?.removeChild(e)
                            }, it.inWholeMilliseconds.toInt()) }
                        e.style.opacity = "0"
                    }
                    existingElement = null
                }
            }
        }
    }


    actual fun close() {
        source.closePopovers()
    }
}