package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.reactive.invokeAllSafe
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.popoverClosers
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

actual class FloatingInfoHolder actual constructor(val source: RView) {
//    val theme get() = source.theme
//    val pos = source
//    var existingElement: HTMLElement? = null
//    var existingDismisser: HTMLElement? = null
//    val maxDist = 32
//    var stayOpen = false
//    var close = {}
//
    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    actual var menuGenerator: ViewWriter.() -> Unit = { space() }
//
//    val writer = object: ViewWriter() {
//        override val context: RContext = source.context
//        override fun addChild(view: RView) { document.body?.append(view) }
//    }
//    val innerWriter = object: ViewWriter() {
//        override val context: RContext = source.context.split()
//        override fun addChild(view: RView) { document.body?.append(view) }
//    }

    actual fun open() {
//        if (existingElement != null) return
//        writer.popoverClosers.forEach { it() }
//        writer.popoverClosers.clear()
//        writer.popoverClosers.add({ close() })
//        with(writer) {
//            currentTheme = { rootTheme().dialog() }
//            stayOpen = false
//            transitionNextView = ViewWriter.TransitionNextView.Yes
//            themedElement<HTMLDivElement>("div") {
//                existingElement = this
//                style.position = "absolute"
//                style.zIndex = "999"
////                    style.transform = "scale(0,0)"
//                style.opacity = "0"
//                style.height = "auto"
//                calculationContext.reactiveScope {
//                    style.setProperty("--parentSpacing", theme().spacing.value)
//                }
//                fun reposition() {
//                    val r = pos.getBoundingClientRect()
//                    style.removeProperty("top")
//                    style.removeProperty("left")
//                    style.removeProperty("right")
//                    style.removeProperty("bottom")
//                    style.removeProperty("transform")
//                    if (preferredDirection.horizontal) {
//                        if (preferredDirection.after) {
//                            style.left = "${r.right}px"
//                        } else {
//                            style.right = "${kotlinx.browser.window.innerWidth - r.left}px"
//                        }
//                        when (preferredDirection.align) {
//                            com.lightningkite.kiteui.models.Align.Start -> style.bottom = "${kotlinx.browser.window.innerHeight - r.bottom}px"
//                            com.lightningkite.kiteui.models.Align.End -> style.top = "${r.top}px"
//                            else -> {
//                                style.top = "${(r.top + r.bottom) / 2}px"
//                                style.transform = "translateY(-50%)"
//                            }
//                        }
//                    } else {
//                        if (preferredDirection.after) {
//                            style.top = "${r.bottom}px"
//                        } else {
//                            style.bottom = "${kotlinx.browser.window.innerHeight - r.top}px"
//                        }
//                        when (preferredDirection.align) {
//                            com.lightningkite.kiteui.models.Align.Start -> style.right = "${kotlinx.browser.window.innerWidth - r.right}px"
//                            com.lightningkite.kiteui.models.Align.End -> style.left = "${r.left}px"
//                            else -> {
//                                style.left = "${(r.left + r.right) / 2}px"
//                                style.transform = "translateX(-50%)"
//                            }
//                        }
//                    }
//                }
//                reposition()
//                this.onmouseenter = {
//                    makeElement()
//                }
//                val currentElement = this
//                val mouseMove = { it: Event ->
//                    it as MouseEvent
//                    if (!stayOpen) {
//                        val clientRect = sourceElement.getBoundingClientRect()
//                        val popUpRect = currentElement.getBoundingClientRect()
//                        if (min(
//                                maxOf(
//                                    it.x - popUpRect.right,
//                                    popUpRect.left - it.x,
//                                    it.y - popUpRect.bottom,
//                                    popUpRect.top - it.y,
//                                ), maxOf(
//                                    it.x - clientRect.right,
//                                    clientRect.left - it.x,
//                                    it.y - clientRect.bottom,
//                                    clientRect.top - it.y,
//                                )
//                            ) > maxDist
//                        ) close()
//                    }
//                }
//                close = {
//                    kotlinx.browser.window.removeEventListener("mousemove", mouseMove)
//                    existingElement?.style?.opacity = "0"
////                        existingElement?.style?.transform = "scale(0,0)"
//                    existingDismisser?.style?.opacity = "0"
//                    kotlinx.browser.window.setTimeout({
//                        existingElement?.let { it.parentElement?.removeChild(it) }
//                        existingElement = null
//                        existingDismisser?.let { it.parentElement?.removeChild(it) }
//                        existingDismisser = null
//                    }, 150)
//                    close = {}
//                }
//                kotlinx.browser.window.setTimeout({
//                    style.opacity = "1"
////                        style.transform = "none"
//                }, 16)
//                kotlinx.browser.window.addEventListener("mousemove", mouseMove)
//                kotlinx.browser.window.addEventListener("scroll", { reposition() }, true)
////                    addEventListener("mousewheel", { event: Event ->
////                        pos.dispatchEvent(event)
////                    })
//                action()
//            }
//        }
    }

    actual fun close() {
//        writer.popoverClosers.invokeAllSafe()
//        writer.popoverClosers.clear()
    }
}