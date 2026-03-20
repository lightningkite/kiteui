@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.reactive.context.*

@ViewModifierDsl3
actual fun ElementWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ElementWriter = beforeSetup {
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

@ViewModifierDsl3
actual fun ElementWriter.textPopover(message: String): ElementWriter = hintPopover {
    themed(PopoverSemantic).text(message)
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddWeight.weight(amount: Float): ElementWriter.CanAddShownWhen {
    return beforeSetup {
        native.style.flexGrow = "$amount"
        native.style.flexShrink = "$amount"
        native.style.flexBasis = "0"
        parent?.native?.classes?.add("childHasWeight")
    }
}

// by Claude - wrapper pattern for animation-aware weight changes
@ViewModifierDsl3
actual fun ElementWriter.CanAddWeight.changingWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddShownWhen {
    return write(object : NativeContainerElement(context) {
        init {
            native.tag = "div"
            native.classes.add("noInteraction")
            native.classes.add("kiteui-stack")
            var previousAmount: Float? = null
            reactive {
                val newAmount = amount()
                val oldAmount = previousAmount
                previousAmount = newAmount
                if (areAnimationsEnabled && fullyStarted && oldAmount != null && oldAmount != newAmount) {
                    nativeAnimateWeight(oldAmount, newAmount)
                } else {
                    // Apply immediately (initial render or animations disabled)
                    native.style.flexGrow = "$newAmount"
                    native.style.flexShrink = "$newAmount"
                    native.style.flexBasis = if (newAmount != 0f) "0" else "auto"
                }
                parent?.native?.classes?.add("childHasWeight")
            }
        }

        override fun nativeAddChild(index: Int, element: Element) {
            super.nativeAddChild(index, element)
            element.underlyingNativeElement.themeBase = GetBaseTheme.fromParentNonCascading
            Frame.internalAddChildStack(this, index, element)
        }
    }) {}
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddAlignment.align(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight =
    beforeSetup { // Use parent's default alignment if not explicitly set (Align.Stretch means not set)
        native.classes.add("h${horizontal}")
        native.desiredHorizontalGravity = horizontal
        native.classes.add("v${vertical}")
        native.desiredVerticalGravity = vertical
    }

@ViewModifierDsl3
actual inline fun ElementWriter.CanAddScrolling.__scrollsUncontracted(vertical: Boolean, horizontal: Boolean, crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter =
    beforeSetup { setup(ScrollingBehaviorImpl(this, horizontal = horizontal, vertical = vertical)) }

@ViewModifierDsl3
actual inline fun ElementWriter.CanAddScrolling.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ElementWriter =
    // For web, we'll just use regular scrolling as pull-to-refresh isn't a common pattern on web
    beforeSetup { setup(ScrollingBehaviorImpl(this, horizontal = horizontal, vertical = vertical)) }

@ViewModifierDsl3
actual fun ElementWriter.CanAddSizing.sizedBox(constraints: SizeConstraints): ElementWriter.CanAddScrolling =
    beforeSetup {
        if (constraints.minHeight == null) native.style.minHeight = null
        else native.style.minHeight = constraints.minHeight.value.toString()

        if (constraints.maxHeight == null) native.style.maxHeight = null
        else native.style.maxHeight = constraints.maxHeight.value.toString()

        if (constraints.minWidth == null) native.style.minWidth = null
        else native.style.minWidth = constraints.minWidth.value.toString()

        if (constraints.maxWidth == null) native.style.maxWidth = null
        else native.style.maxWidth = constraints.maxWidth.value.toString()

        if (constraints.aspectRatio == null) native.setStyleProperty("aspect-ratio", null)
        else native.setStyleProperty(
            "aspect-ratio",
            "${constraints.aspectRatio} / 1"
        )

        if (constraints.width == null) native.style.width = null
        else native.style.width = constraints.width.value.toString()

        if (constraints.height == null) native.style.height = null
        else native.style.height = constraints.height.value.toString()
    }

@ViewModifierDsl3
actual fun ElementWriter.CanAddSizing.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddScrolling =
    beforeSetup {
        reactive {
            val constraints = constraints()
            if (constraints.minHeight == null) native.style.minHeight = null
            else native.style.minHeight = constraints.minHeight.value.toString()

            if (constraints.maxHeight == null) native.style.maxHeight = null
            else native.style.maxHeight = constraints.maxHeight.value.toString()

            if (constraints.minWidth == null) native.style.minWidth = null
            else native.style.minWidth = constraints.minWidth.value.toString()

            if (constraints.maxWidth == null) native.style.maxWidth = null
            else native.style.maxWidth = constraints.maxWidth.value.toString()

            if (constraints.aspectRatio == null) native.setStyleProperty("aspect-ratio", null)
            else native.setStyleProperty(
                "aspect-ratio",
                "${constraints.aspectRatio} / 1"
            )

            if (constraints.width == null) native.style.width = null
            else native.style.width = constraints.width.value.toString()

            if (constraints.height == null) native.style.height = null
            else native.style.height = constraints.height.value.toString()
        }
    }

// End

@ViewModifierDsl3
actual fun ElementWriter.CanAddShownWhen.shownWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddTheme {
    return write(object : NativeContainerElement(context) {
        init {
            native.tag = "div"
            native.classes.add("noInteraction")
            native.classes.add("kiteui-stack")
            native.attributes.hidden = !default
            var currentState = default
            reactive {
                if (areAnimationsEnabled && fullyStarted) {
                    val c = condition()
                    if (c != currentState) {
                        if (condition()) {
                            nativeAnimateShow()
                        } else {
                            nativeAnimateHide()
                        }
                    }
                    currentState = c
                } else {
                    val c = condition()
                    if (c != currentState) {
                        native.attributes.hidden = !condition()
                    }
                    currentState = c
                }
            }
        }

        override fun nativeAddChild(index: Int, element: Element) {
            super.nativeAddChild(index, element)
            element.underlyingNativeElement.themeBase = GetBaseTheme.fromParentNonCascading
            Frame.internalAddChildStack(this, index, element)
        }
    }) {}
}

internal expect fun ContainerElement.nativeAnimateShow()
internal expect fun ContainerElement.nativeAnimateHide()

// by Claude - expect for weight animation
internal expect fun ContainerElement.nativeAnimateWeight(fromWeight: Float, toWeight: Float)
