package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.dom.DOMRect
import com.lightningkite.kiteui.models.Align

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.PopoverSemantic
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import com.lightningkite.kiteui.dom.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public actual class FloatingInfoHolder actual constructor(public val source: Element) {
    public val maxDist: Int = 32
    public var blockView: Element? = null
    public var closeView: Element? = null
    public var existingView: Element? = null

    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    public var currentDirection: PopoverPreferredDirection = preferredDirection
    public actual var menuGenerator: Frame.() -> Unit = { space() }

    public fun closeButton() {
        if (closeView != null) return
        val o = source.context.overlayFrame ?: return
        val v = existingView ?: return
        o.atTopEnd.button {
            closeView = this
            icon(Icon.close, "Close")
            onClick {
                close()
            }
        }
    }

    public actual fun block() {
        if (blockView != null) return
        val o = source.context.overlayFrame ?: return
        val v = existingView ?: return
        @OptIn(OverrideOnly::class)
        o.addChild(
            o.children.indexOf(v),
            object : NativeElement(o.context) {
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
                    native.setAttribute("role", "presentation")
                    native.setAttribute("aria-hidden", "true")
                    blockView = this
                }
            }
        )
    }

    public actual fun open() {
        if (existingView != null) return
        var removeElementFromOverlay = {}
        // Held so it can be disconnected on teardown; a live ResizeObserver retains its target
        // element and the reposition closure, leaking the popover's DOM subtree otherwise.
        var resizeObserver: ResizeObserver? = null
        val popoverWriter = source.popoverWriter(source.context.overlayFrame!!) {
            removeElementFromOverlay()
        }
        val startedFocused = source.native.element == document.activeElement
        with(popoverWriter) {
            frame {
                source.context.keepPopoverOpen(this)
                if(source.context.addons == this.context.addons) throw Exception("NO NO NO")
                currentDirection = preferredDirection
                existingView = this
                themeChoice = PopoverSemantic
                native.style.position = "absolute"
                native.style.zIndex = "999"
                native.style.height = "auto"
                native.style.width = "unset"
                native.style.height = "unset"
                native.classes.add("popover")
                val menuId = "kiteui-menu-${Random.nextInt().toUInt()}"
                native.id = menuId
                source.native.setAttribute("aria-controls", menuId)
                native.setAttribute("role", "dialog")
                native.setAttribute("aria-modal", "true")
                // Update aria-expanded on the source element
                source.native.setAttribute("aria-expanded", "true")
                var tx = 0.0
                var txm = 0
                var ty = 0.0
                var tym = 0

                fun reposition() {
                    native.onElement { e ->
                        e as HTMLElement
                        val sourcePosition = source.native.element!!.getBoundingClientRect()
                        val screen = document.body!!.getBoundingClientRect()
                        val size = e.getBoundingClientRect()
                        e.style.removeProperty("top")
                        e.style.removeProperty("left")
                        e.style.removeProperty("right")
                        e.style.removeProperty("bottom")
                        e.style.removeProperty("transform")
                        val gap = 0

                        fun PopoverPreferredDirection.bounds(): DOMRect {
                            return if(this.horizontal) {
                                val x = if(this.after)
                                    sourcePosition.right + gap
                                else
                                    sourcePosition.left - gap - size.width
                                when(this.align) {
                                    Align.Start -> DOMRect(x, sourcePosition.bottom - size.height, size.width, size.height)
                                    Align.End -> DOMRect(x, sourcePosition.top, size.width, size.height)
                                    Align.Center -> DOMRect(x, sourcePosition.centerY - size.height / 2, size.width, size.height)
                                    Align.Stretch -> DOMRect(x, 0.0, size.width, screen.height)
                                }
                            } else {
                                val y = if(this.after)
                                    sourcePosition.bottom + gap
                                else
                                    sourcePosition.top - gap - size.height
                                when(this.align) {
                                    Align.Start -> DOMRect(sourcePosition.right - size.width, y, size.width, size.height)
                                    Align.End -> DOMRect(sourcePosition.left, y, size.width, size.height)
                                    Align.Center -> DOMRect(sourcePosition.centerX - size.width / 2, y, size.width, size.height)
                                    Align.Stretch -> DOMRect(0.0, y, screen.width, size.height)
                                }
                            }
                        }
                        val currentDirection = buildList {
                            add(preferredDirection)
                            for(otherAlign in Align.entries)
                                add(preferredDirection.copy(align = otherAlign))
                            for(otherAlign in listOf(preferredDirection.align)+Align.entries)
                                add(preferredDirection.copy(after = !preferredDirection.after, align = otherAlign))
                            val altAligns = if(preferredDirection.after) listOf(Align.End, Align.Center, Align.Start, Align.Stretch) else listOf(Align.Start, Align.Center, Align.End, Align.Stretch)
                            for(otherAlign in altAligns)
                                add(preferredDirection.copy(horizontal = !preferredDirection.horizontal, after = !preferredDirection.after, align = otherAlign))
                        }
                            .firstOrNull {
                                val proposed = it.bounds()
                                val epsilon =
                                    0.01 // Fix precision mismatch i.e. screen.right is 1231 but proposed is 1231.0000457763672
                                (proposed.left >= screen.left - epsilon) && (proposed.right <= screen.right + epsilon) &&
                                        (proposed.top >= screen.top - epsilon) && (proposed.bottom <= screen.bottom + epsilon)
                            }

                        if(currentDirection == null) {
                            closeButton()
                            e.style.left = "0px"
                            e.style.right = "0px"
                            e.style.bottom = "0px"
                            e.style.top = "0px"
                            tx = 0.0
                            ty = 0.0
                            txm = 0
                            tym = 0
                        } else if (currentDirection.horizontal) {
                            if (currentDirection.after) {
                                tx = sourcePosition.right
                                txm = 0
                            } else {
                                tx = sourcePosition.left
                                txm = -100
                            }
                            when (currentDirection.align) {
                                Align.Start -> {
                                    ty = sourcePosition.bottom
                                    tym = -100
                                }

                                Align.End -> {
                                    ty = sourcePosition.top
                                    tym = 0
                                }

                                Align.Center -> {
                                    ty = (sourcePosition.top + sourcePosition.bottom) / 2
                                    tym = -50
                                }

                                Align.Stretch -> {
                                    ty = 0.0
                                    tym = 0
                                    e.style.top = "0px"
                                    e.style.bottom = "0px"
                                }
                            }
                        } else {
                            if (currentDirection.after) {
                                ty = sourcePosition.bottom
                                tym = 0
                            } else {
                                ty = sourcePosition.top
                                tym = -100
                            }
                            when (currentDirection.align) {
                                Align.Start -> {
                                    tx = sourcePosition.right
                                    txm = -100
                                }

                                Align.End -> {
                                    tx = sourcePosition.left
                                    txm = 0
                                }

                                Align.Center -> {
                                    tx = (sourcePosition.left + sourcePosition.right) / 2
                                    txm = -50
                                }

                                Align.Stretch -> {
                                    tx = 0.0
                                    txm = 0
                                    e.style.left = "0px"
                                    e.style.right = "0px"
                                }
                            }
                        }
                        e.style.transform = "translate(${tx}px, ${ty}px) translate($txm%, $tym%)"
                    }
                }

                // Corrective measures: force it back on-screen
                native.onElement { e ->
                    e as HTMLElement
                    resizeObserver = ResizeObserver { entry, observer ->
                        reposition()
                    }.also { it.observe(e) }
                }

                menuGenerator(this)
                native.create()

                // Focus first focusable child, or container as fallback
                if(startedFocused) {
                    native.onElement { e ->
                        e as HTMLElement
                        afterTimeout(16) {
                            val focusable = e.querySelector(
                                "button, [href], input, select, textarea, [tabindex]:not([tabindex=\"-1\"])"
                            ) as? HTMLElement
                            if (focusable != null) {
                                focusable.focus()
                            } else {
                                if (e.getAttribute("tabindex") == null) e.setAttribute("tabindex", "-1")
                                e.focus()
                            }
                        }
                    }
                }

                reposition()
                val repos = { ev: Event -> reposition() }
                window.addEventListener("scroll", repos, true)

                val escapeHandler = { ev: Event ->
                    if ((ev as? KeyboardEvent)?.key == "Escape") {
                        close()
                    }
                    Unit
                }
                document.addEventListener("keydown", escapeHandler)

                val mouseMove = { it: Event ->
                    it as MouseEvent
                    if (blockView == null && context.popoverKeepOpen <= 0) {
                        val clientRect = (source.native.element as HTMLElement).getBoundingClientRect()
                        val popUpRect = (native.element as HTMLElement).getBoundingClientRect()
                        val popUpDist = maxOf(
                            it.x - popUpRect.right,
                            popUpRect.left - it.x,
                            it.y - popUpRect.bottom,
                            popUpRect.top - it.y,
                        )
                        val clientDist = maxOf(
                            it.x - clientRect.right,
                            clientRect.left - it.x,
                            it.y - clientRect.bottom,
                            clientRect.top - it.y,
                        )
                        if (min(popUpDist, clientDist) > maxDist) close()
                    }
                }
                window.addEventListener("mousemove", mouseMove)

                removeElementFromOverlay = {
                    blockView?.let {
                        source.overlayFrame!!.removeChild(it)
                    }
                    blockView = null
                    closeView?.let {
                        source.overlayFrame!!.removeChild(it)
                    }
                    closeView = null
                    window.removeEventListener("scroll", repos, true)
                    window.removeEventListener("mousemove", mouseMove)
                    document.removeEventListener("keydown", escapeHandler)
                    resizeObserver?.disconnect()
                    resizeObserver = null
                    source.native.setAttribute("aria-expanded", "false")
                    source.native.setAttribute("aria-controls", null)
                    if (startedFocused) {
                        // Restore focus to the trigger element
                        (source.native.element as? HTMLElement)?.focus()
                    }
                    native.onElement { e ->
                        this.onShutdown()
                        (e as HTMLElement)
                        window.getComputedStyle(e).getPropertyValue("transition-duration")
                            .let { Duration.parseOrNull(it) ?: 0.25.seconds }
                            .let {
                                window.setTimeout({
                                    source.context.overlayFrame!!.removeChild(this)
                                }, it.inWholeMilliseconds.toInt())
                            }
                        e.style.opacity = "0"
                        e.style.setProperty("pointer-events", "none")
                    }
                    existingView = null
                }
            }
        }
    }

    public actual fun close() {
        source.context.closeSiblingPopovers()
    }
}

public val DOMRect.centerY: Double get() =  (top + bottom) / 2
public val DOMRect.centerX: Double get() =  (left + right) / 2