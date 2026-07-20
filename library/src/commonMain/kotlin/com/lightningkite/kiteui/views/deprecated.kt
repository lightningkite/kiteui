@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.UnsafeModifier
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.EmphasizedSemantic
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.MainContentSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.direct.NumberInput
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.ScrollingBehaviors
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.__scrollsUncontracted
import com.lightningkite.kiteui.views.direct.alert
import com.lightningkite.kiteui.views.direct.align
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.confirmDanger
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.subtext
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.rawPopover
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.getValue
import kotlin.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


@Deprecated("Wrong import; this has moved", ReplaceWith("launch", "com.lightningkite.reactive.launch"), DeprecationLevel.ERROR) public val launch: Unit = Unit
@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope", "com.lightningkite.reactive.context.reactiveScope"), DeprecationLevel.ERROR) public val reactiveScope: Unit = Unit

@Deprecated("Wrong import; this has moved", ReplaceWith("DropTargetDelegate", "com.lightningkite.kiteui.models.DropTargetDelegate")) public typealias DropTargetDelegate = com.lightningkite.kiteui.models.DropTargetDelegate

@Deprecated("Renamed to ElementContext", ReplaceWith("ElementContext")) public typealias RContext = ElementContext
@Deprecated("Renamed", ReplaceWith("RContextCommonCode")) public typealias RContextHelper = ElementContextCommonCode

@Deprecated("Renamed", ReplaceWith("debugName"))
public var Element.testId: String?
    get() = debugName
    set(value) {
        debugName = value
    }

@Deprecated("No longer supported")
public var Element.transitionId: String?
    get() = null
    set(value) {}

// modifiers

@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddScrolling.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddScrolling = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddSizing.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddSizing = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddTheme.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddTheme = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddShownWhen.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddShownWhen = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddWeight.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddWeight = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddAlignment.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddAlignment = beforeSetup(setup)

@Deprecated("No longer supported, set debugName directly on the element", level = DeprecationLevel.ERROR)
public operator fun String.minus(writer: ViewWriter): ViewWriter = writer.also {
    it.beforeSetup {
        debugName = this@minus
    }
}


@Deprecated("Use modifier syntax or `applyDynamicTheme`")
@UnsafeModifier
/**
 * Apply a dynamic theme directly to an [Element] (old version)
 *
 * The new version is [dynamicThemed] (with a 'd') applied as a modifier __outside__ the element:
 *
 * ```kotlin
 * val myTheme: Reactive<ThemeDerivation> = ...
 *
 * // from this
 * frame {
 *    dynamicTheme { myTheme() }
 *    text("hello world")
 * }
 *
 * // to this
 * dynamicThemed { myTheme() }.frame {
 *    text("hello world")
 * }
 * ```
 *
 * This change was made to help encourage safety. Dynamic themes require [themeChoice][Element.themeChoice] to be
 * static at the time they are defined. Also, you can only call `dynamicTheme` once per element.
 *
 * If you change the `themeChoice` after a call to `dynamicTheme` you won't get the result you expect. Similarly,
 * if you call `dynamicTheme` twice on an element you'll get weird bugs. The modifier syntax enforces this contract.
 * If you apply a dynamic theme directly _you_ are responsible to uphold this contract.
 *
 * ```kotlin
 * frame {
 *    themeChoice += CardSemantic
 *    dynamicTheme { myTheme() } // dynamic theme applied
 *
 *    themeChoice += ImportantSemantic // <- Bug!!
 *    dynamicTheme { myTheme2() } // <- Bug!!
 * }
 * ```
 * */
public fun Element.dynamicTheme(calculate: ReactiveContext.() -> ThemeDerivation?): Unit = applyDynamicTheme(calculate)

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
public fun ElementWriter.CanAddTheme.themeFromLast(calculate: (Theme) -> Theme): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it).withBack } }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
public inline fun ElementWriter.CanAddTheme.maybeThemeFromLast(crossinline calculate: (Theme) -> Theme?): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it)?.withBack ?: it.withoutBack } }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
public inline fun ElementWriter.CanAddTheme.tweakTheme(crossinline calculate: (Theme) -> Theme): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it).withoutBack } }
}

