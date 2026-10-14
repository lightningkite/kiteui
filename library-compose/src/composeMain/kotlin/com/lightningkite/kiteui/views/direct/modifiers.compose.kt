@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive

// Layout modifiers just record the request on the element; its parent's layout reads it.

public actual fun ElementWriter.CanAddWeight.weight(amount: Float): ElementWriter.CanAddListElementModifier =
    beforeSetup { underlyingNativeElement.weight = amount }

public actual fun ElementWriter.CanAddWeight.dynamicWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddListElementModifier =
    beforeSetup { reactive { underlyingNativeElement.weight = amount() } }

public actual fun ElementWriter.CanAddAlignment.align(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight =
    beforeSetup {
        underlyingNativeElement.horizontalAlign = horizontal
        underlyingNativeElement.verticalAlign = vertical
    }

public actual fun ElementWriter.CanAddSizing.sizedBox(constraints: SizeConstraints): ElementWriter.CanAddTheme =
    beforeSetup { underlyingNativeElement.sizeConstraints = constraints }

public actual fun ElementWriter.CanAddSizing.dynamicSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme =
    beforeSetup { reactive { underlyingNativeElement.sizeConstraints = constraints() } }

// TODO: transitions; animations are out of scope for now.
public actual fun ElementWriter.CanAddShownWhen.shownWhen(default: Boolean, transition: ScreenTransition, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddSizing =
    beforeSetup {
        shown = default
        reactive { shown = condition() }
    }

private val desktopDeviceTraits = MediaQueryDeviceTraits(
    pointer = MediaQuery.Pointer.Option.Fine,
    hover = MediaQuery.Hover.Option.Hover,
    update = MediaQuery.Update.Option.Fast,
    displayMode = MediaQuery.DisplayMode.Option.Standalone,
)

public actual fun ElementWriter.CanAddShownWhen.shownForQuery(query: MediaQuery): ElementWriter.CanAddSizing =
    shownWhen(default = query.matches(AppState.windowInfo.value, desktopDeviceTraits)) {
        query.matches(AppState.windowInfo(), desktopDeviceTraits)
    }

public actual inline fun ElementWriter.CanAddScrolling.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit,
): ElementWriter = beforeSetup { setup(enableScrolling(this, vertical, horizontal)) }

// Pull-to-refresh is a touch gesture; desktop has no equivalent, so this scrolls like a plain scroll view.
public actual inline fun ElementWriter.CanAddScrolling.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit,
): ElementWriter = beforeSetup { setup(enableScrolling(this, vertical, horizontal)) }

@PublishedApi
internal fun enableScrolling(element: Element, vertical: Boolean, horizontal: Boolean): ScrollingBehaviors {
    val native = element.underlyingNativeElement
    return ScrollingBehaviorImpl(native, horizontal = horizontal, vertical = vertical).also { native.scrolling = it }
}

// The web also opens these on contextmenu (a right click or long press); hovering covers that on a desktop.
public actual fun ElementWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit,
): ElementWriter = beforeSetup {
    val native = underlyingNativeElement
    check(native.onHoverChange == null) { "$native already opens a popover on hover" }
    val popover = Popover(source = this, anchor = this)
    native.onHoverChange = { hovered -> if (hovered) popover.open(preferredDirection, setup) else popover.close() }
}

public actual fun ElementWriter.textPopover(message: String): ElementWriter = hintPopover {
    themed(PopoverSemantic).text(message)
}

// TODO: accessibility semantics for landmarks and headings.
public actual fun ElementWriter.CanAddTheme.asHeading(level: Int): ElementWriter.CanAddTheme = this
public actual val ElementWriter.CanAddTheme.asMain: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asNavigation: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asBanner: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asContentInfo: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asComplementary: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asSearch: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asPresentation: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asList: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddListElementModifier.asListItem: ElementWriter.CanAddListElementModifier get() = this

internal actual fun ContainerElement.setupAsListContainer() {}
