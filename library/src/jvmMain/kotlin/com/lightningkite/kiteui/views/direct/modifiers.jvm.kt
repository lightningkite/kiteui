package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ViewModifierDsl3
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.ReactiveContext

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ViewWrapper {
    TODO("Not yet implemented")
}

@Deprecated(message = "Use hintPopover or opensMenu depending on your situation.")
@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper {
    TODO("Not yet implemented")
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWrapper {
    TODO("Not yet implemented")
}

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    TODO("Not yet implemented")
}

@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWrapper {
    TODO("Not yet implemented")
}

@ViewModifierDsl3
actual fun ViewWriter.align(
    horizontal: Align,
    vertical: Align
): ViewWrapper {
    TODO("Not yet implemented")
}

actual inline fun ViewWriter.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWrapper {
    TODO("Not yet implemented")
}

actual inline fun ViewWriter.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWrapper {
    TODO("Not yet implemented")
}

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    TODO("Not yet implemented")
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWrapper {
    TODO("Not yet implemented")
}

@ViewModifierDsl3
actual fun ViewWriter.shownWhen(
    default: Boolean,
    condition: ReactiveContext.() -> Boolean
): ViewWrapper {
    TODO("Not yet implemented")
}