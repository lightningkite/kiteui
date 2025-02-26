package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ForcePaddingSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.px
import com.lightningkite.readable.CalculationContext
import com.lightningkite.readable.ReactiveContext
import com.lightningkite.kiteui.views.ViewModifierDsl3
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


@ViewModifierDsl3
expect fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight,
    setup: ViewWriter.() -> Unit
): ViewWrapper

@Deprecated("Use hintPopover or opensMenu depending on your situation.")
@ViewModifierDsl3
expect fun ViewWriter.hasPopover(
    requiresClick: Boolean = false,
    preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper

interface PopoverContext {
    val calculationContext: CalculationContext
    fun close()
}

@ViewModifierDsl3
expect fun ViewWriter.textPopover(message: String): ViewWrapper

@ViewModifierDsl3
expect fun ViewWriter.weight(amount: Float): ViewWrapper

@ViewModifierDsl3
expect fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWrapper

@ViewModifierDsl3
expect fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper

@ViewModifierDsl3
val ViewWriter.scrolls: ViewWrapper get() = __scrollsUncontracted(true, false)

@ViewModifierDsl3
val ViewWriter.scrollsHorizontally: ViewWrapper get() = __scrollsUncontracted(false, true)

@ViewModifierDsl3
inline fun ViewWriter.scrolls(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWrapper {
    return __scrollsUncontracted(vertical = true, horizontal = false, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrollsHorizontally(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWrapper {
    return __scrollsUncontracted(vertical = false, horizontal = true, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrollsBoth(crossinline setup: ScrollingBehaviors.() -> Unit): ViewWrapper {
    return __scrollsUncontracted(vertical = true, horizontal = true, setup)
}

@ViewModifierDsl3
inline fun ViewWriter.scrolls(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWrapper {
    return __scrollsUncontracted(vertical = vertical, horizontal = horizontal, setup)
}

expect inline fun ViewWriter.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ViewWrapper

@ViewModifierDsl3
expect fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper

@ViewModifierDsl3
fun ViewWriter.sizeConstraints(
    minWidth: Dimension? = null,
    maxWidth: Dimension? = null,
    minHeight: Dimension? = null,
    maxHeight: Dimension? = null,
    aspectRatio: Pair<Int, Int>,
    width: Dimension? = null,
    height: Dimension? = null,
): ViewWrapper = sizedBox(
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
): ViewWrapper = sizedBox(
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
expect fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWrapper

@ViewModifierDsl3
@Deprecated("No longer needed - just tell the parent what its spacing value should be.")
val ViewWriter.marginless: ViewWrapper get() = ViewWrapper

@ViewModifierDsl3
val ViewWriter.padded: ViewWrapper
    get() {
        beforeNextElementSetup { themeChoice += ForcePaddingSemantic }
        return ViewWrapper
    }

@ViewModifierDsl3
val ViewWriter.unpadded: ViewWrapper
    get() {
        beforeNextElementSetup { padding = 0.px }
        return ViewWrapper
    }

@ViewModifierDsl3
@Deprecated("Renamed to 'padded'", ReplaceWith("padded", "com.lightningkite.kiteui.views.direct.padded"))
val ViewWriter.withDefaultPadding: ViewWrapper get() = padded

@ViewModifierDsl3
expect fun ViewWriter.onlyWhen(default: Boolean = false, condition: ReactiveContext.() -> Boolean): ViewWrapper