@Deprecated("Use hintPopover or menuButton depending on your situation.", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
public expect fun ElementWriter.hasPopover(
    requiresClick: Boolean = false,
    preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight,
    setup: ViewWriter.() -> Unit
): ElementWriter

@ViewModifierDsl3
@Deprecated("No longer needed - just tell the parent what its spacing value should be.", ReplaceWith("this"), DeprecationLevel.ERROR)
public val ViewWriter.marginless: ViewWriter get() = this

@ViewModifierDsl3
@Deprecated("Renamed to 'shownWhen'", ReplaceWith("shownWhen", "com.lightningkite.kiteui.views.direct.shownWhen"))
public fun ElementWriter.CanAddShownWhen.onlyWhen(default: Boolean = false, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddTheme = shownWhen(default, condition = condition)

@ViewModifierDsl3
@Deprecated("use align instead", ReplaceWith("align"))
public fun ElementWriter.CanAddAlignment.gravity(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight = align(horizontal, vertical)

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
public val ElementWriter.CanAddScrolling.scrolls: ElementWriter get() = __scrollsUncontracted(vertical = true, horizontal = false)

@ViewModifierDsl3
@Deprecated("use scrollingHorizontally instead", ReplaceWith("scrollingHorizontally"))
public val ElementWriter.CanAddScrolling.scrollsHorizontally: ElementWriter get() = __scrollsUncontracted(vertical = false, horizontal = true)

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
public inline fun ElementWriter.CanAddScrolling.scrolls(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = true, horizontal = false, setup)
}

@ViewModifierDsl3
@Deprecated("use scrollingHorizontally instead", ReplaceWith("scrollingHorizontally"))
public inline fun ElementWriter.CanAddScrolling.scrollsHorizontally(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = false, horizontal = true, setup)
}

@ViewModifierDsl3
@Deprecated("use scrollingBoth instead", ReplaceWith("scrollingBoth"))
public inline fun ElementWriter.CanAddScrolling.scrollsBoth(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = true, horizontal = true, setup)
}

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
public inline fun ElementWriter.CanAddScrolling.scrolls(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter {
    return __scrollsUncontracted(vertical = vertical, horizontal = horizontal, setup)
}

// themes

@Deprecated("Use themed() instead", ReplaceWith("themed(this)"))
public context(writer: ElementWriter.CanAddTheme)
val Semantic.onNext: ElementWriter.CanAddTheme get() = writer.themed(this)

@Deprecated("Use themed() instead", ReplaceWith("themed(theme)"))
public fun ElementWriter.CanAddTheme.onNext(theme: ThemeDerivation): ElementWriter.CanAddTheme = themed(theme)

@ViewModifierDsl3
@Deprecated("Renamed to 'emphasized' for consistency of adjective terms.", ReplaceWith("emphasized", "com.lightningkite.kiteui.views.emphasized"))
public inline val ElementWriter.CanAddTheme.emphasize: ElementWriter.CanAddTheme get() = themed(EmphasizedSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DialogSemantic)", "com.lightningkite.kiteui.models.DialogSemantic"))
public inline val ElementWriter.CanAddTheme.dialog: ElementWriter.CanAddTheme get() = themed(DialogSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(MainContentSemantic)", "com.lightningkite.kiteui.models.MainContentSemantic"))
public inline val ElementWriter.CanAddTheme.mainContent: ElementWriter.CanAddTheme get() = themed(MainContentSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(HoverSemantic)", "com.lightningkite.kiteui.models.HoverSemantic"))
public inline val ElementWriter.CanAddTheme.hover: ElementWriter.CanAddTheme get() = themed(HoverSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DownSemantic)", "com.lightningkite.kiteui.models.DownSemantic"))
public inline val ElementWriter.CanAddTheme.down: ElementWriter.CanAddTheme get() = themed(DownSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(SelectedSemantic)", "com.lightningkite.kiteui.models.SelectedSemantic"))
public inline val ElementWriter.CanAddTheme.selected: ElementWriter.CanAddTheme get() = themed(SelectedSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(UnselectedSemantic)", "com.lightningkite.kiteui.models.UnselectedSemantic"))
public inline val ElementWriter.CanAddTheme.unselected: ElementWriter.CanAddTheme get() = themed(UnselectedSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DisabledSemantic)", "com.lightningkite.kiteui.models.DisabledSemantic"))
public inline val ElementWriter.CanAddTheme.disabled: ElementWriter.CanAddTheme get() = themed(DisabledSemantic)

@ViewModifierDsl3
@Deprecated("Renamed to 'padded'", ReplaceWith("padded", "com.lightningkite.kiteui.views.direct.padded"))
public val ElementWriter.CanAddTheme.withDefaultPadding: ElementWriter.CanAddTheme get() = padded


// views

@Deprecated("Use NumberInput instead", ReplaceWith("NumberInput"))
public typealias NumberField = NumberInput

@OptIn(ExperimentalContracts::class)
@Deprecated("Use numberInput instead", ReplaceWith("this.numberInput(setup)", "com.lightningkite.kiteui.views.direct.numberInput"))
public inline fun ElementWriter.numberField(setup: NumberInput.() -> Unit = {}): NumberInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(NumberInput(context) , setup)
}

// addons

@Deprecated(
    "Use 'pageNavigator' instead",
    ReplaceWith("this.pageNavigator", "com.lightningkite.kiteui.navigator.pageNavigator"),
    DeprecationLevel.ERROR
)
public val ViewWriter.navigator: PageNavigator by ViewWriter::pageNavigator

@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
public val PlatformNavigator: PageNavigator get() = throw NotImplementedError()

@Deprecated("Use directly through context", ReplaceWith("context.safeInsets()"))
public var Element.safeInsets: Reactive<Edges>
    get() = context.safeInsets
    set(value) { context.safeInsets = value }
@Deprecated("Use directly through context", ReplaceWith("context.popoverParent()"))
public var Element.popoverParent: ContainerElement?
    get() = context.popoverParent
    set(value) { context.popoverParent = value }
@Deprecated("Use directly through context", ReplaceWith("context.popoverCloser()"))
public var Element.popoverCloser: (() -> Unit)?
    get() = context.popoverCloser
    set(value) { context.popoverCloser = value }
@Deprecated("Use directly through context", ReplaceWith("context.popoverKeepOpen()"))
public var Element.popoverKeepOpen: Int
    get() = context.popoverKeepOpen
    set(value) { context.popoverKeepOpen = value }

@Deprecated("Use directly through context", ReplaceWith("context.closePopovers()"))
public fun Element.closePopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closePopovers()
}
@Deprecated("Use directly through context", ReplaceWith("context.closeThisPopover()"))
public fun Element.closeThisPopover() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closeSiblingPopovers()
}
@Deprecated("Use directly through context", ReplaceWith("context.closeSiblingPopovers()"))
public fun Element.closeSiblingPopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
}
@Deprecated("Use directly through context", ReplaceWith("context.keepPopoverOpen()"))
public fun Element.keepPopoverOpen(lifecycle: CoroutineScope) {
    popoverKeepOpen++
    lifecycle.onRemove { popoverKeepOpen-- }
}

@Deprecated("Set on native element directly")
public var Element.showOnPrint: Boolean
    get() = underlyingNativeElement.showOnPrint
    set(value) { underlyingNativeElement.showOnPrint = value }

@Deprecated("Use `themeBase` directly", ReplaceWith("themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading"))
public var NativeElement.themeTakeNonCascadingFromParent: Boolean
    get() = themeBase === NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
    set(value) {
        themeBase =
            if (value) NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
            else NativeElementCommonCode.GetBaseTheme.fromParent
    }

@Deprecated("use directly through context", ReplaceWith("context.overlay(modal, transition, content)"))
public fun ElementWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    content: ContainerElement.(close: ()->Unit) -> Unit
): Unit = context.overlay(modal = modal, navClosable = modal, transition = transition, body = content)

