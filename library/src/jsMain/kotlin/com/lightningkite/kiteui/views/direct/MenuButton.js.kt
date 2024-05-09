package com.lightningkite.kiteui.views.direct

import ViewWriter
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.min

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NMenuButton = HTMLButtonElement

@ViewDsl
actual inline fun ViewWriter.menuButtonActual(crossinline setup: MenuButton.() -> Unit): Unit =
    themedElementClickable<NMenuButton>("menuButton") {
        classList.add("kiteui-stack")
        this.asDynamic().__KiteUI_writer = this@menuButtonActual.split()
        setup(MenuButton(this))
    }


actual fun MenuButton.opensMenu(action: ViewWriter.() -> Unit) = with(native.asDynamic().__KiteUI_writer as ViewWriter) {
    val theme = currentTheme
    val pos = this@opensMenu.native
    val sourceElement = this@opensMenu.native
    var existingElement: HTMLElement? = null
    var existingDismisser: HTMLElement? = null
    val maxDist = 32
    var stayOpen = false
    var close = {}
    val writerTargetingBody = targeting(document.body!!).apply { popoverClosers = ArrayList() }
    fun makeElement() {
        if (existingElement != null) return
        this@with.popoverClosers.forEach { it() }
        this@with.popoverClosers.clear()
        this@with.popoverClosers.add({ close() })
        with(writerTargetingBody) {
            currentTheme = { rootTheme().dialog() }
            stayOpen = false
            transitionNextView = ViewWriter.TransitionNextView.Yes
            themedElement<HTMLDivElement>("div") {
                existingElement = this
                style.position = "absolute"
                style.zIndex = "999"
//                    style.transform = "scale(0,0)"
                style.opacity = "0"
                style.height = "auto"
                calculationContext.reactiveScope {
                    style.setProperty("--parentSpacing", theme().spacing.value)
                }
                fun reposition() {
                    val r = pos.getBoundingClientRect()
                    style.removeProperty("top")
                    style.removeProperty("left")
                    style.removeProperty("right")
                    style.removeProperty("bottom")
                    style.removeProperty("transform")
                    if (preferredDirection.horizontal) {
                        if (preferredDirection.after) {
                            style.left = "${r.right}px"
                        } else {
                            style.right = "${kotlinx.browser.window.innerWidth - r.left}px"
                        }
                        when (preferredDirection.align) {
                            com.lightningkite.kiteui.models.Align.Start -> style.bottom = "${kotlinx.browser.window.innerHeight - r.bottom}px"
                            com.lightningkite.kiteui.models.Align.End -> style.top = "${r.top}px"
                            else -> {
                                style.top = "${(r.top + r.bottom) / 2}px"
                                style.transform = "translateY(-50%)"
                            }
                        }
                    } else {
                        if (preferredDirection.after) {
                            style.top = "${r.bottom}px"
                        } else {
                            style.bottom = "${kotlinx.browser.window.innerHeight - r.top}px"
                        }
                        when (preferredDirection.align) {
                            com.lightningkite.kiteui.models.Align.Start -> style.right = "${kotlinx.browser.window.innerWidth - r.right}px"
                            com.lightningkite.kiteui.models.Align.End -> style.left = "${r.left}px"
                            else -> {
                                style.left = "${(r.left + r.right) / 2}px"
                                style.transform = "translateX(-50%)"
                            }
                        }
                    }
                }
                reposition()
                this.onmouseenter = {
                    makeElement()
                }
                val currentElement = this
                val mouseMove = { it: Event ->
                    it as MouseEvent
                    if (!stayOpen) {
                        val clientRect = sourceElement.getBoundingClientRect()
                        val popUpRect = currentElement.getBoundingClientRect()
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
                close = {
                    kotlinx.browser.window.removeEventListener("mousemove", mouseMove)
                    existingElement?.style?.opacity = "0"
//                        existingElement?.style?.transform = "scale(0,0)"
                    existingDismisser?.style?.opacity = "0"
                    kotlinx.browser.window.setTimeout({
                        existingElement?.let { it.parentElement?.removeChild(it) }
                        existingElement = null
                        existingDismisser?.let { it.parentElement?.removeChild(it) }
                        existingDismisser = null
                    }, 150)
                    close = {}
                }
                kotlinx.browser.window.setTimeout({
                    style.opacity = "1"
//                        style.transform = "none"
                }, 16)
                kotlinx.browser.window.addEventListener("mousemove", mouseMove)
                kotlinx.browser.window.addEventListener("scroll", { reposition() }, true)
//                    addEventListener("mousewheel", { event: Event ->
//                        pos.dispatchEvent(event)
//                    })
                action()
            }
        }
    }
    sourceElement.addEventListener("click", {
        makeElement()
        stayOpen = true
        if(existingDismisser == null) {
            val native = document.createElement("div") as HTMLDivElement
            native.style.position = "absolute"
            native.style.left = "0"
            native.style.right = "0"
            native.style.bottom = "0"
            native.style.top = "0"
            native.style.opacity = "0"
            native.style.zIndex = "998"
            native.style.setProperty("backdrop-filter", "blur(0px)")
            window.setTimeout({
                native.style.opacity = "1"
                native.style.removeProperty("backdrop-filter")
            }, 16)
            native.onclick = { close() }
            existingDismisser = native
            document.body!!.insertBefore(native, existingElement)
        }
    })
    sourceElement.onmouseenter = {
        if(!this@opensMenu.requireClick) makeElement()
    }
}

actual inline var MenuButton.enabled: Boolean
    get() = !native.disabled
    set(value) {
        native.disabled = !value
    }
actual inline var MenuButton.requireClick: Boolean
    get() = (native.asDynamic().__KiteUI_requireClick as? Boolean) ?: false
    set(value) {
        native.asDynamic().__KiteUI_requireClick = value
    }
actual var MenuButton.preferredDirection: PopoverPreferredDirection
    get() = (native.asDynamic().__KiteUI_preferredDirection as? PopoverPreferredDirection) ?: PopoverPreferredDirection.aboveCenter
    set(value) {
        native.asDynamic().__KiteUI_preferredDirection = value
    }