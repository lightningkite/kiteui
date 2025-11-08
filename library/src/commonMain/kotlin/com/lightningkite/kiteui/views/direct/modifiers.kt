package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ForcePaddingSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ViewModifierDsl3
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


@ViewModifierDsl3
expect fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight,
    setup: ViewWriter.() -> Unit
): ViewWriter

@Deprecated("Use hintPopover or opensMenu depending on your situation.")
@ViewModifierDsl3
expect fun ViewWriter.hasPopover(
    requiresClick: Boolean = false,
    preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWriter

interface PopoverContext {
    val calculationContext: CalculationContext
    fun close()
}

@ViewModifierDsl3
expect fun ViewWriter.textPopover(message: String): ViewWriter

@ViewModifierDsl3
expect fun ViewWriter.weight(amount: Float): ViewWriter

@ViewModifierDsl3
expect fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWriter

@ViewModifierDsl3
expect fun ViewWriter.align(horizontal: Align, vertical: Align): ViewWriter

@ViewModifierDsl3
@Deprecated("use align instead", ReplaceWith("align"))
fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWriter = align(horizontal, vertical)

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
val ViewWriter.scrolls: ViewWriter get() = __scrollsUncontracted(true, false)

@ViewModifierDsl3
@Deprecated("use scrollingHorizontally instead", ReplaceWith("scrollsHorizontally"))
val ViewWriter.scrollsHorizontally: ViewWriter get() = __scrollsUncontracted(false, true)

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
inline fun ViewWriter.scrolls(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWriter {
    return __scrollsUncontracted(vertical = true, horizontal = false, setup)
}

@ViewModifierDsl3
@Deprecated("use scrollingHorizontally instead", ReplaceWith("scrollingHorizontally"))
inline fun ViewWriter.scrollsHorizontally(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWriter {
    return __scrollsUncontracted(vertical = false, horizontal = true, setup)
}

@ViewModifierDsl3
@Deprecated("use scrollingBoth instead", ReplaceWith("scrollingBoth"))
inline fun ViewWriter.scrollsBoth(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWriter {
    return __scrollsUncontracted(vertical = true, horizontal = true, setup)
}

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
inline fun ViewWriter.scrolls(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWriter {
    return __scrollsUncontracted(vertical = vertical, horizontal = horizontal, setup)
}

@ViewModifierDsl3
val ViewWriter.scrolling: ViewWriter get() = __scrollsUncontracted(true, false)

@ViewModifierDsl3
val ViewWriter.scrollingHorizontally: ViewWriter get() = __scrollsUncontracted(false, true)

@ViewModifierDsl3
inline fun ViewWriter.scrolling(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWriter {
    return __scrollsUncontracted(vertical = true, horizontal = false, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrollingHorizontally(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWriter {
    return __scrollsUncontracted(vertical = false, horizontal = true, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrollingBoth(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWriter {
    return __scrollsUncontracted(vertical = true, horizontal = true, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrolling(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWriter {
    return __scrollsUncontracted(vertical = vertical, horizontal = horizontal, setup)
}

expect inline fun ViewWriter.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWriter

@ViewModifierDsl3
inline fun ViewWriter.scrollingWithRefresh(
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWriter {
    return __scrollsWithRefreshUncontracted(vertical = true, horizontal = false, refreshAction = refreshAction, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrollingHorizontallyWithRefresh(
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWriter {
    return __scrollsWithRefreshUncontracted(vertical = false, horizontal = true, refreshAction = refreshAction, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrollingBothWithRefresh(
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWriter {
    return __scrollsWithRefreshUncontracted(vertical = true, horizontal = true, refreshAction = refreshAction, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrollingWithRefresh(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWriter {
    return __scrollsWithRefreshUncontracted(vertical = vertical, horizontal = horizontal, refreshAction = refreshAction, setup)
}

expect inline fun ViewWriter.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWriter

@ViewModifierDsl3
expect fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWriter

@ViewModifierDsl3
fun ViewWriter.sizeConstraints(
    minWidth: Dimension? = null,
    maxWidth: Dimension? = null,
    minHeight: Dimension? = null,
    maxHeight: Dimension? = null,
    aspectRatio: Pair<Int, Int>,
    width: Dimension? = null,
    height: Dimension? = null,
): ViewWriter = sizedBox(
    SizeConstraints(
        minWidth = minWidth,
        maxWidth = maxWidth,
        minHeight = minHeight,
        maxHeight = maxHeight,
        aspectRatio = aspectRatio,
        width = width,
        height = height
    )
)

@ViewModifierDsl3
fun ViewWriter.sizeConstraints(
    minWidth: Dimension? = null,
    maxWidth: Dimension? = null,
    minHeight: Dimension? = null,
    maxHeight: Dimension? = null,
    aspectRatio: Double? = null,
    width: Dimension? = null,
    height: Dimension? = null,
): ViewWriter = sizedBox(
    SizeConstraints(
        minWidth = minWidth,
        maxWidth = maxWidth,
        minHeight = minHeight,
        maxHeight = maxHeight,
        aspectRatio = aspectRatio,
        width = width,
        height = height
    )
)

@ViewModifierDsl3
expect fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWriter

@ViewModifierDsl3
@Deprecated("No longer needed - just tell the parent what its spacing value should be.")
val ViewWriter.marginless: ViewWriter get() = this

@ViewModifierDsl3
val ViewWriter.padded: ViewWriter
    get() = beforeNextElementSetup { themeChoice += ForcePaddingSemantic }

@ViewModifierDsl3
val ViewWriter.unpadded: ViewWriter
    get() = beforeNextElementSetup { padding = 0.px }

@ViewModifierDsl3
@Deprecated("Renamed to 'padded'", ReplaceWith("padded", "com.lightningkite.kiteui.views.direct.padded"))
val ViewWriter.withDefaultPadding: ViewWriter get() = padded

@ViewModifierDsl3
expect fun ViewWriter.shownWhen(default: Boolean = false, condition: ReactiveContext.() -> Boolean): ViewWriter

@ViewModifierDsl3
@Deprecated("Renamed to 'shownWhen'", ReplaceWith("shownWhen", "com.lightningkite.kiteui.views.direct.shownWhen"))
fun ViewWriter.onlyWhen(default: Boolean = false, condition: ReactiveContext.() -> Boolean): ViewWriter = shownWhen(default, condition)
