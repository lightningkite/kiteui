@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.UnsafeModifier
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.reactive.context.ReactiveContext

@UnsafeModifier
public fun ElementWriter.withUnsafeModifiers(): ViewWriter = object : ViewWriter, ElementWriter by this {}

@ViewModifierDsl3
public expect fun ElementWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight,
    setup: ViewWriter.() -> Unit
): ElementWriter

@ViewModifierDsl3
public expect fun ElementWriter.textPopover(message: String): ElementWriter

@ViewModifierDsl3
public expect fun ElementWriter.CanAddWeight.weight(amount: Float): ElementWriter.CanAddListElementModifier

@ViewModifierDsl3
public expect fun ElementWriter.CanAddWeight.dynamicWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddListElementModifier

@ViewModifierDsl3
public expect fun ElementWriter.CanAddAlignment.align(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight

@ViewModifierDsl3
public val ElementWriter.CanAddScrolling.scrolling: ElementWriter get() = __scrollsUncontracted(vertical = true, horizontal = false)

@ViewModifierDsl3
public val ElementWriter.CanAddScrolling.scrollingHorizontally: ElementWriter get() = __scrollsUncontracted(vertical = false, horizontal = true)

@ViewModifierDsl3
public inline fun ElementWriter.CanAddScrolling.scrolling(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = true, horizontal = false, setup)
}

@ViewModifierDsl3
public inline fun ElementWriter.CanAddScrolling.scrollingHorizontally(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = false, horizontal = true, setup)
}

@ViewModifierDsl3
public inline fun ElementWriter.CanAddScrolling.scrollingBoth(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = true, horizontal = true, setup)
}

@ViewModifierDsl3
public inline fun ElementWriter.CanAddScrolling.scrolling(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter {
    return __scrollsUncontracted(vertical = vertical, horizontal = horizontal, setup)
}

@InternalKiteUi
public expect inline fun ElementWriter.CanAddScrolling.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter

@ViewModifierDsl3
public inline fun ElementWriter.CanAddScrolling.scrollingWithRefresh(
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter {
    return __scrollsWithRefreshUncontracted(vertical = true, horizontal = false, refreshAction = refreshAction, setup)
}

@ViewModifierDsl3
public inline fun ElementWriter.CanAddScrolling.scrollingHorizontallyWithRefresh(
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter {
    return __scrollsWithRefreshUncontracted(vertical = false, horizontal = true, refreshAction = refreshAction, setup)
}

@ViewModifierDsl3
public inline fun ElementWriter.CanAddScrolling.scrollingBothWithRefresh(
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter {
    return __scrollsWithRefreshUncontracted(vertical = true, horizontal = true, refreshAction = refreshAction, setup)
}

@ViewModifierDsl3
public inline fun ElementWriter.CanAddScrolling.scrollingWithRefresh(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter {
    return __scrollsWithRefreshUncontracted(vertical = vertical, horizontal = horizontal, refreshAction = refreshAction, setup)
}

@InternalKiteUi
public expect inline fun ElementWriter.CanAddScrolling.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter

@ViewModifierDsl3
public expect fun ElementWriter.CanAddSizing.sizedBox(constraints: SizeConstraints): ElementWriter.CanAddTheme

@ViewModifierDsl3
public fun ElementWriter.CanAddSizing.sizeConstraints(
    minWidth: Dimension? = null,
    maxWidth: Dimension? = null,
    minHeight: Dimension? = null,
    maxHeight: Dimension? = null,
    aspectRatio: Pair<Int, Int>,
    width: Dimension? = null,
    height: Dimension? = null,
): ElementWriter.CanAddTheme = sizedBox(
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
public fun ElementWriter.CanAddSizing.sizeConstraints(
    minWidth: Dimension? = null,
    maxWidth: Dimension? = null,
    minHeight: Dimension? = null,
    maxHeight: Dimension? = null,
    aspectRatio: Double? = null,
    width: Dimension? = null,
    height: Dimension? = null,
): ElementWriter.CanAddTheme = sizedBox(
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
public expect fun ElementWriter.CanAddSizing.dynamicSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme

@ViewModifierDsl3
public inline val ElementWriter.CanAddTheme.padded: ElementWriter.CanAddTheme get() = themed(ForcePaddingSemantic)

@ViewModifierDsl3
public val ElementWriter.CanAddTheme.unpadded: ElementWriter.CanAddTheme get() = beforeSetup { padding = 0.px }

@ViewModifierDsl3
public expect fun ElementWriter.CanAddShownWhen.shownWhen(default: Boolean = false, transition: ScreenTransition = ScreenTransition.None, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddSizing



@ViewModifierDsl3 public expect fun ElementWriter.CanAddTheme.asHeading(level: Int): ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddTheme.asMain: ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddTheme.asNavigation: ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddTheme.asBanner: ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddTheme.asContentInfo: ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddTheme.asComplementary: ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddTheme.asSearch: ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddTheme.asPresentation: ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddTheme.asList: ElementWriter.CanAddTheme
@ViewModifierDsl3 public expect val ElementWriter.CanAddListElementModifier.asListItem: ElementWriter.CanAddListElementModifier

internal expect fun ContainerElement.setupAsListContainer()
