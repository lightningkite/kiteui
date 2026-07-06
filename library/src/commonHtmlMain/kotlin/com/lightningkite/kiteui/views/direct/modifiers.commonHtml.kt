@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
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
actual fun ElementWriter.CanAddWeight.weight(amount: Float): ElementWriter.CanAddListElementModifier {
    return beforeSetup {
        native.style.flexGrow = "$amount"
        native.style.flexShrink = "$amount"
        native.style.flexBasis = "0"
        parent?.native?.classes?.add("childHasWeight")
    }
}

// by Claude - wrapper pattern for animation-aware weight changes
@ViewModifierDsl3
actual fun ElementWriter.CanAddWeight.dynamicWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddListElementModifier {
    return lazyInjectModifierWriter {
        object : NativeContainerElement(context) {
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
        }
    }
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
    beforeSetup {
        setup(ScrollingBehaviorImpl(this, horizontal = horizontal, vertical = vertical))

        if (vertical) {
            native.setStyleProperty("overscroll-behavior-y", "none")
            nativeSetupPullToRefresh(refreshAction)
        }
    }

@ViewModifierDsl3
actual fun ElementWriter.CanAddSizing.sizedBox(constraints: SizeConstraints): ElementWriter.CanAddTheme =
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
actual fun ElementWriter.CanAddSizing.dynamicSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme =
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

//inline fun ElementWriter.addsSecretElement(crossinline produce: () -> ContainerElement): ViewWriter =
//    object : ViewWriter, ElementWriter by this {
//        var current: ContainerElement? = null
//
//        @OverrideOnly
//        override fun willAddChild(element: Element) {
//            val e = this@addsSecretElement.write(produce())
//            current = e
//            e.willAddChild(element)
//        }
//
//        @OverrideOnly
//        override fun addChild(element: Element) {
//            current?.addChild(element) ?: throw IllegalStateException("addChild called on $element before willAddChild!")
//        }
//    }

@ViewModifierDsl3
actual fun ElementWriter.CanAddShownWhen.shownWhen(default: Boolean, transition: ScreenTransition, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddSizing {
    return lazyInjectModifierWriter {
        object : NativeContainerElement(context) {
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
                            if (c) {
                                nativeAnimateShow(transition)
                            } else {
                                nativeAnimateHide(transition)
                            }
                        }
                        currentState = c
                    } else {
                        val c = condition()
                        if (c != currentState) {
                            native.attributes.hidden = !c
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

            @Suppress("DEPRECATION")
            @Deprecated("Will probably be removed in the future.")
            override val spacingForChildCornerRadii: Dimension
                get() = parent?.spacingForChildCornerRadii ?: 0.px
        }
    }
}

internal expect fun ContainerElement.nativeAnimateShow(transition: ScreenTransition = ScreenTransition.None)
internal expect fun ContainerElement.nativeAnimateHide(transition: ScreenTransition = ScreenTransition.None)

// by Claude - expect for weight animation
internal expect fun ContainerElement.nativeAnimateWeight(fromWeight: Float, toWeight: Float)

@PublishedApi
internal expect fun Element.nativeSetupPullToRefresh(refreshAction: Action)

private class ApplyTag(
    val tag: String,
    val wraps: ElementWriter,
) : ViewWriter, ElementWriter by wraps {

    var wrapperElement: NativeContainerElement? = null

    @OverrideOnly
    override fun willAddChild(element: Element) {
        if (element.native.tag == "span" || element.native.tag == "div") {
            wrapperElement = null
            element.native.tag = tag
            wraps.willAddChild(element)
        } else {
            val we: NativeContainerElement = PassthroughContainer(wraps.context)
            we.native.tag = tag
            wraps.willAddChild(we)
            we.willAddChild(element)
            wrapperElement = we
        }
    }

    @OverrideOnly
    override fun addChild(element: Element) {
        wrapperElement?.let {
            it.addChild(element)
            it.onStartup()
            wraps.addChild(it)
        } ?: wraps.addChild(element)
    }
}

/**
 * Invisible wrapper that exists only to carry a semantic HTML tag (e.g. `<h1>`, `<nav>`) around an
 * element whose own tag cannot be rewritten. It must be layout-transparent: the single child must
 * fill it so that weight/stretch applied by the parent (which lands on this wrapper) flows through
 * to the content.
 *
 * It always holds exactly one child, and — because the modifier order forces alignment/weight/sizing
 * onto the wrapper rather than the child — that child always wants to fill the wrapper. So instead of
 * the general stack machinery ([Frame.internalAddChildStack], built for overlapping children) we use
 * the cheapest layout that fills both axes: a flex box whose single child grows along the main axis
 * (width) and stretches along the cross axis (height) via flex's default `align-items: stretch`.
 * `display: contents` is unusable here: with no box, the parent's layout analysis would have nothing
 * to act on.
 */
internal class PassthroughContainer(context: ElementContext): NativeContainerElement(context) {
    init {
        native.tag = "div"
        native.style.display = "flex"
    }

    override fun nativeAddChild(index: Int, element: Element) {
        // Purely a semantic label, so don't introduce a new theme/card boundary.
        element.underlyingNativeElement.themeBase = GetBaseTheme.fromParentNonCascading
        super.nativeAddChild(index, element)
        // Fill the main axis; the cross axis fills automatically via flex's default align-items: stretch.
        element.underlyingNativeElement.native.style.flexGrow = "1"
    }
}

@ViewModifierDsl3 actual fun ElementWriter.CanAddTheme.asHeading(level: Int): ElementWriter.CanAddTheme = ApplyTag("h${level.coerceIn(1, 6)}", this)
@ViewModifierDsl3 actual val ElementWriter.CanAddTheme.asMain: ElementWriter.CanAddTheme get() = ApplyTag("main", this)
@ViewModifierDsl3 actual val ElementWriter.CanAddTheme.asNavigation: ElementWriter.CanAddTheme get() = ApplyTag("nav", this)
@ViewModifierDsl3 actual val ElementWriter.CanAddTheme.asBanner: ElementWriter.CanAddTheme get() = ApplyTag("header", this)
@ViewModifierDsl3 actual val ElementWriter.CanAddTheme.asContentInfo: ElementWriter.CanAddTheme get() = ApplyTag("footer", this)
@ViewModifierDsl3 actual val ElementWriter.CanAddTheme.asComplementary: ElementWriter.CanAddTheme get() = ApplyTag("aside", this)
@ViewModifierDsl3 actual val ElementWriter.CanAddTheme.asSearch: ElementWriter.CanAddTheme get() = ApplyTag("search", this)
@ViewModifierDsl3 actual val ElementWriter.CanAddTheme.asPresentation: ElementWriter.CanAddTheme get() =
    beforeSetup { native.setAttribute("aria-hidden", "true") }
@ViewModifierDsl3 actual val ElementWriter.CanAddTheme.asList: ElementWriter.CanAddTheme get() = ApplyTag("ul", this)
@ViewModifierDsl3 actual val ElementWriter.CanAddListElementModifier.asListItem: ElementWriter.CanAddListElementModifier get() = ApplyTag("li", this)

internal actual fun ContainerElement.setupAsListContainer() {
    if (native.tag == "div" || native.tag == "span") native.tag = "ul"
}
