package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.*
import org.w3c.dom.HTMLElement
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


@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requireClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper {
    beforeNextElementSetup {
        val theme = currentTheme
        val pos = this
        val sourceElement = this
        var existingElement: HTMLElement? = null
        var existingDismisser: HTMLElement? = null
        val maxDist = 64
        var stayOpen = false
        var close = {}
        val writerTargetingBody = targeting(NView3(document.body!!))
        val newViews = newViews()
        fun makeElement() {
            if (existingElement != null) return
            with(writerTargetingBody) {
                currentTheme = rootTheme
                stayOpen = false
                element("div", ::NContainingView) {
                    existingElement = js
                    js.style.position = "absolute"
                    js.style.zIndex = "999"
//                    js.style.transform = "scale(0,0)"
                    js.style.opacity = "0"
                    js.style.height = "max-content"
                    calculationContext.reactiveScope {
                        js.style.setProperty("--parentSpacing", theme().spacing.value)
                    }
                    fun reposition() {
                        val r = pos.js.getBoundingClientRect()
                        js.style.removeProperty("top")
                        js.style.removeProperty("left")
                        js.style.removeProperty("right")
                        js.style.removeProperty("bottom")
                        js.style.removeProperty("transform")
                        if (preferredDirection.horizontal) {
                            if (preferredDirection.after) {
                                js.style.left = "${r.right}px"
                            } else {
                                js.style.right = "${window.innerWidth - r.left}px"
                            }
                            when (preferredDirection.align) {
                                Align.Start -> js.style.bottom = "${window.innerHeight - r.bottom}px"
                                Align.End -> js.style.top = "${r.top}px"
                                else -> {
                                    js.style.top = "${(r.top + r.bottom) / 2}px"
                                    js.style.transform = "translateY(-50%)"
                                }
                            }
                        } else {
                            if (preferredDirection.after) {
                                js.style.top = "${r.bottom}px"
                            } else {
                                js.style.bottom = "${window.innerHeight - r.top}px"
                            }
                            when (preferredDirection.align) {
                                Align.Start -> js.style.right = "${window.innerWidth - r.right}px"
                                Align.End -> js.style.left = "${r.left}px"
                                else -> {
                                    js.style.left = "${(r.left + r.right) / 2}px"
                                    js.style.transform = "translateX(-50%)"
                                }
                            }
                        }
                    }
                    reposition()
                    this.js.onmouseenter = {
                        makeElement()
                    }
                    val currentElement = this
                    val mouseMove = { it: Event ->
                        it as MouseEvent
                        if (!stayOpen) {
                            val clientRect = sourceElement.js.getBoundingClientRect()
                            val popUpRect = currentElement.js.getBoundingClientRect()
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
                            null
                        }, 150)
                        close = {}
                    }
                    window.setTimeout({
                        js.style.opacity = "1"
//                        style.transform = "none"
                        null
                    }, 16)
                    window.addEventListener("mousemove", mouseMove)
                    window.addEventListener("scroll", { reposition() }, true)
                    setup(object : PopoverContext {
                        override val calculationContext: CalculationContext
                            get() = pos.js.calculationContext
                        override fun close() {
                            close()
                        }
                    })
                }
            }
        }
        this.js.addEventListener("click", {
            makeElement()
            stayOpen = true
            with(newViews)  {
                currentTheme = rootTheme
                dismissBackground {
                    native.js.style.position = "absolute"
                    native.js.style.left = "0"
                    native.js.style.right = "0"
                    native.js.style.bottom = "0"
                    native.js.style.top = "0"
                    native.js.style.opacity = "0"
                    native.js.style.setProperty("backdrop-filter", "blur(0px)")
                    window.setTimeout({
                        native.js.style.opacity = "1"
                        native.js.style.removeProperty("backdrop-filter")
                        null
                    }, 16)
                    onClick { close() }
                    existingDismisser = native.js
                }
            }
            document.body!!.insertBefore(newViews.rootCreated!!.js, existingElement)
        })
        if(!requireClick) {
            js.onmouseenter = {
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
        js.style.flexGrow = "$amount"
        js.style.flexShrink = "$amount"
        js.style.flexBasis = "0"
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        js.classList.add("h${horizontal}", "v${vertical}")
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.scrolls: ViewWrapper
    get() {
        beforeNextElementSetup {
            js.classList.add("scroll-vertical")
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.scrollsHorizontally: ViewWrapper
    get() {
        beforeNextElementSetup {
            js.classList.add("scroll-horizontal")
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    beforeNextElementSetup {

        if (constraints.minHeight == null) js.style.removeProperty("minHeight")
        else js.style.minHeight = constraints.minHeight.value

        if (constraints.maxHeight == null) js.style.removeProperty("maxHeight")
        else js.style.maxHeight = constraints.maxHeight.value

        if (constraints.minWidth == null) js.style.removeProperty("minWidth")
        else js.style.minWidth = constraints.minWidth.value

        if (constraints.maxWidth == null) js.style.removeProperty("maxWidth")
        else js.style.maxWidth = constraints.maxWidth.value

        if (constraints.width == null) js.style.removeProperty("width")
        else js.style.width = constraints.width.value

        if (constraints.height == null) js.style.removeProperty("height")
        else js.style.height = constraints.height.value
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.padded: ViewWrapper
    get() {
        beforeNextElementSetup {
            js.classList.add("padded")
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.unpadded: ViewWrapper
    get() {
        beforeNextElementSetup {
            js.classList.add("unpadded")
        }
        return ViewWrapper
    }


// End

private var HTMLElement.__kiteui__hiddenAnim by JsAnyDelegate<HTMLElement, Animation>("__kiteui__hiddenAnim")
private var HTMLElement.__kiteui__hiddenAnim2 by JsAnyDelegate<HTMLElement, Animation>("__kiteui__hiddenAnim2")
private var HTMLElement.__kiteui__goalHidden by JsAnyDelegate<HTMLElement, JsBoolean>("__kiteui__goalHidden")

@ViewModifierDsl3
actual fun ViewWriter.onlyWhen(default: Boolean, condition: suspend () -> Boolean): ViewWrapper {
    wrapNext("div", ::NContainingView) {
        js.className = "hidingContainer"
        js.hidden = !default
        var last = !default
        window.setTimeout({
            calculationContext.reactiveScope {
                val child = js.firstElementChild as? HTMLElement ?: return@reactiveScope
                val value = !condition()
                if (value == last) return@reactiveScope
                last = value
                if (animationsEnabled) {
                    js.classList.add("animatingShowHide")

                    val myStyle = window.getComputedStyle(child)
                    val transitionTime = myStyle.transitionDuration.let { Duration.parseOrNull(it) } ?: 150.milliseconds
                    val totalTime = transitionTime.inWholeMilliseconds.toDouble()
                    var oldAnimTime = totalTime
                    (this.js.__kiteui__hiddenAnim as? Animation)?.let {
                        oldAnimTime = it.currentTime
                        it.cancel()
                    }
                    (this.js.__kiteui__hiddenAnim2 as? Animation)?.let {
                        it.cancel()
                    }
                    this.js.__kiteui__goalHidden = value.toJsBoolean()
                    js.hidden = false
                    val parent = generateSequence(this as HTMLElement) { it.parentElement as? HTMLElement }.drop(1)
                        .firstOrNull { !it.classList.contains("toggle-button") } ?: return@reactiveScope
                    val parentStyle = window.getComputedStyle(parent)
                    val x =
                        parentStyle.display == "flex" && parentStyle.flexDirection.contains("row")// && myStyle.width.none { it.isDigit() }
                    val y =
                        parentStyle.display == "flex" && parentStyle.flexDirection.contains("column")// && myStyle.height.none { it.isDigit() }

                    val before = jsObj()
                    val after = jsObj()
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
                        gone.set("marginLeft", "calc($gap / -2.0)".toJsString())
                        gone.set("paddingLeft", "0px".toJsString())
                        gone.set("marginRight", "calc($gap / -2.0)".toJsString())
                        gone.set("paddingRight", "0px".toJsString())
                        gone.set("width", "0px".toJsString())
                        gone.set("minWidth", "0px".toJsString())
                        gone.set("maxWidth", "0px".toJsString())
                        full.set("width", fullWidth.toJsString())
                        full.set("minWidth", fullWidth.toJsString())
                        full.set("maxWidth", fullWidth.toJsString())
                    }
                    if (y) {
                        goneTransform.add("scaleY(0)")
                        fullTransform.add("scaleY(1)")
                        gone.set("marginTop", "calc($gap / -2.0)".toJsString())
                        gone.set("paddingTop", "0px".toJsString())
                        gone.set("marginBottom", "calc($gap / -2.0)".toJsString())
                        gone.set("paddingBottom", "0px".toJsString())
                        gone.set("height", "0px".toJsString())
                        gone.set("minHeight", "0px".toJsString())
                        gone.set("maxHeight", "0px".toJsString())
                        full.set("height", fullHeight.toJsString())
                        full.set("minHeight", fullHeight.toJsString())
                        full.set("maxHeight", fullHeight.toJsString())
                    }
                    if (!x && !y) {
                        full.set("opacity", "1".toJsString())
                        gone.set("opacity", "0".toJsString())
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
                        jsArrayOf(before, after),
                        jsObj(
                            "duration" to totalTime.toJsNumber(),
                            "easing" to "ease-out".toJsString()
                        )
                    ).let {
                        it.currentTime = (totalTime - oldAnimTime).coerceAtLeast(0.0)
                        it.onfinish = { ev ->
                            if (this.__kiteui__hiddenAnim == it) {
                                hidden = value
                                classList.remove("animatingShowHide")
                                this.__kiteui__hiddenAnim = null
                                child.style.removeProperty("width")
                                child.style.removeProperty("maxWidth")
                                child.style.removeProperty("height")
                                child.style.removeProperty("maxHeight")
                            }
                        }
                        it.oncancel = { ev ->
                            if (this.__kiteui__hiddenAnim == it) {
                                hidden = value
                                classList.remove("animatingShowHide")
                                this.__kiteui__hiddenAnim = null
                                child.style.removeProperty("width")
                                child.style.removeProperty("maxWidth")
                                child.style.removeProperty("height")
                                child.style.removeProperty("maxHeight")
                            }
                        }
                        it.onremove = { ev ->
                            if (this.__kiteui__hiddenAnim == it) {
                                hidden = value
                                classList.remove("animatingShowHide")
                                this.__kiteui__hiddenAnim = null
                                child.style.removeProperty("width")
                                child.style.removeProperty("maxWidth")
                                child.style.removeProperty("height")
                                child.style.removeProperty("maxHeight")
                            }
                        }
                        this.__kiteui__hiddenAnim = it
                    }
                } else {
                    js.hidden = !value
                }
            }
            null
        }, 1)
    }
    return ViewWrapper
}