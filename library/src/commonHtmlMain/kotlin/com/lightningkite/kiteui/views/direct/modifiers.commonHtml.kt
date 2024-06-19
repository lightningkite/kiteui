package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.invokeAllSafe
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ViewWrapper {
    beforeNextElementSetup {
        val floating = FloatingInfoHolder(this)
        floating.menuGenerator = setup
        floating.preferredDirection = preferredDirection
        native.addEventListener("contextmenu") {
            floating.open()
        }
        native.addEventListener("mouseenter") {
            floating.open()
        }
        native.addEventListener("mouseleave") {
            floating.close()
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
        val floating = FloatingInfoHolder(this)
        floating.menuGenerator = {
            setup(this, object : PopoverContext {
                override val calculationContext: CalculationContext
                    get() = this@beforeNextElementSetup

                override fun close() {
                    popoverClosers.invokeAllSafe()
                }
            })
        }
        floating.preferredDirection = preferredDirection
        if (this is Button || requiresClick)
            native.addEventListener("click") {
                floating.open()
            }
        native.addEventListener("contextmenu") {
            floating.open()
        }
        native.addEventListener("mouseenter") {
            floating.open()
        }
        native.addEventListener("mouseleave") {
            floating.close()
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
        native.style.flexGrow = "$amount"
        native.style.flexShrink = "$amount"
        native.style.flexBasis = "0"
        parent?.native?.classes?.add("childHasWeight")
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: suspend () -> Float): ViewWrapper {
    beforeNextElementSetup {
        reactiveScope {
            val amount = amount()
            if (amount != 0f) {
                native.style.flexGrow = "$amount"
                native.style.flexShrink = "$amount"
                native.style.flexBasis = "0"
            } else {
                native.style.flexGrow = "0"
                native.style.flexShrink = "0"
                native.style.flexBasis = "auto"
            }
            parent?.native?.classes?.add("childHasWeight")
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        native.classes.add("h${horizontal}")
        native.classes.add("v${vertical}")
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.scrolls: ViewWrapper
    get() {
        beforeNextElementSetup {
            native.classes.add("scroll-vertical")
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.scrollsHorizontally: ViewWrapper
    get() {
        beforeNextElementSetup {
            native.classes.add("scroll-horizontal")
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    beforeNextElementSetup {

        if (constraints.minHeight == null) native.style.minHeight = null
        else native.style.minHeight = constraints.minHeight.value

        if (constraints.maxHeight == null) native.style.maxHeight = null
        else native.style.maxHeight = constraints.maxHeight.value

        if (constraints.minWidth == null) native.style.minWidth = null
        else native.style.minWidth = constraints.minWidth.value

        if (constraints.maxWidth == null) native.style.maxWidth = null
        else native.style.maxWidth = constraints.maxWidth.value

        if (constraints.aspectRatio == null) native.setStyleProperty("aspect-ratio", null)
        else native.setStyleProperty(
            "aspect-ratio",
            "${constraints.aspectRatio.first} / ${constraints.aspectRatio.second}"
        )

        if (constraints.width == null) native.style.width = null
        else native.style.width = constraints.width.value

        if (constraints.height == null) native.style.height = null
        else native.style.height = constraints.height.value
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: suspend () -> SizeConstraints): ViewWrapper {
    beforeNextElementSetup {

        reactiveScope {
            val constraints = constraints()
            if (constraints.minHeight == null) native.style.minHeight = null
            else native.style.minHeight = constraints.minHeight.value

            if (constraints.maxHeight == null) native.style.maxHeight = null
            else native.style.maxHeight = constraints.maxHeight.value

            if (constraints.minWidth == null) native.style.minWidth = null
            else native.style.minWidth = constraints.minWidth.value

            if (constraints.maxWidth == null) native.style.maxWidth = null
            else native.style.maxWidth = constraints.maxWidth.value

            if (constraints.aspectRatio == null) native.setStyleProperty("aspect-ratio", null)
            else native.setStyleProperty(
                "aspect-ratio",
                "${constraints.aspectRatio.first} / ${constraints.aspectRatio.second}"
            )

            if (constraints.width == null) native.style.width = null
            else native.style.width = constraints.width.value

            if (constraints.height == null) native.style.height = null
            else native.style.height = constraints.height.value
        }
    }
    return ViewWrapper
}

// End

@ViewModifierDsl3
actual fun ViewWriter.onlyWhen(default: Boolean, condition: suspend () -> Boolean): ViewWrapper {
//    // TODO: include old animation code
//    beforeNextElementSetup {
//        ::exists.invoke(condition)
//    }
    wrapNextIn(object: RView(context) {
        init {
            native.tag = "div"
            native.classes.add("hidingContainer")
            native.classes.add("kiteui-stack")
            nativeAnimateHideBinding(default, condition)
        }
    })
    return ViewWrapper
}

internal expect fun RView.nativeAnimateHideBinding(default: Boolean, condition: suspend () -> Boolean)
//@ViewModifierDsl3
//actual fun ViewWriter.onlyWhen(default: Boolean, condition: suspend () -> Boolean): ViewWrapper {
//    wrapNext(document.createElement("div") as HTMLDivElement) {
//        classList.add("hidingContainer")
//        hidden = !default
//        var last = !default
//        calculationContext.reactiveScope {
//            val child = firstElementChild as? HTMLElement
//            val value = !condition()
//            if (value == last) return@reactiveScope
//            last = value
//            if (animationsEnabled && child != null) {
//                classList.add("animatingShowHide")
//
//                val myStyle = window.getComputedStyle(child)
//                val transitionTime = myStyle.transitionDuration.let { Duration.parseOrNull(it) } ?: 150.milliseconds
//                val totalTime = transitionTime.inWholeMilliseconds.toDouble()
//                var oldAnimTime = totalTime
//                (this.asDynamic().__kiteui__hiddenAnim as? Animation)?.let {
//                    oldAnimTime = it.currentTime
//                    it.cancel()
//                }
//                (this.asDynamic().__kiteui__hiddenAnim2 as? Animation)?.let {
//                    it.cancel()
//                }
//                this.asDynamic().__kiteui__goalHidden = value
//                hidden = false
//                val parent = generateSequence(this as HTMLElement) { it.parentElement as? HTMLElement }.drop(1)
//                    .firstOrNull { !it.classList.contains("toggle-button") } ?: return@reactiveScope
//                val parentStyle = window.getComputedStyle(parent)
//                val x =
//                    parentStyle.display == "flex" && parentStyle.flexDirection.contains("row")// && myStyle.width.none { it.isDigit() }
//                val y =
//                    parentStyle.display == "flex" && parentStyle.flexDirection.contains("column")// && myStyle.height.none { it.isDigit() }
//
//                val before = js("{}")
//                val after = js("{}")
//                val full = if (value) before else after
//                val fullTransform = ArrayList<String>()
//                val gone = if (value) after else before
//                val goneTransform = ArrayList<String>()
//
//                var fullWidth = ""
//                var fullHeight = ""
//                var gap = ""
//                if (hidden) {
//                    hidden = false
//                    fullWidth = myStyle.width
//                    fullHeight = myStyle.height
//                    gap = parentStyle.columnGap
//                    hidden = true
//                } else {
//                    fullWidth = myStyle.width
//                    fullHeight = myStyle.height
//                    gap = parentStyle.columnGap
//                }
//                child.style.width = myStyle.width
//                child.style.maxWidth = "unset"
//                child.style.height = myStyle.height
//                child.style.maxHeight = "unset"
//
//                if (x) {
//                    goneTransform.add("scaleX(0)")
//                    fullTransform.add("scaleX(1)")
//                    gone.marginLeft = "calc($gap / -2.0)"
//                    gone.paddingLeft = "0px"
//                    gone.marginRight = "calc($gap / -2.0)"
//                    gone.paddingRight = "0px"
//                    gone.width = "0px"
//                    gone.minWidth = "0px"
//                    gone.maxWidth = "0px"
//                    full.width = fullWidth
//                    full.minWidth = fullWidth
//                    full.maxWidth = fullWidth
//                }
//                if (y) {
//                    goneTransform.add("scaleY(0)")
//                    fullTransform.add("scaleY(1)")
//                    gone.marginTop = "calc($gap / -2.0)"
//                    gone.paddingTop = "0px"
//                    gone.marginBottom = "calc($gap / -2.0)"
//                    gone.paddingBottom = "0px"
//                    gone.height = "0px"
//                    gone.minHeight = "0px"
//                    gone.maxHeight = "0px"
//                    full.height = fullHeight
//                    full.minHeight = fullHeight
//                    full.maxHeight = fullHeight
//                }
//                if (!x && !y) {
//                    full.opacity = "1"
//                    gone.opacity = "0"
//                }
//                goneTransform.takeUnless { it.isEmpty() }?.let {
////                        gone.transform = it.joinToString(" ")
////                        gone.transformOrigin = "top left"
//                }
//                fullTransform.takeUnless { it.isEmpty() }?.let {
////                        full.transform = it.joinToString(" ")
////                        full.transformOrigin = "top left"
//                }
//                this.animate(
//                    arrayOf(before, after),
//                    js(
//                        "duration" to totalTime,
//                        "easing" to "ease-out"
//                    )
//                ).let {
//                    it.currentTime = (totalTime - oldAnimTime).coerceAtLeast(0.0)
//                    it.onfinish = { ev ->
//                        if (this.asDynamic().__kiteui__hiddenAnim == it) {
//                            hidden = value
//                            classList.remove("animatingShowHide")
//                            this.asDynamic().__kiteui__hiddenAnim = null
//                            child.style.removeProperty("width")
//                            child.style.removeProperty("maxWidth")
//                            child.style.removeProperty("height")
//                            child.style.removeProperty("maxHeight")
//                        }
//                    }
//                    it.oncancel = { ev ->
//                        if (this.asDynamic().__kiteui__hiddenAnim == it) {
//                            hidden = value
//                            classList.remove("animatingShowHide")
//                            this.asDynamic().__kiteui__hiddenAnim = null
//                            child.style.removeProperty("width")
//                            child.style.removeProperty("maxWidth")
//                            child.style.removeProperty("height")
//                            child.style.removeProperty("maxHeight")
//                        }
//                    }
//                    it.onremove = { ev ->
//                        if (this.asDynamic().__kiteui__hiddenAnim == it) {
//                            hidden = value
//                            classList.remove("animatingShowHide")
//                            this.asDynamic().__kiteui__hiddenAnim = null
//                            child.style.removeProperty("width")
//                            child.style.removeProperty("maxWidth")
//                            child.style.removeProperty("height")
//                            child.style.removeProperty("maxHeight")
//                        }
//                    }
//                    this.asDynamic().__kiteui__hiddenAnim = it
//                }
//            } else {
//                hidden = value
//            }
//        }
//    }
//    return ViewWrapper
//}