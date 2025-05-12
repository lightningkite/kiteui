package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.DOMRect
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.DOMRectInit
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
    var closeView: RView? = null
    var existingView: RView? = null

    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter
    var currentDirection: PopoverPreferredDirection = preferredDirection
    actual var menuGenerator: Frame.() -> Unit = { space() }

    fun closeButton() {
        if (closeView != null) return
        val o = source.overlayFrame ?: return
        val v = existingView ?: return
        with<RView, Unit>(o) {
            beforeNextElementSetup { closeView = this } - atTopEnd - button {
                icon(Icon.close, "Close")
                onClick {
                    close()
                }
            }
        }
    }

    actual fun block() {
        if (blockView != null) return
        val o = source.overlayFrame ?: return
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
        var removeElementFromOverlay = {}
        val popoverWriter = source.popoverWriter(source.overlayFrame!!) {
            removeElementFromOverlay()
        }
        with(popoverWriter) {
            frame {
                source.keepPopoverOpen(this)
                currentDirection = preferredDirection
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
                                proposed.left >= screen.left && proposed.right <= screen.right &&
                                    proposed.top >= screen.top && proposed.bottom <= screen.bottom
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
                    ResizeObserver { entry, observer ->
                        reposition()
                    }.observe(e)
                }

                menuGenerator(this)
                native.create()

                reposition()
                val repos = { ev: Event -> reposition() }
                window.addEventListener("scroll", repos, true)

                val mouseMove = { it: Event ->
                    it as MouseEvent
                    if (blockView == null && popoverKeepOpen <= 0) {
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
                    native.onElement { e ->
                        this.shutdown()
                        (e as HTMLElement)
                        window.getComputedStyle(e).getPropertyValue("transition-duration")
                            .let { Duration.parseOrNull(it) ?: 0.25.seconds }
                            .let {
                                window.setTimeout({
                                    source.overlayFrame!!.removeChild(this)
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

    actual fun close() {
        source.closeSiblingPopovers()
    }
}

val DOMRect.centerY get() =  (top + bottom) / 2
val DOMRect.centerX get() =  (left + right) / 2