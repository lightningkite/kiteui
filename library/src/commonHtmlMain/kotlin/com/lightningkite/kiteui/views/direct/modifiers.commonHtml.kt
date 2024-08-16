package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ViewWrapper
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
                    closePopovers()
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
        native.desiredHorizontalGravity = horizontal
        native.classes.add("v${vertical}")
        native.desiredVerticalGravity = vertical
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
            "${constraints.aspectRatio} / 1"
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
                "${constraints.aspectRatio} / 1"
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
        override fun internalAddChild(index: Int, view: RView) {
            super.internalAddChild(index, view)
            Stack.internalAddChildStack(this, index, view)
        }
    })
    return ViewWrapper
}

internal expect fun RView.nativeAnimateHideBinding(default: Boolean, condition: suspend () -> Boolean)
