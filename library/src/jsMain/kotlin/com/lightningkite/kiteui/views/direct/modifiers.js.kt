package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val openingOtherPopover = ArrayList<()->Unit>()

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ViewWrapper {
    beforeNextElementSetup {
        val theme = currentTheme
        val pos = this
        val sourceElement = this
        var existingElement: HTMLElement? = null
        var existingDismisser: HTMLElement? = null
        var stayOpen = false
        var close = {}
        val writerTargetingBody = targeting(document.body!!)
        fun makeElement() {
            if (existingElement != null) return
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
                                style.right = "${window.innerWidth - r.left}px"
                            }
                            when (preferredDirection.align) {
                                Align.Start -> style.bottom = "${window.innerHeight - r.bottom}px"
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
                                Align.Start -> style.right = "${window.innerWidth - r.right}px"
                                Align.End -> style.left = "${r.left}px"
                                else -> {
                                    style.left = "${(r.left + r.right) / 2}px"
                                    style.transform = "translateX(-50%)"
                                }
                            }
                        }
                    }
                    reposition()
                    val currentElement = this
                    this.style.setProperty("pointer-events", "none")
                    close = {
                        existingElement?.style?.opacity = "0"
//                        existingElement?.style?.transform = "scale(0,0)"
                        existingDismisser?.style?.opacity = "0"
                        window.setTimeout({
                            existingElement?.let { it.parentElement?.removeChild(it) }
                            existingElement = null
                            existingDismisser?.let { it.parentElement?.removeChild(it) }
                            existingDismisser = null
                        }, 150)
                        close = {}
                    }
                    window.setTimeout({
                        style.opacity = "1"
//                        style.transform = "none"
                    }, 16)
                    window.addEventListener("scroll", { reposition() }, true)
