package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.reactive.invokeAllSafe
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayStack
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.min

actual class FloatingInfoHolder actual constructor(val source: RView) {
    val theme get() = source.theme
    var existingElement: Element? = null
    var existingDismisser: Element? = null
    val maxDist = 32
    var stayOpen = false

    init {
        println("New FloatingInfoHolder at layer ${source.popoverClosersId}")
    }

    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    actual var menuGenerator: ViewWriter.() -> Unit = { space() }

    actual fun open() {
        if (existingElement != null) return
        var close = {}
        object: ViewWriter() {
            override val context: RContext = source.context.split()
            override fun addChild(view: RView) = source.overlayStack!!.addChild(view)
        }.popoverLayer(
            closer = {
                close()
            },
            createPopover = {
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
                        native.onElement {
                            it as HTMLElement
                            with(it) {
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
                            }
                        }
                    }
                    reposition()

                    println("Closer id here is ${popoverClosersId}")
                    menuGenerator(this)

                    close = {
                        native.onElement { it.parentNode?.removeChild(it); this.shutdown() }
                        existingElement = null
                    }
                }
            }
        )
    }

    actual fun close() {
        source.closePopovers()
    }
}