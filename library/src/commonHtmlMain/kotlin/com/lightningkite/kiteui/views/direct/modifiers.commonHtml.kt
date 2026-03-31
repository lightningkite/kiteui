package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ViewWriter {
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
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWriter {
    beforeNextElementSetup {
        val floating = FloatingInfoHolder(this)
        floating.menuGenerator = {
            setup(this, object : PopoverContext {
                override val calculationContext: CoroutineScope
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
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWriter = hasPopover {
    card.text {
        content = message
    }
}

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWriter {
    beforeNextElementSetup {
        lastSetWeight = amount
        native.style.flexGrow = "$amount"
        native.style.flexShrink = "$amount"
        native.style.flexBasis = "0"
        parent?.native?.classes?.add("childHasWeight")
    }
        .let { return it }
}

// by Claude - wrapper pattern for animation-aware weight changes
@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWriter {
    return write(object : RViewWriter(context) {
        init {
            native.tag = "div"
            native.classes.add("noInteraction")
            native.classes.add("kiteui-stack")
            var previousAmount: Float? = null
            reactive {
                val newAmount = amount()
                val oldAmount = previousAmount
                previousAmount = newAmount
                lastSetWeight = newAmount
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
        override fun internalAddChild(index: Int, view: RView) {
            super.internalAddChild(index, view)
            view.themeTakeNonCascadingFromParent = true
            Frame.internalAddChildStack(this, index, view)
        }
        override val mySpacingForChildren: Dimension
            get() = parent?.mySpacingForChildren ?: 0.px
    }) {}
}

@ViewModifierDsl3
actual fun ViewWriter.align(horizontal: Align, vertical: Align): ViewWriter {
    beforeNextElementSetup {
        lastSetHorizontalAlign = horizontal
        lastSetVerticalAlign = vertical

        // Use parent's default alignment if not explicitly set (Align.Stretch means not set)
        val effectiveHorizontal = if (horizontal == Align.Stretch) {
            parent?.newChildHorizontalAlign ?: horizontal
        } else horizontal

        val effectiveVertical = if (vertical == Align.Stretch) {
            parent?.newChildVerticalAlign ?: vertical
        } else vertical

        native.classes.add("h${effectiveHorizontal}")
        native.desiredHorizontalGravity = effectiveHorizontal
        native.classes.add("v${effectiveVertical}")
        native.desiredVerticalGravity = effectiveVertical
    }
        .let { return it }
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsUncontracted(vertical: Boolean, horizontal: Boolean, crossinline setup: ScrollingBehaviors.()->Unit): ViewWriter {
    beforeNextElementSetup {
        setup(ScrollingBehaviorImpl(this, horizontal = horizontal, vertical = vertical))
    }
        .let { return it }
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWriter {
    beforeNextElementSetup {
        setup(ScrollingBehaviorImpl(this, horizontal = horizontal, vertical = vertical))
        if (vertical) {
            native.setStyleProperty("overscroll-behavior-y", "none")
            native.setStyleProperty("position", "relative")
            nativeSetupPullToRefresh(refreshAction)
        }
    }
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWriter {
    beforeNextElementSetup {

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
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWriter {
    beforeNextElementSetup {

        reactiveScope {
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
        .let { return it }
}

// End

@ViewModifierDsl3
actual fun ViewWriter.shownWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ViewWriter {
    var v: RView? = null
    return write(object: RViewWriter(context) {
        init {
            v = this
            native.tag = "div"
            native.classes.add("noInteraction")
            native.classes.add("kiteui-stack")
            native.attributes.hidden = !default
            var currentState = default
            reactive {
                if(areAnimationsEnabled && fullyStarted) {
                    val c = condition()
                    if(c != currentState) {
                        if (condition()) {
                            nativeAnimateShow()
                        } else {
                            nativeAnimateHide()
                        }
                    }
                    currentState = c
                } else {
                    val c = condition()
                    if(c != currentState) {
                        native.attributes.hidden = !condition()
                    }
                    currentState = c
                }
            }
        }
        override fun internalAddChild(index: Int, view: RView) {
            super.internalAddChild(index, view)
            view.themeTakeNonCascadingFromParent = true
            Frame.internalAddChildStack(this, index, view)
        }

        override val mySpacingForChildren: Dimension
            get() = parent?.mySpacingForChildren ?: 0.px
    }) {}
}

internal expect fun RView.nativeAnimateShow()
internal expect fun RView.nativeAnimateHide()
// by Claude - expect for weight animation
internal expect fun RView.nativeAnimateWeight(fromWeight: Float, toWeight: Float)
@PublishedApi
internal expect fun RView.nativeSetupPullToRefresh(refreshAction: Action)
