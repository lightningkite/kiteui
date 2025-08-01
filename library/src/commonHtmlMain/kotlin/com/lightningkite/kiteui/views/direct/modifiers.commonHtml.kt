package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.*
import com.lightningkite.signal.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.CoroutineScope

@InternalKiteUi
@ViewModifierDsl3
public actual fun ViewWriter.hintPopover(
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

@InternalKiteUi
@ViewModifierDsl3
public actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper {
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
    return ViewWrapper
}

@InternalKiteUi
@ViewModifierDsl3
public actual fun ViewWriter.textPopover(message: String): ViewWrapper = hasPopover {
    card - text {
        content = message
    }
}

@InternalKiteUi
@ViewModifierDsl3
public actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    beforeNextElementSetup {
        native.style.flexGrow = "$amount"
        native.style.flexShrink = "$amount"
        native.style.flexBasis = "0"
        parent?.native?.classes?.add("childHasWeight")
    }
    return ViewWrapper
}

@ViewModifierDsl3
public actual fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWrapper {
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
public actual fun ViewWriter.align(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        native.classes.add("h${horizontal}")
        native.desiredHorizontalGravity = horizontal
        native.classes.add("v${vertical}")
        native.desiredVerticalGravity = vertical
    }
    return ViewWrapper
}

@InternalKiteUi
@ViewModifierDsl3
public actual inline fun ViewWriter.__scrollsUncontracted(vertical: Boolean, horizontal: Boolean, crossinline setup: ScrollingBehaviors.()->Unit): ViewWrapper {
    beforeNextElementSetup {
        setup(ScrollingBehaviorImpl(this, horizontal = horizontal, vertical = vertical))
    }
    return ViewWrapper
}

@InternalKiteUi
@ViewModifierDsl3
public actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
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
    return ViewWrapper
}

@ViewModifierDsl3
public actual fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWrapper {
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
    return ViewWrapper
}

// End

@ViewModifierDsl3
public actual fun ViewWriter.shownWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ViewWrapper {
//    // TODO: include old animation code
//    beforeNextElementSetup {
//        ::exists.invoke(condition)
//    }
    var v: RView? = null
    wrapNextIn(object: RViewWrapper(context) {
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
    })
    return object: ViewWrapper() {
        override fun view(): RView? = v
    }
}

internal expect fun RView.nativeAnimateShow()
internal expect fun RView.nativeAnimateHide()