//                    addEventListener("mousewheel", { event: Event ->
//                        pos.dispatchEvent(event)
//                    })
                    setup()
                }
            }
        }
        this.addEventListener("contextmenu", {
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
        this.onmouseenter = {
            makeElement()
        }
        this.onmouseleave = {
            close()
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper {
    beforeNextElementSetup {
        val theme = currentTheme
        val pos = this
        val sourceElement = this
        var existingElement: HTMLElement? = null
        var existingDismisser: HTMLElement? = null
        val maxDist = 32
        var stayOpen = false
        var close = {}
        val writerTargetingBody = targeting(document.body!!)
        fun makeElement() {
            if (existingElement != null) return
            openingOtherPopover.forEach { it() }
            openingOtherPopover.clear()
            openingOtherPopover.add { close() }
            with(writerTargetingBody) {
                currentTheme = { rootTheme().dialog() }
                stayOpen = false
                element<HTMLDivElement>("div") {
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
                                style.right = "${window.innerWidth - r.left}px"
                            }
                            when (preferredDirection.align) {
                                Align.Start -> style.bottom = "${window.innerHeight - r.bottom}px"
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
                                Align.Start -> style.right = "${window.innerWidth - r.right}px"
                                Align.End -> style.left = "${r.left}px"
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
                        window.removeEventListener("mousemove", mouseMove)
                        existingElement?.style?.opacity = "0"
//                        existingElement?.style?.transform = "scale(0,0)"
                        existingDismisser?.style?.opacity = "0"
                        window.setTimeout({
                            existingElement?.let { it.parentElement?.removeChild(it) }
                            existingElement = null
                            existingDismisser?.let { it.parentElement?.removeChild(it) }
                            existingDismisser = null
                        }, 150)
                        close = {}
                    }
                    window.setTimeout({
                        style.opacity = "1"
//                        style.transform = "none"
                    }, 16)
                    window.addEventListener("mousemove", mouseMove)
                    window.addEventListener("scroll", { reposition() }, true)
//                    addEventListener("mousewheel", { event: Event ->
//                        pos.dispatchEvent(event)
//                    })
                    setup(object : PopoverContext {
                        override val calculationContext: CalculationContext
                            get() = pos.calculationContext
                        override fun close() {
                            close()
                        }
                    })
                }
            }
        }
        if(this.tagName != "button") {
            this.addEventListener("click", {
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
        }
        if (!requiresClick) {
            this.onmouseenter = {
                makeElement()
            }
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWrapper = hasPopover {
    text {
        content = message
    } in card
}

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    beforeNextElementSetup {
        style.flexGrow = "$amount"
        style.flexShrink = "$amount"
        style.flexBasis = "0"
    }
    return ViewWrapper
}
@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: suspend () -> Float): ViewWrapper {
    beforeNextElementSetup {
        calculationContext.reactiveScope {
            val amount = amount()
            if(amount != 0f) {
                style.flexGrow = "$amount"
                style.flexShrink = "$amount"
                style.flexBasis = "0"
            } else {
                style.flexGrow = "0"
                style.flexShrink = "0"
                style.flexBasis = "auto"
            }
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        classList.add("h${horizontal}", "v${vertical}")
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.scrolls: ViewWrapper
    get() {
        beforeNextElementSetup {
            classList.add("scroll-vertical")
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.scrollsHorizontally: ViewWrapper
    get() {
        beforeNextElementSetup {
            classList.add("scroll-horizontal")
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    beforeNextElementSetup {

        if (constraints.minHeight == null) style.removeProperty("minHeight")
        else style.minHeight = constraints.minHeight.value

        if (constraints.maxHeight == null) style.removeProperty("maxHeight")
        else style.maxHeight = constraints.maxHeight.value

        if (constraints.minWidth == null) style.removeProperty("minWidth")
        else style.minWidth = constraints.minWidth.value

        if (constraints.maxWidth == null) style.removeProperty("maxWidth")
        else style.maxWidth = constraints.maxWidth.value

        if (constraints.aspectRatio == null) style.removeProperty("aspect-ratio")
        else style.setProperty("aspect-ratio", "${constraints.aspectRatio.first} / ${constraints.aspectRatio.second}")

        if (constraints.width == null) style.removeProperty("width")
        else style.width = constraints.width.value

        if (constraints.height == null) style.removeProperty("height")
        else style.height = constraints.height.value
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.padded: ViewWrapper
    get() {
        beforeNextElementSetup {
            classList.add("padded")
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.unpadded: ViewWrapper
    get() {
        beforeNextElementSetup {
            classList.add("unpadded")
        }
        return ViewWrapper
    }


// End

@ViewModifierDsl3
actual fun ViewWriter.onlyWhen(default: Boolean, condition: suspend () -> Boolean): ViewWrapper {
    wrapNext(document.createElement("div") as HTMLDivElement) {
        classList.add("hidingContainer")
        hidden = !default
        var last = !default
        calculationContext.reactiveScope {
            val child = firstElementChild as? HTMLElement
            val value = !condition()
            if (value == last) return@reactiveScope
            last = value
            if (animationsEnabled && child != null) {
                classList.add("animatingShowHide")

                val myStyle = window.getComputedStyle(child)
                val transitionTime = myStyle.transitionDuration.let { Duration.parseOrNull(it) } ?: 150.milliseconds
                val totalTime = transitionTime.inWholeMilliseconds.toDouble()
                var oldAnimTime = totalTime
                (this.asDynamic().__kiteui__hiddenAnim as? Animation)?.let {
                    oldAnimTime = it.currentTime
                    it.cancel()
                }
                (this.asDynamic().__kiteui__hiddenAnim2 as? Animation)?.let {
                    it.cancel()
                }
                this.asDynamic().__kiteui__goalHidden = value
                hidden = false
                val parent = generateSequence(this as HTMLElement) { it.parentElement as? HTMLElement }.drop(1)
                    .firstOrNull { !it.classList.contains("toggle-button") } ?: return@reactiveScope
                val parentStyle = window.getComputedStyle(parent)
                val x =
                    parentStyle.display == "flex" && parentStyle.flexDirection.contains("row")// && myStyle.width.none { it.isDigit() }
                val y =
                    parentStyle.display == "flex" && parentStyle.flexDirection.contains("column")// && myStyle.height.none { it.isDigit() }

                val before = js("{}")
                val after = js("{}")
                val full = if (value) before else after
                val fullTransform = ArrayList<String>()
                val gone = if (value) after else before
                val goneTransform = ArrayList<String>()

                var fullWidth = ""
                var fullHeight = ""
                var gap = ""
                if (hidden) {
                    hidden = false
                    fullWidth = myStyle.width
                    fullHeight = myStyle.height
                    gap = parentStyle.columnGap
                    hidden = true
                } else {
                    fullWidth = myStyle.width
                    fullHeight = myStyle.height
                    gap = parentStyle.columnGap
                }
                child.style.width = myStyle.width
                child.style.maxWidth = "unset"
                child.style.height = myStyle.height
                child.style.maxHeight = "unset"

                if (x) {
                    goneTransform.add("scaleX(0)")
                    fullTransform.add("scaleX(1)")
                    gone.marginLeft = "calc($gap / -2.0)"
                    gone.paddingLeft = "0px"
                    gone.marginRight = "calc($gap / -2.0)"
                    gone.paddingRight = "0px"
                    gone.width = "0px"
                    gone.minWidth = "0px"
                    gone.maxWidth = "0px"
                    full.width = fullWidth
                    full.minWidth = fullWidth
                    full.maxWidth = fullWidth
                }
                if (y) {
                    goneTransform.add("scaleY(0)")
                    fullTransform.add("scaleY(1)")
                    gone.marginTop = "calc($gap / -2.0)"
                    gone.paddingTop = "0px"
                    gone.marginBottom = "calc($gap / -2.0)"
                    gone.paddingBottom = "0px"
                    gone.height = "0px"
                    gone.minHeight = "0px"
                    gone.maxHeight = "0px"
                    full.height = fullHeight
                    full.minHeight = fullHeight
                    full.maxHeight = fullHeight
                }
                if (!x && !y) {
                    full.opacity = "1"
                    gone.opacity = "0"
                }
                goneTransform.takeUnless { it.isEmpty() }?.let {
//                        gone.transform = it.joinToString(" ")
//                        gone.transformOrigin = "top left"
                }
                fullTransform.takeUnless { it.isEmpty() }?.let {
//                        full.transform = it.joinToString(" ")
//                        full.transformOrigin = "top left"
                }
                this.animate(
                    arrayOf(before, after),
                    js(
                        "duration" to totalTime,
                        "easing" to "ease-out"
                    )
                ).let {
                    it.currentTime = (totalTime - oldAnimTime).coerceAtLeast(0.0)
                    it.onfinish = { ev ->
                        if (this.asDynamic().__kiteui__hiddenAnim == it) {
                            hidden = value
                            classList.remove("animatingShowHide")
                            this.asDynamic().__kiteui__hiddenAnim = null
                            child.style.removeProperty("width")
                            child.style.removeProperty("maxWidth")
                            child.style.removeProperty("height")
                            child.style.removeProperty("maxHeight")
                        }
                    }
                    it.oncancel = { ev ->
                        if (this.asDynamic().__kiteui__hiddenAnim == it) {
                            hidden = value
                            classList.remove("animatingShowHide")
                            this.asDynamic().__kiteui__hiddenAnim = null
                            child.style.removeProperty("width")
                            child.style.removeProperty("maxWidth")
                            child.style.removeProperty("height")
                            child.style.removeProperty("maxHeight")
                        }
                    }
                    it.onremove = { ev ->
                        if (this.asDynamic().__kiteui__hiddenAnim == it) {
                            hidden = value
                            classList.remove("animatingShowHide")
                            this.asDynamic().__kiteui__hiddenAnim = null
                            child.style.removeProperty("width")
                            child.style.removeProperty("maxWidth")
                            child.style.removeProperty("height")
                            child.style.removeProperty("maxHeight")
                        }
                    }
                    this.asDynamic().__kiteui__hiddenAnim = it
                }
            } else {
                hidden = value
            }
        }
    }
    return ViewWrapper
}