@Deprecated("use directly through context", ReplaceWith("context.toast(text, duration)")) public fun ElementWriter.toast(text: String, duration: Duration = 3.seconds): Unit = context.toast(text, duration)
@Deprecated("use directly through context", ReplaceWith("context.toast(duration, content)")) public fun ElementWriter.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddTheme.() -> Unit): Unit = context.toast(duration, content)
@Deprecated("use directly through context", ReplaceWith("context.dialog(dismissable, content)")) public fun ElementWriter.dialog(dismissable: Boolean = true, content: ElementWriter.CanAddTheme.(close: ()->Unit) -> Unit): Unit = context.dialog(dismissable, content)
@Deprecated("use directly through context", ReplaceWith("context.rawPopover(transition, content)")) public fun ElementWriter.rawPopover(transition: ScreenTransitions, content: ElementWriter.() -> Unit): Unit = context.rawPopover(transition, content = content)

@Deprecated("use directly through context", ReplaceWith("context.toast(text, duration)")) public fun Element.toast(text: String, duration: Duration = 3.seconds): Unit = context.toast(text, duration)
@Deprecated("use directly through context", ReplaceWith("context.toast(duration, content)")) public fun Element.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddTheme.() -> Unit): Unit = context.toast(duration, content)
@Deprecated("use directly through context", ReplaceWith("context.dialog(dismissable, content)")) public fun Element.dialog(dismissable: Boolean = true, content: ElementWriter.CanAddTheme.(close: ()->Unit) -> Unit): Unit = context.dialog(dismissable, content)
@Deprecated("use directly through context", ReplaceWith("context.rawPopover(transition, content)")) public fun Element.rawPopover(transition: ScreenTransitions, content: ElementWriter.() -> Unit): Unit = context.rawPopover(transition, content = content)

@Deprecated("use directly through context", ReplaceWith("context.toast(text, duration)")) public fun ContainerElement.toast(text: String, duration: Duration = 3.seconds): Unit = context.toast(text, duration)
@Deprecated("use directly through context", ReplaceWith("context.toast(duration, content)")) public fun ContainerElement.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddTheme.() -> Unit): Unit = context.toast(duration, content)
@Deprecated("use directly through context", ReplaceWith("context.dialog(dismissable, content)")) public fun ContainerElement.dialog(dismissable: Boolean = true, content: ElementWriter.CanAddTheme.(close: ()->Unit) -> Unit): Unit = context.dialog(dismissable, content)
@Deprecated("use directly through context", ReplaceWith("context.rawPopover(transition, content)")) public fun ContainerElement.rawPopover(transition: ScreenTransitions, content: ElementWriter.() -> Unit): Unit = context.rawPopover(transition, content = content)

@Deprecated("use directly through context", ReplaceWith("context.confirmDanger(title, body, actionName, cancelName, action)"))
public fun ElementWriter.confirmDanger(
    title: String,
    body: String,
    actionName: String = "OK",
    cancelName: String = "Cancel",
    action: suspend () -> Unit
): Unit = context.confirmDanger(title, body, actionName, cancelName, action)

@Deprecated("use directly through context", ReplaceWith("context.alert(title, body)"))
public fun ElementWriter.alert(
    title: String,
    body: String,
): Unit = context.alert(title, body)